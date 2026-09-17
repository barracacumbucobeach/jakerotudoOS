package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StatusOrdem
import com.example.ui.theme.JakeroTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StepProgressBar(
    status: StatusOrdem,
    entregaReal: LocalDate?,
    onAvancarEtapa: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    val etapas = listOf(StatusOrdem.ABERTA, StatusOrdem.ANDAMENTO, StatusOrdem.ENTREGUE)
    val indiceAtual = etapas.indexOf(status)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.cardBackground,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "ENTREGA DO SERVIÇO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondaryColor,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Linha e Círculos das 3 etapas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                etapas.forEachIndexed { index, etapa ->
                    val isActive = index <= indiceAtual
                    val isCurrent = index == indiceAtual
                    val circleColor = when {
                        index < indiceAtual -> Color(0xFF2E7D32) // Verde concluído
                        isCurrent -> when (etapa) {
                            StatusOrdem.ABERTA -> Color(0xFFE65100)
                            StatusOrdem.ANDAMENTO -> Color(0xFF0288D1)
                            StatusOrdem.ENTREGUE -> Color(0xFF2E7D32)
                        }
                        else -> Color(0xFFCCCCCC)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(circleColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < indiceAtual || (index == 2 && status == StatusOrdem.ENTREGUE)) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = etapa.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) palette.textColor else palette.textSecondaryColor
                        )
                    }

                    if (index < etapas.size - 1) {
                        val lineColor = if (index < indiceAtual) Color(0xFF2E7D32) else Color(0xFFE0E0E0)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(lineColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de Avanço ou Informação de Conclusão
            if (status != StatusOrdem.ENTREGUE) {
                val proximoTexto = when (status) {
                    StatusOrdem.ABERTA -> "Iniciar serviço (Mudar para Em andamento)"
                    StatusOrdem.ANDAMENTO -> "Concluir serviço (Marcar como Entregue)"
                    StatusOrdem.ENTREGUE -> ""
                }

                Button(
                    onClick = onAvancarEtapa,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.brandLime,
                        contentColor = palette.brandLimeText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = proximoTexto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                val dataFormatada = entregaReal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Hoje"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x224CAF50))
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Serviço entregue com sucesso em $dataFormatada",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}
