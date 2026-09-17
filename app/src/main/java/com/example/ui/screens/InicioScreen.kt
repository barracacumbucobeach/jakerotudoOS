package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AvisoItem
import com.example.data.model.Ordem
import com.example.ui.components.OrdemCard
import com.example.ui.theme.JakeroTheme
import com.example.ui.viewmodel.FiltroOrdens
import java.util.Locale

@Composable
fun InicioScreen(
    caixaHoje: Double,
    totalHoje: Double,
    contadores: Triple<Int, Int, Int>,
    ultimasOrdens: List<Ordem>,
    avisos: List<AvisoItem>,
    temAvisoUrgente: Boolean,
    onAvisosClick: () -> Unit,
    onContadorClick: (FiltroOrdens) -> Unit,
    onVerTodasOrdens: () -> Unit,
    onOrdemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(palette.pageBackground)
    ) {
        // 1. Faixa de Avisos (se houver pendências)
        if (avisos.isNotEmpty()) {
            item {
                val bannerBg = if (temAvisoUrgente) Color(0xFFFFEBEE) else Color(0xFFE1F5FE)
                val bannerBorder = if (temAvisoUrgente) Color(0xFFFFCDD2) else Color(0xFFB3E5FC)
                val bannerTextColor = if (temAvisoUrgente) Color(0xFFC62828) else Color(0xFF0277BD)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = bannerBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, bannerBorder, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onAvisosClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(bannerTextColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = bannerTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (temAvisoUrgente) "${avisos.size} pendência(s) encontrada(s)" else "${avisos.size} lembrete(s)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = bannerTextColor
                            )
                            Text(
                                text = avisos.first().descricao,
                                fontSize = 12.sp,
                                color = bannerTextColor.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Ver avisos",
                            tint = bannerTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Cartões Principais: CAIXA DE HOJE e TOTAL DE HOJE
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // CAIXA DE HOJE (Fundo Escuro)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.darkBlockBackground,
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CAIXA HOJE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA49C92),
                                letterSpacing = 0.5.sp
                            )
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF8EE600),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %,.2f", caixaHoje),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF3F0EA)
                            )
                            Text(
                                text = "recebido",
                                fontSize = 11.sp,
                                color = Color(0xFFA49C92)
                            )
                        }
                    }
                }

                // TOTAL DE HOJE (Fundo Verde-Limão)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.brandLime,
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TOTAL HOJE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.brandLimeText,
                                letterSpacing = 0.5.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = palette.brandLimeText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %,.2f", totalHoje),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.brandLimeText
                            )
                            Text(
                                text = "em pedidos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.brandLimeText.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Três Contadores Clicáveis
        item {
            val (aFazer, emAndamento, aReceber) = contadores
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ContadorCard(
                    titulo = "A Fazer",
                    quantidade = aFazer,
                    icone = Icons.Default.Assignment,
                    corDestaque = Color(0xFFE65100),
                    onClick = { onContadorClick(FiltroOrdens.ABERTAS) },
                    modifier = Modifier.weight(1f)
                )

                ContadorCard(
                    titulo = "Andamento",
                    quantidade = emAndamento,
                    icone = Icons.Default.HourglassTop,
                    corDestaque = Color(0xFF0288D1),
                    onClick = { onContadorClick(FiltroOrdens.ANDAMENTO) },
                    modifier = Modifier.weight(1f)
                )

                ContadorCard(
                    titulo = "A Receber",
                    quantidade = aReceber,
                    icone = Icons.Default.PendingActions,
                    corDestaque = Color(0xFF8A2B06),
                    onClick = { onContadorClick(FiltroOrdens.A_RECEBER) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Seção Últimas Ordens
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "ÚLTIMAS ORDENS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor,
                    letterSpacing = 0.5.sp
                )

                TextButton(onClick = onVerTodasOrdens) {
                    Text(
                        text = "Ver todas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.brandTeal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = palette.brandTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (ultimasOrdens.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Nenhuma ordem cadastrada ainda",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toque em + para abrir a primeira OS",
                            fontSize = 12.sp,
                            color = palette.textSecondaryColor
                        )
                    }
                }
            }
        } else {
            items(ultimasOrdens.take(5), key = { it.id }) { ordem ->
                OrdemCard(
                    ordem = ordem,
                    onClick = { onOrdemClick(ordem.id) }
                )
            }
        }
    }
}

@Composable
private fun ContadorCard(
    titulo: String,
    quantidade: Int,
    icone: ImageVector,
    corDestaque: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.cardBackground,
        modifier = modifier
            .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = corDestaque,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$quantidade",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textColor
            )

            Text(
                text = titulo,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondaryColor
            )
        }
    }
}
