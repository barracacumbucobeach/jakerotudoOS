package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.Converters
import com.example.data.model.Ordem
import com.example.data.model.Pagamento
import com.example.data.model.StatusOrdem
import com.example.data.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class OrdemRepository(
    private val database: AppDatabase,
    private val syncManager: SyncManager
) {
    private val ordemDao = database.ordemDao()

    val allActiveOrdens: Flow<List<Ordem>> = ordemDao.getAllActive()

    fun getOrdem(id: Long): Flow<Ordem?> = ordemDao.getById(id)

    suspend fun salvarOrdem(ordem: Ordem): Long = withContext(Dispatchers.IO) {
        val numFinal = if (ordem.num <= 0) {
            (ordemDao.getMaxNumero() ?: 0) + 1
        } else {
            ordem.num
        }

        val ordemParaSalvar = ordem.copy(
            num = numFinal,
            atualizadoEm = System.currentTimeMillis(),
            sincronizado = false
        )

        val idResult = if (ordemParaSalvar.id == 0L) {
            ordemDao.insert(ordemParaSalvar)
        } else {
            ordemDao.update(ordemParaSalvar)
            ordemParaSalvar.id
        }

        syncManager.atualizarContadorPendentes()
        syncManager.forcarSincronizacao()
        idResult
    }

    suspend fun avancarStatus(ordem: Ordem) = withContext(Dispatchers.IO) {
        val novoStatus = when (ordem.status) {
            StatusOrdem.ABERTA -> StatusOrdem.ANDAMENTO
            StatusOrdem.ANDAMENTO -> StatusOrdem.ENTREGUE
            StatusOrdem.ENTREGUE -> StatusOrdem.ENTREGUE
        }
        val novaEntregaReal = if (novoStatus == StatusOrdem.ENTREGUE && ordem.entregaReal == null) {
            LocalDate.now()
        } else {
            ordem.entregaReal
        }

        val atualizada = ordem.copy(
            status = novoStatus,
            entregaReal = novaEntregaReal,
            atualizadoEm = System.currentTimeMillis(),
            sincronizado = false
        )
        ordemDao.update(atualizada)
        syncManager.atualizarContadorPendentes()
        syncManager.forcarSincronizacao()
    }

    suspend fun adicionarPagamento(ordem: Ordem, valor: Double, data: LocalDate) = withContext(Dispatchers.IO) {
        if (valor <= 0.0) return@withContext
        val novoPagamento = Pagamento(
            id = UUID.randomUUID().toString(),
            data = data,
            valor = valor
        )
        val novosPagamentos = ordem.pagamentos + novoPagamento
        val atualizada = ordem.copy(
            pagamentos = novosPagamentos,
            atualizadoEm = System.currentTimeMillis(),
            sincronizado = false
        )
        ordemDao.update(atualizada)
        syncManager.atualizarContadorPendentes()
        syncManager.forcarSincronizacao()
    }

    suspend fun removerPagamento(ordem: Ordem, pagamentoId: String) = withContext(Dispatchers.IO) {
        val novosPagamentos = ordem.pagamentos.filterNot { it.id == pagamentoId }
        val atualizada = ordem.copy(
            pagamentos = novosPagamentos,
            atualizadoEm = System.currentTimeMillis(),
            sincronizado = false
        )
        ordemDao.update(atualizada)
        syncManager.atualizarContadorPendentes()
        syncManager.forcarSincronizacao()
    }

    suspend fun adicionarFoto(ordemId: Long, caminhoFoto: String) = withContext(Dispatchers.IO) {
        val ordem = ordemDao.getByIdImmediate(ordemId) ?: return@withContext
        if (ordem.fotos.size >= 6) return@withContext
        val novasFotos = ordem.fotos + caminhoFoto
        val atualizada = ordem.copy(
            fotos = novasFotos,
            atualizadoEm = System.currentTimeMillis(),
            sincronizado = false
        )
        ordemDao.update(atualizada)
        syncManager.atualizarContadorPendentes()
        syncManager.forcarSincronizacao()
    }

    suspend fun removerFoto(ordemId: Long, caminhoFoto: String) = withContext(Dispatchers.IO) {
        val ordem = ordemDao.getByIdImmediate(ordemId) ?: return@withContext
        val novasFotos = ordem.fotos.filterNot { it == caminhoFoto }
        val atualizada = ordem.copy(
            fotos = novasFotos,
            atualizadoEm = System.currentTimeMillis(),
            sincronizado = false
        )
        ordemDao.update(atualizada)
        syncManager.atualizarContadorPendentes()
        syncManager.forcarSincronizacao()
    }

    suspend fun excluirOrdem(id: Long) = withContext(Dispatchers.IO) {
        ordemDao.softDelete(id)
        syncManager.atualizarContadorPendentes()
        syncManager.forcarSincronizacao()
    }

    suspend fun exportarBackupJson(): String = withContext(Dispatchers.IO) {
        val converters = Converters()
        val todas = ordemDao.getPendingSync() + ordemDao.getPendingSync() // get all active
        // Let's get all active from database
        val ordens = database.openHelper.readableDatabase.let {
            val list = mutableListOf<Ordem>()
            val cursor = it.query("SELECT * FROM ordens WHERE removido = 0")
            val idIdx = cursor.getColumnIndex("id")
            val numIdx = cursor.getColumnIndex("num")
            val dataIdx = cursor.getColumnIndex("data")
            val clienteIdx = cursor.getColumnIndex("cliente")
            val telIdx = cursor.getColumnIndex("telefone")
            val servicoIdx = cursor.getColumnIndex("servico")
            val qtdIdx = cursor.getColumnIndex("quantidade")
            val vuIdx = cursor.getColumnIndex("valorUnitario")
            val entradaIdx = cursor.getColumnIndex("entrada")
            val descIdx = cursor.getColumnIndex("desconto")
            val obsIdx = cursor.getColumnIndex("observacoes")
            val statusIdx = cursor.getColumnIndex("status")
            val epIdx = cursor.getColumnIndex("entregaPrevista")
            val erIdx = cursor.getColumnIndex("entregaReal")
            val vencIdx = cursor.getColumnIndex("vencimento")
            val planoIdx = cursor.getColumnIndex("plano")
            val pagsIdx = cursor.getColumnIndex("pagamentos")
            val fotosIdx = cursor.getColumnIndex("fotos")
            val atIdx = cursor.getColumnIndex("atualizadoEm")

            while (cursor.moveToNext()) {
                list.add(
                    Ordem(
                        id = cursor.getLong(idIdx),
                        num = cursor.getInt(numIdx),
                        data = LocalDate.parse(cursor.getString(dataIdx)),
                        cliente = cursor.getString(clienteIdx),
                        telefone = cursor.getString(telIdx),
                        servico = cursor.getString(servicoIdx),
                        quantidade = cursor.getDouble(qtdIdx),
                        valorUnitario = cursor.getDouble(vuIdx),
                        entrada = cursor.getDouble(entradaIdx),
                        desconto = cursor.getDouble(descIdx),
                        observacoes = cursor.getString(obsIdx),
                        status = converters.toStatusOrdem(cursor.getString(statusIdx)),
                        entregaPrevista = converters.toLocalDate(cursor.getString(epIdx)),
                        entregaReal = converters.toLocalDate(cursor.getString(erIdx)),
                        vencimento = converters.toLocalDate(cursor.getString(vencIdx)),
                        plano = converters.toPlano(cursor.getString(planoIdx)),
                        pagamentos = converters.toPagamentos(cursor.getString(pagsIdx)),
                        fotos = converters.toStringList(cursor.getString(fotosIdx)),
                        atualizadoEm = cursor.getLong(atIdx)
                    )
                )
            }
            cursor.close()
            list
        }

        val root = JSONObject()
        root.put("versao", 1)
        root.put("geradoEm", System.currentTimeMillis())
        val jsonArray = JSONArray()

        for (o in ordens) {
            val item = JSONObject().apply {
                put("num", o.num)
                put("data", o.data.toString())
                put("cliente", o.cliente)
                put("telefone", o.telefone)
                put("servico", o.servico)
                put("quantidade", o.quantidade)
                put("valorUnitario", o.valorUnitario)
                put("entrada", o.entrada)
                put("desconto", o.desconto)
                put("observacoes", o.observacoes)
                put("status", o.status.name)
                put("entregaPrevista", o.entregaPrevista?.toString())
                put("entregaReal", o.entregaReal?.toString())
                put("vencimento", o.vencimento?.toString())
                put("plano", converters.fromPlano(o.plano))
                put("pagamentos", converters.fromPagamentos(o.pagamentos))
                put("fotos", converters.fromStringList(o.fotos))
            }
            jsonArray.put(item)
        }
        root.put("ordens", jsonArray)
        root.toString(2)
    }

    suspend fun importarBackupJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val converters = Converters()
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("ordens")
        var count = 0

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val num = item.getInt("num")
            val data = LocalDate.parse(item.getString("data"))
            val cliente = item.getString("cliente")
            val telefone = item.optString("telefone", "")
            val servico = item.getString("servico")
            val quantidade = item.optDouble("quantidade", 1.0)
            val valorUnitario = item.optDouble("valorUnitario", 0.0)
            val entrada = item.optDouble("entrada", 0.0)
            val desconto = item.optDouble("desconto", 0.0)
            val observacoes = item.optString("observacoes", "")
            val status = converters.toStatusOrdem(item.optString("status"))
            val entregaPrevista = converters.toLocalDate(item.optString("entregaPrevista", null))
            val entregaReal = converters.toLocalDate(item.optString("entregaReal", null))
            val vencimento = converters.toLocalDate(item.optString("vencimento", null))
            val plano = converters.toPlano(item.optString("plano", null))
            val pagamentos = converters.toPagamentos(item.optString("pagamentos", "[]"))
            val fotos = converters.toStringList(item.optString("fotos", "[]"))

            val nova = Ordem(
                num = num,
                data = data,
                cliente = cliente,
                telefone = telefone,
                servico = servico,
                quantidade = quantidade,
                valorUnitario = valorUnitario,
                entrada = entrada,
                desconto = desconto,
                observacoes = observacoes,
                status = status,
                entregaPrevista = entregaPrevista,
                entregaReal = entregaReal,
                vencimento = vencimento,
                plano = plano,
                pagamentos = pagamentos,
                fotos = fotos,
                atualizadoEm = System.currentTimeMillis(),
                sincronizado = false
            )
            ordemDao.insert(nova)
            count++
        }
        syncManager.atualizarContadorPendentes()
        syncManager.forcarSincronizacao()
        count
    }
}
