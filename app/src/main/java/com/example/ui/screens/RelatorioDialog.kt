package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Ordem
import com.example.ui.theme.JakeroTheme
import java.util.Locale

enum class FormatoRelatorio {
    PDF,
    CSV
}

@Composable
fun RelatorioDialog(
    tituloPeriodo: String,
    ordens: List<Ordem>,
    caixaRecebido: Double,
    valorTotal: Double,
    onDismiss: () -> Unit,
    onExportarPdf: () -> Unit,
    onExportarCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    var formato by remember { mutableStateOf(FormatoRelatorio.PDF) }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Topo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = palette.brandTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exportar Relatório",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textColor
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                // Resumo do Período
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.cardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.borderColor, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tituloPeriodo,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Text(
                            text = "${ordens.size} ordem(ns) de serviço incluída(s)",
                            fontSize = 12.5.sp,
                            color = palette.textSecondaryColor
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Caixa: ${String.format(Locale("pt", "BR"), "R$ %,.2f", caixaRecebido)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Total OS: ${String.format(Locale("pt", "BR"), "R$ %,.2f", valorTotal)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                        }
                    }
                }

                // Escolha do Formato
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FORMATO DO ARQUIVO",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondaryColor,
                        letterSpacing = 0.5.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = formato == FormatoRelatorio.PDF,
                            onClick = { formato = FormatoRelatorio.PDF },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("PDF (A4 Paisagem)", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.brandLime,
                                selectedLabelColor = palette.brandLimeText
                            )
                        )

                        FilterChip(
                            selected = formato == FormatoRelatorio.CSV,
                            onClick = { formato = FormatoRelatorio.CSV },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Excel / CSV", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.brandLime,
                                selectedLabelColor = palette.brandLimeText
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Botão Exportar
                Button(
                    onClick = {
                        if (formato == FormatoRelatorio.PDF) {
                            onExportarPdf()
                        } else {
                            onExportarCsv()
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.brandLime,
                        contentColor = palette.brandLimeText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Exportar e Compartilhar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
