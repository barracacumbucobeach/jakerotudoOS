package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.Ordem
import com.example.data.model.StatusOrdem
import com.example.ui.components.OrdemCard
import com.example.ui.theme.JakeroTheme
import com.example.ui.viewmodel.FiltroOrdens

@Composable
fun OrdensScreen(
    ordens: List<Ordem>,
    filtroAtual: FiltroOrdens,
    termoBusca: String,
    clienteFiltro: String?,
    onFiltroChange: (FiltroOrdens) -> Unit,
    onBuscaChange: (String) -> Unit,
    onLimparClienteFiltro: () -> Unit,
    onOrdemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    // Filtragem local
    val ordensFiltradas = ordens.filter { ordem ->
        val atendeCliente = clienteFiltro.isNullOrBlank() || ordem.cliente.equals(clienteFiltro, ignoreCase = true)
        val atendeFiltro = when (filtroAtual) {
            FiltroOrdens.TODAS -> true
            FiltroOrdens.ABERTAS -> ordem.status == StatusOrdem.ABERTA
            FiltroOrdens.ANDAMENTO -> ordem.status == StatusOrdem.ANDAMENTO
            FiltroOrdens.ENTREGUES -> ordem.status == StatusOrdem.ENTREGUE
            FiltroOrdens.A_RECEBER -> !ordem.quitada
        }
        val atendeBusca = termoBusca.isBlank() ||
                ordem.cliente.contains(termoBusca, ignoreCase = true) ||
                ordem.servico.contains(termoBusca, ignoreCase = true) ||
                ordem.numeroFormatado.contains(termoBusca) ||
                ordem.telefone.contains(termoBusca)

        atendeCliente && atendeFiltro && atendeBusca
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.pageBackground)
    ) {
        // Campo de Busca
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = termoBusca,
                onValueChange = onBuscaChange,
                placeholder = { Text("Buscar por cliente, OS nº, serviço…", fontSize = 13.5.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = palette.textSecondaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (termoBusca.isNotBlank()) {
                        IconButton(onClick = { onBuscaChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpar busca",
                                tint = palette.textSecondaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.cardBackground,
                    unfocusedContainerColor = palette.cardBackground,
                    focusedBorderColor = palette.brandLime,
                    unfocusedBorderColor = palette.borderColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filtro de Cliente Ativo (se houver)
        if (!clienteFiltro.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.brandTeal.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = palette.brandTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filtrando por: $clienteFiltro",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.brandTeal
                        )
                    }
                    IconButton(
                        onClick = onLimparClienteFiltro,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remover filtro de cliente",
                            tint = palette.brandTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Chips de Filtro: Todas · Abertas · Em andamento · Entregues · A receber
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            FiltroOrdens.values().forEach { filtro ->
                val isSelected = filtro == filtroAtual
                FilterChip(
                    selected = isSelected,
                    onClick = { onFiltroChange(filtro) },
                    label = {
                        Text(
                            text = filtro.label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.brandLime,
                        selectedLabelColor = palette.brandLimeText,
                        containerColor = palette.cardBackground,
                        labelColor = palette.textColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) palette.brandLime else palette.borderColor
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Lista de Ordens
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (ordensFiltradas.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(palette.borderColor.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = palette.textSecondaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhuma ordem encontrada",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tente alterar os filtros ou o termo de busca.",
                            fontSize = 12.5.sp,
                            color = palette.textSecondaryColor
                        )
                    }
                }
            } else {
                items(ordensFiltradas, key = { it.id }) { ordem ->
                    OrdemCard(
                        ordem = ordem,
                        onClick = { onOrdemClick(ordem.id) }
                    )
                }
            }
        }
    }
}
