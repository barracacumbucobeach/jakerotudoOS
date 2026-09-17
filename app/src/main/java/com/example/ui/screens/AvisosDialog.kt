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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AvisoItem
import com.example.data.model.TipoAviso
import com.example.ui.theme.JakeroTheme

@Composable
fun AvisosDialog(
    avisos: List<AvisoItem>,
    frequenciaDias: Int,
    onDismiss: () -> Unit,
    onOrdemClick: (Long) -> Unit,
    onReiniciarLembretes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

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
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = palette.brandTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Avisos e Lembretes",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textColor
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (avisos.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x224CAF50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tudo em dia!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nenhuma pendência ou cobrança no momento.",
                            fontSize = 12.sp,
                            color = palette.textSecondaryColor
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(avisos, key = { it.id }) { aviso ->
                            AvisoItemCard(
                                aviso = aviso,
                                onClick = {
                                    onDismiss()
                                    onOrdemClick(aviso.ordemId)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botão "Já vi · lembrar de novo em X dias"
                    OutlinedButton(
                        onClick = onReiniciarLembretes,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Já vi · Lembrar de novo em $frequenciaDias dias",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.brandTeal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoItemCard(
    aviso: AvisoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    val (bgColor, borderColor, iconColor) = when (aviso.tipo) {
        TipoAviso.ATRASO_PAGAMENTO -> Triple(Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFC62828))
        TipoAviso.ENTREGA_ATRASADA -> Triple(Color(0xFFE1F5FE), Color(0xFFB3E5FC), Color(0xFF0277BD))
        TipoAviso.A_VENCER -> Triple(Color(0xFFE1F5FE), Color(0xFFB3E5FC), Color(0xFF0277BD))
        TipoAviso.SERVICO_A_CONCLUIR -> Triple(palette.cardBackground, palette.borderColor, palette.brandTeal)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = aviso.titulo.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = iconColor
                    )
                }

                Text(
                    text = "OS ${aviso.ordemNum}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondaryColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = aviso.descricao,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onClick) {
                    Text(
                        text = when (aviso.tipo) {
                            TipoAviso.ATRASO_PAGAMENTO -> "Registrar pagamento"
                            TipoAviso.ENTREGA_ATRASADA -> "Marcar como entregue"
                            TipoAviso.SERVICO_A_CONCLUIR -> "Avançar etapa"
                            TipoAviso.A_VENCER -> "Ver ordem"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }
            }
        }
    }
}
