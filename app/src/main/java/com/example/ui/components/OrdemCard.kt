package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ordem
import com.example.data.model.StatusOrdem
import com.example.ui.theme.JakeroTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OrdemCard(
    ordem: Ordem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val (statusBg, statusTextColor) = when (ordem.status) {
        StatusOrdem.ABERTA -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        StatusOrdem.ANDAMENTO -> Color(0xFFE1F5FE) to Color(0xFF0288D1)
        StatusOrdem.ENTREGUE -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.cardBackground,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linha superior: Nº OS, Data e Badge de Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "OS ${ordem.numeroFormatado}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ordem.data.format(dateFormatter),
                        fontSize = 12.sp,
                        color = palette.textSecondaryColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ordem.status.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cliente e Telefone
            Text(
                text = ordem.cliente,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Serviço
            Text(
                text = ordem.servico,
                fontSize = 13.sp,
                color = palette.textSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Linha inferior: Valor total e Saldo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Valor Total",
                        fontSize = 11.sp,
                        color = palette.textSecondaryColor
                    )
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.valor),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (ordem.quitada) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x224CAF50))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "QUITADA",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    } else {
                        Text(
                            text = "Falta",
                            fontSize = 11.sp,
                            color = palette.textSecondaryColor
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.falta),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8A2B06) // Rust brown
                        )
                    }
                }
            }
        }
    }
}
