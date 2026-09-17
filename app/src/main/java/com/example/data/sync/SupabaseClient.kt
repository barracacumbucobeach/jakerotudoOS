package com.example.data.sync

import android.util.Log
import com.example.data.local.Converters
import com.example.data.model.Ordem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class SupabaseClient(
    private val baseUrl: String,
    private val anonKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    fun cleanBaseUrl(): String {
        return baseUrl.trim().removeSuffix("/")
    }

    /**
     * Garante que o bucket público `fotos` exista no Supabase Storage.
     */
    suspend fun ensureBucketExists(bucketName: String = "fotos"): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))
        try {
            val url = "${cleanBaseUrl()}/storage/v1/bucket"
            val json = JSONObject().apply {
                put("id", bucketName)
                put("name", bucketName)
                put("public", true)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .post(body)
                .build()
            client.newCall(request).execute().close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envia uma ordem para o Supabase (Upsert via PostgREST).
     */
    suspend fun upsertOrdem(ordem: Ordem): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))

        try {
            val converters = Converters()
            val dadosObj = JSONObject().apply {
                put("cliente", ordem.cliente)
                put("telefone", ordem.telefone)
                put("servico", ordem.servico)
                put("quantidade", ordem.quantidade)
                put("valorUnitario", ordem.valorUnitario)
                put("entrada", ordem.entrada)
                put("desconto", ordem.desconto)
                put("observacoes", ordem.observacoes)
                put("status", ordem.status.name)
                put("data", ordem.data.toString())
                put("entregaPrevista", ordem.entregaPrevista?.toString())
                put("entregaReal", ordem.entregaReal?.toString())
                put("vencimento", ordem.vencimento?.toString())
                put("plano", converters.fromPlano(ordem.plano))
                put("pagamentos", converters.fromPagamentos(ordem.pagamentos))
                put("fotos", converters.fromStringList(ordem.fotos))
            }

            val payload = JSONObject().apply {
                if (!ordem.cloudId.isNullOrBlank()) {
                    put("id", ordem.cloudId)
                }
                put("num", ordem.num)
                put("dados", dadosObj)
                put("removido", ordem.removido)
                put("atualizado_em", Instant.ofEpochMilli(ordem.atualizadoEm).toString())
            }

            val url = "${cleanBaseUrl()}/rest/v1/ordens"
            val body = payload.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    var cloudId = ordem.cloudId ?: ""
                    try {
                        val array = JSONArray(respBody)
                        if (array.length() > 0) {
                            cloudId = array.getJSONObject(0).getString("id")
                        }
                    } catch (_: Exception) {}
                    Result.success(cloudId)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao enviar ordem", e)
            Result.failure(e)
        }
    }

    /**
     * Busca ordens atualizadas após determinado timestamp ISO 8601.
     */
    suspend fun fetchOrdensModificadas(ultimoSyncMillis: Long): Result<List<Ordem>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))

        try {
            val isoSince = Instant.ofEpochMilli(ultimoSyncMillis).toString()
            val url = "${cleanBaseUrl()}/rest/v1/ordens?select=*&atualizado_em=gt.$isoSince&order=atualizado_em.asc"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val bodyStr = response.body?.string() ?: "[]"
                parseOrdensArray(JSONArray(bodyStr))
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar ordens modificadas", e)
            Result.failure(e)
        }
    }

    /**
     * Busca todas as ordens (usado na carga inicial ou sincronização completa).
     */
    suspend fun fetchTodasOrdens(): Result<List<Ordem>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))

        try {
            val url = "${cleanBaseUrl()}/rest/v1/ordens?select=*&order=atualizado_em.asc"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val bodyStr = response.body?.string() ?: "[]"
                parseOrdensArray(JSONArray(bodyStr))
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar todas as ordens", e)
            Result.failure(e)
        }
    }

    private fun parseOrdensArray(array: JSONArray): Result<List<Ordem>> {
        val converters = Converters()
        val list = mutableListOf<Ordem>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val cloudId = item.getString("id")
            val num = item.optInt("num", 0)
            val removido = item.optBoolean("removido", false)
            val atualizadoEmStr = item.optString("atualizado_em")
            val atualizadoEm = try {
                Instant.parse(atualizadoEmStr).toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            val dados = item.optJSONObject("dados") ?: JSONObject()
            val plano = converters.toPlano(dados.optString("plano", null))

            // Pagamentos flexíveis (JSON Array ou String)
            val pagamentos = when (val p = dados.opt("pagamentos") ?: dados.opt("pagos")) {
                is JSONArray -> {
                    val pagList = mutableListOf<com.example.data.model.Pagamento>()
                    for (idx in 0 until p.length()) {
                        val obj = p.optJSONObject(idx)
                        if (obj != null) {
                            pagList.add(
                                com.example.data.model.Pagamento(
                                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                    data = try { LocalDate.parse(obj.getString("data")) } catch (_: Exception) { LocalDate.now() },
                                    valor = obj.optDouble("valor", 0.0)
                                )
                            )
                        }
                    }
                    pagList
                }
                is String -> converters.toPagamentos(p)
                else -> emptyList()
            }

            // Fotos flexíveis (JSON Array ou String)
            val fotos = when (val f = dados.opt("fotos")) {
                is JSONArray -> {
                    val fotoList = mutableListOf<String>()
                    for (idx in 0 until f.length()) {
                        val s = f.optString(idx)
                        if (!s.isNullOrBlank()) fotoList.add(s)
                    }
                    fotoList
                }
                is String -> converters.toStringList(f)
                else -> emptyList()
            }

            val data = try {
                LocalDate.parse(dados.getString("data"))
            } catch (_: Exception) {
                LocalDate.now()
            }

            val entregaPrevistaStr = dados.optString("entregaPrevista").takeIf { it.isNotBlank() }
                ?: dados.optString("entregaPrev", null)
            val entregaPrevista = entregaPrevistaStr?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }

            val entregaRealStr = dados.optString("entregaReal", null)
            val entregaReal = entregaRealStr?.takeIf { it.isNotBlank() }?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }

            val vencimentoStr = dados.optString("vencimento", null)
            val vencimento = vencimentoStr?.takeIf { it.isNotBlank() }?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }

            val quantidade = if (dados.has("quantidade")) dados.optDouble("quantidade", 1.0) else dados.optDouble("qtd", 1.0)
            val valorUnitario = if (dados.has("valorUnitario")) dados.optDouble("valorUnitario", 0.0) else dados.optDouble("unit", 0.0)
            val entrada = dados.optDouble("entrada", 0.0)
            val desconto = try {
                if (dados.has("desconto")) dados.getDouble("desconto") else 0.0
            } catch (_: Exception) {
                0.0
            }
            val observacoes = dados.optString("observacoes").takeIf { it.isNotBlank() } ?: dados.optString("obs", "")

            val ordem = Ordem(
                num = num,
                data = data,
                cliente = dados.optString("cliente", ""),
                telefone = dados.optString("telefone", ""),
                servico = dados.optString("servico", ""),
                quantidade = quantidade,
                valorUnitario = valorUnitario,
                entrada = entrada,
                desconto = desconto,
                observacoes = observacoes,
                status = converters.toStatusOrdem(dados.optString("status")),
                entregaPrevista = entregaPrevista,
                entregaReal = entregaReal,
                vencimento = vencimento,
                plano = plano,
                pagamentos = pagamentos,
                fotos = fotos,
                atualizadoEm = atualizadoEm,
                removido = removido,
                sincronizado = true,
                cloudId = cloudId
            )
            list.add(ordem)
        }
        return Result.success(list)
    }

    /**
     * Faz upload de arquivo para o Storage bucket `fotos`.
     */
    suspend fun uploadFoto(file: File, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))

        try {
            ensureBucketExists("fotos")
            val url = "${cleanBaseUrl()}/storage/v1/object/fotos/$fileName"
            val body = file.readBytes().toRequestBody("image/jpeg".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .addHeader("x-upsert", "true")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val publicUrl = "${cleanBaseUrl()}/storage/v1/object/public/fotos/$fileName"
                    Result.success(publicUrl)
                } else {
                    Result.failure(Exception("Erro no upload da foto: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
