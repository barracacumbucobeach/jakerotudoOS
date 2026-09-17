package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Ordem
import com.example.service.ExportHelper
import com.example.ui.theme.JakeroTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WhatsAppShareDialog(
    ordem: Ordem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Opções de personalização
    var incValores by remember { mutableStateOf(true) }
    var incVencimento by remember { mutableStateOf(true) }
    var incEntrega by remember { mutableStateOf(true) }
    var incObservacoes by remember { mutableStateOf(ordem.observacoes.isNotBlank()) }
    var incAvisoFotos by remember { mutableStateOf(ordem.fotos.isNotEmpty()) }
    var incDadosEmpresa by remember { mutableStateOf(true) }

    // Geração do texto da mensagem
    val mensagem = remember(incValores, incVencimento, incEntrega, incObservacoes, incAvisoFotos, incDadosEmpresa) {
        val sb = StringBuilder()
        sb.append("Olá, *${ordem.cliente}*!\n\n")
        sb.append("Aqui estão as informações da sua *Ordem de Serviço Nº ${ordem.numeroFormatado}* na *Jakero Tudo*:\n\n")
        sb.append("• *Serviço:* ${ordem.servico}\n")
        sb.append("• *Situação:* ${ordem.status.label}\n")

        if (incEntrega && ordem.entregaPrevista != null) {
            sb.append("• *Previsão de Entrega:* ${ordem.entregaPrevista.format(dateFormatter)}\n")
        }

        if (incValores) {
            sb.append("\n*Resumo Financeiro:*\n")
            sb.append("• *Valor Total:* R$ ${String.format(Locale("pt", "BR"), "%.2f", ordem.valor)}\n")
            sb.append("• *Total Pago:* R$ ${String.format(Locale("pt", "BR"), "%.2f", ordem.totalPago)}\n")
            if (ordem.quitada) {
                sb.append("• *Situação:* QUITADO ✅\n")
            } else {
                sb.append("• *Saldo Restante:* R$ ${String.format(Locale("pt", "BR"), "%.2f", ordem.falta)}\n")
            }
        }

        if (incVencimento && !ordem.quitada) {
            if (ordem.isParcelado) {
                val proxParcela = ordem.parcelasCalculadas().firstOrNull { it.status != com.example.data.model.ParcelaStatus.PAGA }
                if (proxParcela != null) {
                    sb.append("• *Próxima Parcela:* ${proxParcela.numero}/${proxParcela.totalParcelas} (R$ ${String.format(Locale("pt", "BR"), "%.2f", proxParcela.valor)}) - Vencimento: ${proxParcela.vencimento.format(dateFormatter)}\n")
                }
            } else if (ordem.vencimento != null) {
                sb.append("• *Vencimento:* ${ordem.vencimento.format(dateFormatter)}\n")
            }
        }

        if (incObservacoes && ordem.observacoes.isNotBlank()) {
            sb.append("\n*Observações:*\n${ordem.observacoes}\n")
        }

        if (incAvisoFotos && ordem.fotos.isNotEmpty()) {
            sb.append("\n📸 *Fotos de referência (${ordem.fotos.size}):*\n")
            ordem.fotos.forEachIndexed { idx, foto ->
                val link = if (foto.startsWith("http://") || foto.startsWith("https://")) {
                    foto
                } else {
                    com.example.service.ImageStorageHelper.obterUrlPublica(foto, "https://muxvtacywjfgclfvtgia.supabase.co")
                }
                sb.append("• Imagem ${idx + 1}: $link\n")
            }
        }

        if (incDadosEmpresa) {
            sb.append("\n_Showcial Media_\nCNPJ: 34.741.143/0001-76\nChave Pix (CNPJ): 34.741.143/0001-76\nFavorecido Pix: Jorge Leandro de Jesus Braga")
        }

        sb.toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surfaceBackground,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Topo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enviar no WhatsApp",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textColor
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    // Opções a incluir
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "O QUE INCLUIR NA MENSAGEM:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )

                            CheckItem("Valores e saldo restante", incValores) { incValores = it }
                            CheckItem("Previsão de entrega", incEntrega) { incEntrega = it }
                            CheckItem("Vencimento / Parcelas", incVencimento) { incVencimento = it }
                            if (ordem.observacoes.isNotBlank()) {
                                CheckItem("Observações", incObservacoes) { incObservacoes = it }
                            }
                            if (ordem.fotos.isNotEmpty()) {
                                CheckItem("Aviso de fotos anexas", incAvisoFotos) { incAvisoFotos = it }
                            }
                            CheckItem("Dados da empresa e Pix (CNPJ)", incDadosEmpresa) { incDadosEmpresa = it }
                        }
                    }

                    // Prévia do texto
                    item {
                        Text(
                            text = "PRÉVIA DA MENSAGEM:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondaryColor,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.cardBackground)
                                .border(1.dp, palette.borderColor, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = mensagem,
                                fontSize = 12.sp,
                                color = palette.textColor,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botões de Ação
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (ordem.fotos.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                ExportHelper.compartilharFotos(context, ordem.fotos)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enviar fotos", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            ExportHelper.abrirWhatsApp(context, ordem.telefone, mensagem)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                    ) {
                        Text("Abrir WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckItem(
    titulo: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = JakeroTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = palette.brandLime,
                checkmarkColor = palette.brandLimeText
            ),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = titulo, fontSize = 12.5.sp, color = palette.textColor)
    }
}
