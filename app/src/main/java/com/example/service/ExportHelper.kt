package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Ordem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.util.Locale

object ExportHelper {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private fun formatarDecimal(valor: Double): String {
        return String.format(Locale("pt", "BR"), "%.2f", valor)
    }

    suspend fun gerarCsvRelatorio(
        context: Context,
        tituloPeriodo: String,
        ordens: List<Ordem>,
        caixaRecebido: Double,
        valorTotalGeral: Double
    ): File = withContext(Dispatchers.IO) {
        val relatoriosDir = File(context.cacheDir, "relatorios").apply { mkdirs() }
        val arquivo = File(relatoriosDir, "Relatorio_JakeroTudo_${System.currentTimeMillis()}.csv")

        FileOutputStream(arquivo).use { fos ->
            // BOM UTF-8 para Excel abrir com caracteres em português corretos
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Cabeçalho
                writer.append("Nº OS;Data;Cliente;Telefone;Serviços;Situação;Entrega Prevista;Entrega Real;Quantidade;Valor Unitário;Entrada;Desconto;Valor;Total Pago;Falta;Pagamentos\n")

                for (o in ordens) {
                    val pagamentosStr = o.pagamentos.joinToString(" | ") {
                        "${it.data.format(dateFormatter)}: R$ ${formatarDecimal(it.valor)}"
                    }

                    val linha = listOf(
                        o.numeroFormatado,
                        o.data.format(dateFormatter),
                        "\"${o.cliente.replace("\"", "\"\"")}\"",
                        "\"${o.telefone}\"",
                        "\"${o.servico.replace("\"", "\"\"")}\"",
                        o.status.label,
                        o.entregaPrevista?.format(dateFormatter) ?: "",
                        o.entregaReal?.format(dateFormatter) ?: "",
                        formatarDecimal(o.quantidade),
                        formatarDecimal(o.valorUnitario),
                        formatarDecimal(o.entrada),
                        formatarDecimal(o.desconto),
                        formatarDecimal(o.valor),
                        formatarDecimal(o.totalPago),
                        formatarDecimal(o.falta),
                        "\"$pagamentosStr\""
                    ).joinToString(";")

                    writer.append(linha).append("\n")
                }

                // Linhas finais de resumo
                writer.append("\n")
                writer.append(";;;;;;;;;;;;CAIXA (RECEBIDO);R$ ${formatarDecimal(caixaRecebido)};;\n")
                writer.append(";;;;;;;;;;;;VALOR TOTAL;R$ ${formatarDecimal(valorTotalGeral)};;\n")
            }
        }
        arquivo
    }

    fun compartilharArquivo(context: Context, file: File, mimeType: String, titulo: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, titulo)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, titulo))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun compartilharFotos(context: Context, caminhosFotos: List<String>) {
        if (caminhosFotos.isEmpty()) return
        try {
            val uris = ArrayList<Uri>()
            for (caminho in caminhosFotos) {
                val f = File(caminho)
                if (f.exists()) {
                    uris.add(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            f
                        )
                    )
                }
            }

            if (uris.isEmpty()) return

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Enviar fotos da OS"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun abrirWhatsApp(context: Context, telefoneRaw: String, mensagem: String) {
        try {
            var numeros = telefoneRaw.replace(Regex("[^0-9]"), "")
            if (numeros.startsWith("0")) {
                numeros = numeros.substring(1)
            }
            // Se tem 10 ou 11 dígitos, adicionar DDI Brasil 55
            if (numeros.length in 10..11) {
                numeros = "55$numeros"
            }

            val msgEncoded = URLEncoder.encode(mensagem, "UTF-8")
            val url = if (numeros.isNotBlank()) {
                "https://wa.me/$numeros?text=$msgEncoded"
            } else {
                "https://wa.me/?text=$msgEncoded"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
