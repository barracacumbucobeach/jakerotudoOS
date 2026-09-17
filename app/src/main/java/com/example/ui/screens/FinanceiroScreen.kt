package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ordem
import com.example.ui.components.OrdemCard
import com.example.ui.theme.JakeroTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

enum class SubAbaFinanceiro(val label: String) {
    DIA("Dia"),
    MES("Mês"),
    ANO("Ano")
}

@Composable
fun FinanceiroScreen(
    ordens: List<Ordem>,
    onOrdemClick: (Long) -> Unit,
    onExportarRelatorio: (titulo: String, lista: List<Ordem>, caixa: Double, total: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    var subAba by remember { mutableStateOf(SubAbaFinanceiro.DIA) }

    var dataSelecionada by remember { mutableStateOf(LocalDate.now()) }
    var mesSelecionado by remember { mutableStateOf(YearMonth.now()) }
    var anoSelecionado by remember { mutableStateOf(LocalDate.now().year) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.pageBackground)
    ) {
        // Sub-abas (Dia · Mês · Ano)
        TabRow(
            selectedTabIndex = subAba.ordinal,
            containerColor = palette.surfaceBackground,
            contentColor = palette.brandTeal,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subAba.ordinal]),
                    color = palette.brandLime,
                    height = 3.dp
                )
            }
        ) {
            SubAbaFinanceiro.values().forEach { tab ->
                Tab(
                    selected = subAba == tab,
                    onClick = { subAba = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (subAba == tab) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        when (subAba) {
            SubAbaFinanceiro.DIA -> {
                FinanceiroDiaView(
                    ordens = ordens,
                    dataSelecionada = dataSelecionada,
                    onDataSelecionadaChange = { dataSelecionada = it },
                    onOrdemClick = onOrdemClick,
                    onExportar = {
                        val ordensDoDia = ordens.filter { o ->
                            o.data == dataSelecionada || o.pagamentos.any { it.data == dataSelecionada }
                        }
                        val caixa = calcularCaixaDia(ordens, dataSelecionada)
                        val total = ordens.filter { it.data == dataSelecionada }.sumOf { it.valor }
                        val diaFormatado = String.format(Locale("pt", "BR"), "%02d/%02d/%04d", dataSelecionada.dayOfMonth, dataSelecionada.monthValue, dataSelecionada.year)
                        onExportarRelatorio("Dia $diaFormatado", ordensDoDia, caixa, total)
                    }
                )
            }
            SubAbaFinanceiro.MES -> {
                FinanceiroMesView(
                    ordens = ordens,
                    mesSelecionado = mesSelecionado,
                    onMesChange = { mesSelecionado = it },
                    onDiaClick = { dia ->
                        dataSelecionada = dia
                        subAba = SubAbaFinanceiro.DIA
                    },
                    onExportar = {
                        val ordensDoMes = ordens.filter { o ->
                            YearMonth.from(o.data) == mesSelecionado || o.pagamentos.any { YearMonth.from(it.data) == mesSelecionado }
                        }
                        val caixa = calcularCaixaMes(ordens, mesSelecionado)
                        val total = ordens.filter { YearMonth.from(it.data) == mesSelecionado }.sumOf { it.valor }
                        val nomeMes = mesSelecionado.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                        onExportarRelatorio("Mês de $nomeMes/${mesSelecionado.year}", ordensDoMes, caixa, total)
                    }
                )
            }
            SubAbaFinanceiro.ANO -> {
                FinanceiroAnoView(
                    ordens = ordens,
                    anoSelecionado = anoSelecionado,
                    onAnoChange = { anoSelecionado = it },
                    onMesClick = { mes ->
                        mesSelecionado = YearMonth.of(anoSelecionado, mes)
                        subAba = SubAbaFinanceiro.MES
                    },
                    onExportar = {
                        val ordensDoAno = ordens.filter { o ->
                            o.data.year == anoSelecionado || o.pagamentos.any { it.data.year == anoSelecionado }
                        }
                        val caixa = calcularCaixaAno(ordens, anoSelecionado)
                        val total = ordens.filter { it.data.year == anoSelecionado }.sumOf { it.valor }
                        onExportarRelatorio("Ano de $anoSelecionado", ordensDoAno, caixa, total)
                    }
                )
            }
        }
    }
}

// ---------------------- SUB-ABA DIA ----------------------

@Composable
private fun FinanceiroDiaView(
    ordens: List<Ordem>,
    dataSelecionada: LocalDate,
    onDataSelecionadaChange: (LocalDate) -> Unit,
    onOrdemClick: (Long) -> Unit,
    onExportar: () -> Unit
) {
    val palette = JakeroTheme.palette
    val yearMonth = YearMonth.from(dataSelecionada)
    val totalDiasNoMes = yearMonth.lengthOfMonth()
    val diasList = (1..totalDiasNoMes).map { yearMonth.atDay(it) }

    val listState = rememberLazyListState()

    LaunchedEffect(dataSelecionada) {
        val targetIndex = max(0, dataSelecionada.dayOfMonth - 3)
        listState.animateScrollToItem(targetIndex)
    }

    val caixaDia = calcularCaixaDia(ordens, dataSelecionada)
    val totalDia = ordens.filter { it.data == dataSelecionada }.sumOf { it.valor }
    val ordensDoDia = ordens.filter { it.data == dataSelecionada || it.pagamentos.any { p -> p.data == dataSelecionada } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Seletor de Mês e Ano
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onDataSelecionadaChange(dataSelecionada.minusMonths(1)) }) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Mês anterior", tint = palette.textColor)
                }

                val mesNome = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                Text(
                    text = "$mesNome ${yearMonth.year}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textColor
                )

                IconButton(onClick = { onDataSelecionadaChange(dataSelecionada.plusMonths(1)) }) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Próximo mês", tint = palette.textColor)
                }
            }
        }

        // Faixa horizontal com dias do mês
        item {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(diasList) { dia ->
                    val isSelected = dia == dataSelecionada
                    val temLancamento = ordens.any { it.data == dia || it.pagamentos.any { p -> p.data == dia } }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) palette.brandLime else palette.cardBackground,
                        modifier = Modifier
                            .size(width = 50.dp, height = 66.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.brandLime else palette.borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDataSelecionadaChange(dia) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = dia.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).take(3).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) palette.brandLimeText.copy(alpha = 0.8f) else palette.textSecondaryColor
                            )

                            Text(
                                text = "${dia.dayOfMonth}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) palette.brandLimeText else palette.textColor
                            )

                            // Ponto verde se tem lançamentos
                            if (temLancamento) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) palette.brandLimeText else Color(0xFF2E7D32))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }
        }

        // Cartões: Caixa do Dia & Valor Total
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.darkBlockBackground,
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "CAIXA DO DIA",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA49C92)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", caixaDia),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8EE600)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.brandLime,
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "VALOR TOTAL",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.brandLimeText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", totalDia),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.brandLimeText
                        )
                    }
                }
            }
        }

        // Botão Exportar Relatório
        item {
            Button(
                onClick = onExportar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.surfaceBackground,
                    contentColor = palette.brandTeal
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar relatório do dia", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Ordens do Dia
        item {
            Text(
                text = "ORDENS E LANÇAMENTOS DO DIA (${ordensDoDia.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textColor,
                letterSpacing = 0.5.sp
            )
        }

        if (ordensDoDia.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Nenhum movimento registrado nesta data.",
                        fontSize = 13.sp,
                        color = palette.textSecondaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            items(ordensDoDia, key = { it.id }) { ordem ->
                OrdemCard(ordem = ordem, onClick = { onOrdemClick(ordem.id) })
            }
        }
    }
}

// ---------------------- SUB-ABA MÊS ----------------------

@Composable
private fun FinanceiroMesView(
    ordens: List<Ordem>,
    mesSelecionado: YearMonth,
    onMesChange: (YearMonth) -> Unit,
    onDiaClick: (LocalDate) -> Unit,
    onExportar: () -> Unit
) {
    val palette = JakeroTheme.palette

    val caixaMes = calcularCaixaMes(ordens, mesSelecionado)
    val totalMes = ordens.filter { YearMonth.from(it.data) == mesSelecionado }.sumOf { it.valor }
    val aReceberMes = max(0.0, totalMes - caixaMes)

    val diasNoMes = mesSelecionado.lengthOfMonth()
    val diasList = (1..diasNoMes).map { mesSelecionado.atDay(it) }
    val maxFaturamentoDia = diasList.maxOfOrNull { dia ->
        calcularCaixaDia(ordens, dia)
    }?.coerceAtLeast(1.0) ?: 1.0

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Seletor de Mês
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onMesChange(mesSelecionado.minusMonths(1)) }) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Mês anterior", tint = palette.textColor)
                }

                val mesNome = mesSelecionado.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                Text(
                    text = "$mesNome ${mesSelecionado.year}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textColor
                )

                IconButton(onClick = { onMesChange(mesSelecionado.plusMonths(1)) }) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Próximo mês", tint = palette.textColor)
                }
            }
        }

        // 3 Cartões: Caixa Recebido, Total em Pedidos, A Receber
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ResumoMiniCard("CAIXA", caixaMes, Color(0xFF2E7D32), Modifier.weight(1f))
                ResumoMiniCard("TOTAL", totalMes, palette.textColor, Modifier.weight(1f))
                ResumoMiniCard("A RECEBER", aReceberMes, Color(0xFF8A2B06), Modifier.weight(1f))
            }
        }

        // Botão Exportar
        item {
            Button(
                onClick = onExportar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.surfaceBackground,
                    contentColor = palette.brandTeal
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar relatório do mês", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Lista dia a dia com barras de faturamento
        item {
            Text(
                text = "FATURAMENTO DIA A DIA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textColor,
                letterSpacing = 0.5.sp
            )
        }

        items(diasList) { dia ->
            val caixaDia = calcularCaixaDia(ordens, dia)
            val totalOrdensDia = ordens.count { it.data == dia }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.cardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.borderColor, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDiaClick(dia) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "${dia.dayOfMonth}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor,
                        modifier = Modifier.width(28.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Barra proporcional
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.borderColor.copy(alpha = 0.4f))
                    ) {
                        val proporcao = (caixaDia / maxFaturamentoDia).toFloat().coerceIn(0f, 1f)
                        if (proporcao > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(proporcao)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.brandLime)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", caixaDia),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (caixaDia > 0) Color(0xFF2E7D32) else palette.textSecondaryColor
                        )
                        if (totalOrdensDia > 0) {
                            Text(
                                text = "$totalOrdensDia OS",
                                fontSize = 10.sp,
                                color = palette.textSecondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- SUB-ABA ANO ----------------------

@Composable
private fun FinanceiroAnoView(
    ordens: List<Ordem>,
    anoSelecionado: Int,
    onAnoChange: (Int) -> Unit,
    onMesClick: (Int) -> Unit,
    onExportar: () -> Unit
) {
    val palette = JakeroTheme.palette

    val caixaAno = calcularCaixaAno(ordens, anoSelecionado)
    val totalAno = ordens.filter { it.data.year == anoSelecionado }.sumOf { it.valor }
    val aReceberAno = max(0.0, totalAno - caixaAno)
    val totalOsAno = ordens.count { it.data.year == anoSelecionado }

    val mesesList = (1..12).toList()
    val maxCaixaMes = mesesList.maxOfOrNull { m ->
        calcularCaixaMes(ordens, YearMonth.of(anoSelecionado, m))
    }?.coerceAtLeast(1.0) ?: 1.0

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Seletor de Ano
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onAnoChange(anoSelecionado - 1) }) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Ano anterior", tint = palette.textColor)
                }

                Text(
                    text = "$anoSelecionado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textColor
                )

                IconButton(onClick = { onAnoChange(anoSelecionado + 1) }) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Próximo ano", tint = palette.textColor)
                }
            }
        }

        // Cartões Anuais
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ResumoMiniCard("FATURADO", totalAno, palette.textColor, Modifier.weight(1f))
                ResumoMiniCard("RECEBIDO", caixaAno, Color(0xFF2E7D32), Modifier.weight(1f))
                ResumoMiniCard("A RECEBER", aReceberAno, Color(0xFF8A2B06), Modifier.weight(1f))
            }
        }

        // Botão Exportar
        item {
            Button(
                onClick = onExportar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.surfaceBackground,
                    contentColor = palette.brandTeal
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar relatório anual ($totalOsAno OS)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // 12 Meses com Barras
        item {
            Text(
                text = "FATURAMENTO MENSAL ($anoSelecionado)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textColor,
                letterSpacing = 0.5.sp
            )
        }

        items(mesesList) { mesNum ->
            val ym = YearMonth.of(anoSelecionado, mesNum)
            val caixaM = calcularCaixaMes(ordens, ym)
            val nomeMes = ym.month.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).take(3).uppercase()

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.cardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.borderColor, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onMesClick(mesNum) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = nomeMes,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor,
                        modifier = Modifier.width(42.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.borderColor.copy(alpha = 0.4f))
                    ) {
                        val proporcao = (caixaM / maxCaixaMes).toFloat().coerceIn(0f, 1f)
                        if (proporcao > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(proporcao)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.brandLime)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %,.2f", caixaM),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (caixaM > 0) Color(0xFF2E7D32) else palette.textSecondaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumoMiniCard(
    titulo: String,
    valor: Double,
    corValor: Color,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.cardBackground,
        modifier = modifier
            .border(1.dp, palette.borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = titulo,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondaryColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale("pt", "BR"), "R$ %,.2f", valor),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = corValor
            )
        }
    }
}

// Helpers de cálculo financeiro
private fun calcularCaixaDia(ordens: List<Ordem>, dia: LocalDate): Double {
    var caixa = 0.0
    for (o in ordens) {
        if (o.data == dia && o.entrada > 0) {
            caixa += o.entrada
        }
        for (p in o.pagamentos) {
            if (p.data == dia) {
                caixa += p.valor
            }
        }
    }
    return caixa
}

private fun calcularCaixaMes(ordens: List<Ordem>, mes: YearMonth): Double {
    var caixa = 0.0
    for (o in ordens) {
        if (YearMonth.from(o.data) == mes && o.entrada > 0) {
            caixa += o.entrada
        }
        for (p in o.pagamentos) {
            if (YearMonth.from(p.data) == mes) {
                caixa += p.valor
            }
        }
    }
    return caixa
}

private fun calcularCaixaAno(ordens: List<Ordem>, ano: Int): Double {
    var caixa = 0.0
    for (o in ordens) {
        if (o.data.year == ano && o.entrada > 0) {
            caixa += o.entrada
        }
        for (p in o.pagamentos) {
            if (p.data.year == ano) {
                caixa += p.valor
            }
        }
    }
    return caixa
}
