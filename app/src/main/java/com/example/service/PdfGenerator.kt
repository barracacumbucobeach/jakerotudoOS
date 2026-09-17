package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.model.Ordem
import com.example.data.model.ParcelaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object PdfGenerator {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private fun formatarMoeda(valor: Double): String {
        return String.format(Locale("pt", "BR"), "R$ %,.2f", valor)
    }

    private fun formatarData(data: LocalDate?): String {
        return data?.format(dateFormatter) ?: "—"
    }

    suspend fun gerarPdfOrdem(context: Context, ordem: Ordem): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Fundo branco
        canvas.drawColor(Color.WHITE)

        val margin = 36f
        val pageWidth = 595f
        val contentWidth = pageWidth - (margin * 2)
        var y = margin

        // 1. TOPO: Logo e Dados da Empresa
        val logoBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.jakero_logo)
        } catch (_: Exception) { null }

        if (logoBitmap != null) {
            val logoSize = 64f
            val destRect = RectF(margin, y, margin + logoSize, y + logoSize)
            canvas.drawBitmap(logoBitmap, null, destRect, paint)
        }

        // Dados da empresa à direita da logo
        paint.color = Color.rgb(27, 122, 133) // Teal
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText("SHOWCIAL MEDIA", margin + 74f, y + 16f, paint)

        paint.color = Color.rgb(80, 80, 80)
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("CNPJ: 34.741.143/0001-76", margin + 74f, y + 30f, paint)
        canvas.drawText("Pix (CNPJ): 34.741.143/0001-76 · Titular: Jorge Leandro de Jesus Braga", margin + 74f, y + 44f, paint)

        // Nº da OS e data de emissão alinhados à direita
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(138, 43, 6) // Rust Brown
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("OS Nº ${ordem.numeroFormatado}", pageWidth - margin, y + 16f, paint)

        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText("Emissão: ${LocalDate.now().format(dateFormatter)}", pageWidth - margin, y + 32f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += 74f

        // Linha divisória
        paint.color = Color.rgb(142, 230, 0) // Lime
        paint.strokeWidth = 2.5f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        paint.strokeWidth = 1f
        y += 18f

        // 2. TÍTULO
        paint.color = Color.rgb(35, 31, 28)
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("ORDEM DE SERVIÇO", margin, y, paint)
        y += 18f

        // 3. GRADE DE DADOS: Cliente, Contato, Datas, Situação
        val boxRect = RectF(margin, y, pageWidth - margin, y + 78f)
        paint.color = Color.rgb(248, 248, 245)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(boxRect, 8f, 8f, paint)
        paint.color = Color.rgb(220, 220, 215)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(boxRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var gy = y + 16f
        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText("CLIENTE", margin + 12f, gy, paint)
        canvas.drawText("TELEFONE / WHATSAPP", margin + 260f, gy, paint)

        gy += 14f
        paint.color = Color.rgb(35, 31, 28)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText(ordem.cliente, margin + 12f, gy, paint)
        canvas.drawText(ordem.telefone.ifBlank { "Não informado" }, margin + 260f, gy, paint)

        gy += 20f
        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText("DATA DO PEDIDO", margin + 12f, gy, paint)
        canvas.drawText("PREVISÃO DE ENTREGA", margin + 140f, gy, paint)
        canvas.drawText("SITUAÇÃO", margin + 270f, gy, paint)
        canvas.drawText("ENTREGA REAL", margin + 390f, gy, paint)

        gy += 14f
        paint.color = Color.rgb(35, 31, 28)
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText(formatarData(ordem.data), margin + 12f, gy, paint)
        canvas.drawText(formatarData(ordem.entregaPrevista), margin + 140f, gy, paint)

        // Status badge color
        paint.color = when (ordem.status) {
            com.example.data.model.StatusOrdem.ENTREGUE -> Color.rgb(46, 125, 50)
            com.example.data.model.StatusOrdem.ANDAMENTO -> Color.rgb(2, 136, 209)
            com.example.data.model.StatusOrdem.ABERTA -> Color.rgb(230, 81, 0)
        }
        canvas.drawText(ordem.status.label.uppercase(), margin + 270f, gy, paint)

        paint.color = Color.rgb(35, 31, 28)
        canvas.drawText(formatarData(ordem.entregaReal), margin + 390f, gy, paint)

        y += 92f

        // 4. TABELA DE SERVIÇOS
        val tabelaTop = y
        paint.color = Color.rgb(27, 122, 133) // Teal Header
        val tabelaHeaderRect = RectF(margin, tabelaTop, pageWidth - margin, tabelaTop + 22f)
        canvas.drawRect(tabelaHeaderRect, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("DESCRIÇÃO DO SERVIÇO", margin + 10f, tabelaTop + 14f, paint)
        canvas.drawText("QTD", margin + 270f, tabelaTop + 14f, paint)
        canvas.drawText("VALOR UNIT.", margin + 320f, tabelaTop + 14f, paint)
        canvas.drawText("DESCONTO", margin + 400f, tabelaTop + 14f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("VALOR TOTAL", pageWidth - margin - 10f, tabelaTop + 14f, paint)
        paint.textAlign = Paint.Align.LEFT

        val linhaY = tabelaTop + 40f
        paint.color = Color.rgb(35, 31, 28)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val servicoTxt = if (ordem.servico.length > 42) ordem.servico.take(40) + "…" else ordem.servico
        canvas.drawText(servicoTxt, margin + 10f, linhaY, paint)
        canvas.drawText(String.format(Locale("pt", "BR"), "%.2f", ordem.quantidade), margin + 270f, linhaY, paint)
        canvas.drawText(formatarMoeda(ordem.valorUnitario), margin + 320f, linhaY, paint)
        canvas.drawText(formatarMoeda(ordem.desconto), margin + 400f, linhaY, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.isFakeBoldText = true
        canvas.drawText(formatarMoeda(ordem.valor), pageWidth - margin - 10f, linhaY, paint)
        paint.textAlign = Paint.Align.LEFT

        paint.color = Color.rgb(230, 230, 225)
        canvas.drawLine(margin, linhaY + 8f, pageWidth - margin, linhaY + 8f, paint)

        y = linhaY + 22f

        // 5. PARCELAMENTO & PAGAMENTOS
        if (ordem.isParcelado) {
            paint.color = Color.rgb(35, 31, 28)
            paint.textSize = 10.5f
            paint.isFakeBoldText = true
            canvas.drawText("PLANO DE PARCELAMENTO (${ordem.plano?.parcelas}x ${ordem.plano?.frequencia?.label})", margin, y, paint)
            y += 14f

            val parcelas = ordem.parcelasCalculadas()
            for (p in parcelas) {
                paint.color = Color.rgb(80, 80, 80)
                paint.textSize = 8.5f
                paint.isFakeBoldText = false
                canvas.drawText("Parcela ${p.numero}/${p.totalParcelas} · Vencimento: ${formatarData(p.vencimento)}", margin + 8f, y, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.color = when (p.status) {
                    ParcelaStatus.PAGA -> Color.rgb(46, 125, 50)
                    ParcelaStatus.ATRASADA -> Color.rgb(211, 47, 47)
                    ParcelaStatus.PARCIAL -> Color.rgb(230, 81, 0)
                    ParcelaStatus.EM_ABERTO -> Color.rgb(70, 70, 70)
                }
                paint.isFakeBoldText = true
                val statusStr = if (p.status == ParcelaStatus.PARCIAL) "Parcial (pago ${formatarMoeda(p.valorPago)})" else p.status.label
                canvas.drawText("${formatarMoeda(p.valor)}  [${statusStr}]", pageWidth - margin - 10f, y, paint)
                paint.textAlign = Paint.Align.LEFT
                y += 12f
            }
            y += 6f
        }

        // Histórico de pagamentos
        paint.color = Color.rgb(35, 31, 28)
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas.drawText("HISTÓRICO FINANCEIRO", margin, y, paint)
        y += 14f

        if (ordem.entrada > 0.0) {
            paint.color = Color.rgb(70, 70, 70)
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            canvas.drawText("Entrada no ato (${formatarData(ordem.data)})", margin + 8f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.rgb(46, 125, 50)
            paint.isFakeBoldText = true
            canvas.drawText("+ ${formatarMoeda(ordem.entrada)}", pageWidth - margin - 10f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            y += 12f
        }

        for (pag in ordem.pagamentos) {
            paint.color = Color.rgb(70, 70, 70)
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            canvas.drawText("Pagamento registrado (${formatarData(pag.data)})", margin + 8f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.rgb(46, 125, 50)
            paint.isFakeBoldText = true
            canvas.drawText("+ ${formatarMoeda(pag.valor)}", pageWidth - margin - 10f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            y += 12f
        }

        y += 8f

        // 6. FOTOS DE REFERÊNCIA (se houver)
        if (ordem.fotos.isNotEmpty()) {
            paint.color = Color.rgb(35, 31, 28)
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("FOTOS DE REFERÊNCIA (${ordem.fotos.size})", margin, y, paint)
            y += 12f

            var fx = margin
            val thumbWidth = 56f
            val thumbHeight = 44f

            for (fotoPath in ordem.fotos.take(6)) {
                try {
                    val file = File(fotoPath)
                    if (file.exists()) {
                        val bm = BitmapFactory.decodeFile(file.absolutePath)
                        if (bm != null) {
                            val r = RectF(fx, y, fx + thumbWidth, y + thumbHeight)
                            canvas.drawBitmap(bm, null, r, paint)
                            paint.style = Paint.Style.STROKE
                            paint.color = Color.rgb(200, 200, 200)
                            canvas.drawRect(r, paint)
                            paint.style = Paint.Style.FILL
                            bm.recycle()
                        }
                    }
                } catch (_: Exception) {}
                fx += thumbWidth + 8f
            }
            y += thumbHeight + 14f
        }

        // 7. OBSERVAÇÕES
        if (ordem.observacoes.isNotBlank()) {
            paint.color = Color.rgb(35, 31, 28)
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            canvas.drawText("OBSERVAÇÕES:", margin, y, paint)
            y += 12f

            paint.color = Color.rgb(80, 80, 80)
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            val obsText = if (ordem.observacoes.length > 120) ordem.observacoes.take(118) + "…" else ordem.observacoes
            canvas.drawText(obsText, margin + 4f, y, paint)
            y += 18f
        }

        // 8. CARTÕES TOTAIS (TOTAL PAGO E SALDO A PAGAR / QUITADO)
        val cardWidth = (contentWidth - 12f) / 2f
        val cardHeight = 42f

        // Cartão 1: Total Pago
        val cardPagoRect = RectF(margin, y, margin + cardWidth, y + cardHeight)
        paint.color = Color.rgb(245, 245, 240)
        canvas.drawRoundRect(cardPagoRect, 6f, 6f, paint)
        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("TOTAL PAGO", margin + 10f, y + 14f, paint)
        paint.color = Color.rgb(46, 125, 50)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText(formatarMoeda(ordem.totalPago), margin + 10f, y + 32f, paint)

        // Cartão 2: Saldo / Quitado
        val cardSaldoRect = RectF(margin + cardWidth + 12f, y, pageWidth - margin, y + cardHeight)
        if (ordem.quitada) {
            paint.color = Color.rgb(230, 245, 230)
            canvas.drawRoundRect(cardSaldoRect, 6f, 6f, paint)
            paint.color = Color.rgb(46, 125, 50)
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText("SITUAÇÃO FINANCEIRA", margin + cardWidth + 22f, y + 14f, paint)
            paint.textSize = 13f
            paint.isFakeBoldText = true
            val dtQ = formatarData(ordem.dataQuitacao)
            canvas.drawText("QUITADO EM $dtQ", margin + cardWidth + 22f, y + 32f, paint)
        } else {
            paint.color = Color.rgb(255, 240, 235)
            canvas.drawRoundRect(cardSaldoRect, 6f, 6f, paint)
            paint.color = Color.rgb(138, 43, 6)
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText("FALTA RECEBER", margin + cardWidth + 22f, y + 14f, paint)
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText(formatarMoeda(ordem.falta), margin + cardWidth + 22f, y + 32f, paint)
        }

        y += cardHeight + 40f

        // 9. LINHAS DE ASSINATURA
        val signWidth = 200f
        val signY = y + 20f

        // Linha cliente
        paint.color = Color.rgb(150, 150, 150)
        paint.strokeWidth = 1f
        canvas.drawLine(margin + 20f, signY, margin + 20f + signWidth, signY, paint)
        paint.color = Color.rgb(70, 70, 70)
        paint.textSize = 8f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(ordem.cliente, margin + 20f + (signWidth / 2), signY + 12f, paint)
        canvas.drawText("Assinatura do Cliente", margin + 20f + (signWidth / 2), signY + 22f, paint)

        // Linha empresa
        val empX = pageWidth - margin - 20f - signWidth
        canvas.drawLine(empX, signY, empX + signWidth, signY, paint)
        canvas.drawText("Showcial Media", empX + (signWidth / 2), signY + 12f, paint)
        canvas.drawText("Jorge Leandro de Jesus Braga", empX + (signWidth / 2), signY + 22f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 10. RODAPÉ
        paint.color = Color.rgb(150, 150, 150)
        paint.textSize = 7.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Showcial Media · CNPJ 34.741.143/0001-76 · Pix: Jorge Leandro de Jesus Braga · OS Nº ${ordem.numeroFormatado}", pageWidth / 2f, 816f, paint)

        document.finishPage(page)

        // Salvar em arquivo
        val pdfDir = File(context.cacheDir, "pdf_os").apply { mkdirs() }
        val arquivo = File(pdfDir, "OS_${ordem.numeroFormatado}.pdf")
        FileOutputStream(arquivo).use { out ->
            document.writeTo(out)
        }
        document.close()
        arquivo
    }

    suspend fun gerarPdfRelatorio(
        context: Context,
        tituloPeriodo: String,
        ordens: List<Ordem>,
        caixaRecebido: Double,
        valorTotalGeral: Double
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // A4 Paisagem
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        val margin = 36f
        val pageWidth = 842f
        var y = margin

        // Cabeçalho
        paint.color = Color.rgb(27, 122, 133)
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("SHOWCIAL MEDIA — RELATÓRIO FINANCEIRO", margin, y + 16f, paint)

        paint.color = Color.rgb(80, 80, 80)
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        canvas.drawText("Período: $tituloPeriodo  |  Gerado em ${LocalDate.now().format(dateFormatter)}", margin, y + 32f, paint)

        // Cartões Caixa e Total
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(46, 125, 50)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Caixa Recebido: ${formatarMoeda(caixaRecebido)}", pageWidth - margin, y + 16f, paint)

        paint.color = Color.rgb(35, 31, 28)
        paint.textSize = 12f
        canvas.drawText("Valor Total das OS: ${formatarMoeda(valorTotalGeral)}", pageWidth - margin, y + 32f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += 50f
        paint.color = Color.rgb(142, 230, 0)
        paint.strokeWidth = 2f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 16f

        // Tabela cabeçalho
        paint.color = Color.rgb(240, 240, 235)
        canvas.drawRect(margin, y, pageWidth - margin, y + 20f, paint)

        paint.color = Color.rgb(35, 31, 28)
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("Nº OS", margin + 8f, y + 14f, paint)
        canvas.drawText("DATA", margin + 65f, y + 14f, paint)
        canvas.drawText("CLIENTE", margin + 140f, y + 14f, paint)
        canvas.drawText("SERVIÇO", margin + 280f, y + 14f, paint)
        canvas.drawText("SITUAÇÃO", margin + 450f, y + 14f, paint)
        canvas.drawText("VALOR", margin + 550f, y + 14f, paint)
        canvas.drawText("PAGO", margin + 640f, y + 14f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("FALTA", pageWidth - margin - 8f, y + 14f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += 24f

        paint.textSize = 8f
        paint.isFakeBoldText = false

        for (o in ordens.take(24)) {
            val clienteTxt = if (o.cliente.length > 22) o.cliente.take(20) + "…" else o.cliente
            val servicoTxt = if (o.servico.length > 25) o.servico.take(23) + "…" else o.servico

            paint.color = Color.rgb(50, 50, 50)
            canvas.drawText(o.numeroFormatado, margin + 8f, y, paint)
            canvas.drawText(formatarData(o.data), margin + 65f, y, paint)
            canvas.drawText(clienteTxt, margin + 140f, y, paint)
            canvas.drawText(servicoTxt, margin + 280f, y, paint)
            canvas.drawText(o.status.label, margin + 450f, y, paint)
            canvas.drawText(formatarMoeda(o.valor), margin + 550f, y, paint)
            paint.color = Color.rgb(46, 125, 50)
            canvas.drawText(formatarMoeda(o.totalPago), margin + 640f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = if (o.quitada) Color.rgb(120, 120, 120) else Color.rgb(138, 43, 6)
            canvas.drawText(if (o.quitada) "QUITADA" else formatarMoeda(o.falta), pageWidth - margin - 8f, y, paint)
            paint.textAlign = Paint.Align.LEFT

            y += 15f
            if (y > 550f) break
        }

        // Rodapé
        paint.color = Color.rgb(140, 140, 140)
        paint.textSize = 7.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Showcial Media · CNPJ 34.741.143/0001-76 · Relatório de Ordens de Serviço", pageWidth / 2f, 575f, paint)

        document.finishPage(page)

        val pdfDir = File(context.cacheDir, "relatorios").apply { mkdirs() }
        val arquivo = File(pdfDir, "Relatorio_${System.currentTimeMillis()}.pdf")
        FileOutputStream(arquivo).use { out ->
            document.writeTo(out)
        }
        document.close()
        arquivo
    }
}
