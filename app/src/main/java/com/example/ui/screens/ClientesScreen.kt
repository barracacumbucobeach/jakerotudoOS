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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ordem
import com.example.ui.theme.JakeroTheme
import java.util.Locale

data class ClienteResumo(
    val nome: String,
    val telefone: String,
    val totalOrdens: Int,
    val valorTotal: Double,
    val totalPago: Double,
    val saldoDevedor: Double
) {
    val estaEmDia: Boolean
        get() = saldoDevedor <= 0.009
}

@Composable
fun ClientesScreen(
    ordens: List<Ordem>,
    onClienteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    var buscaCliente by remember { mutableStateOf("") }

    // Agrupamento por cliente
    val clientesResumo = remember(ordens) {
        val grupos = ordens.groupBy { it.cliente.trim() }
        grupos.map { (nome, lista) ->
            val telefone = lista.firstOrNull { it.telefone.isNotBlank() }?.telefone ?: ""
            val totalOrdens = lista.size
            val valorTotal = lista.sumOf { it.valor }
            val totalPago = lista.sumOf { it.totalPago }
            val saldoDevedor = lista.sumOf { it.falta }

            ClienteResumo(
                nome = nome,
                telefone = telefone,
                totalOrdens = totalOrdens,
                valorTotal = valorTotal,
                totalPago = totalPago,
                saldoDevedor = saldoDevedor
            )
        }.sortedByDescending { it.saldoDevedor }
    }

    val clientesFiltrados = clientesResumo.filter { c ->
        buscaCliente.isBlank() ||
                c.nome.contains(buscaCliente, ignoreCase = true) ||
                c.telefone.contains(buscaCliente)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.pageBackground)
    ) {
        // Campo de busca
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = buscaCliente,
                onValueChange = { buscaCliente = it },
                placeholder = { Text("Buscar cliente por nome ou telefone…", fontSize = 13.5.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = palette.textSecondaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (buscaCliente.isNotBlank()) {
                        IconButton(onClick = { buscaCliente = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpar",
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

        // Lista de Clientes
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (clientesFiltrados.isEmpty()) {
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
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = palette.textSecondaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum cliente encontrado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                    }
                }
            } else {
                items(clientesFiltrados, key = { it.nome }) { cliente ->
                    ClienteCard(
                        cliente = cliente,
                        onClick = { onClienteClick(cliente.nome) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClienteCard(
    cliente: ClienteResumo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.cardBackground,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(palette.brandTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cliente.nome.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.brandTeal
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cliente.nome,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )

                if (cliente.telefone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = palette.textSecondaryColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cliente.telefone,
                            fontSize = 12.sp,
                            color = palette.textSecondaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${cliente.totalOrdens} OS · Total: ${String.format(Locale("pt", "BR"), "R$ %,.2f", cliente.valorTotal)}",
                        fontSize = 12.sp,
                        color = palette.textSecondaryColor
                    )
                }
            }

            // Status: em dia ou deve R$ X
            Column(horizontalAlignment = Alignment.End) {
                if (cliente.estaEmDia) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x224CAF50))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EM DIA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x228A2B06))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Deve ${String.format(Locale("pt", "BR"), "R$ %,.2f", cliente.saldoDevedor)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A2B06)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Ver ordens",
                    tint = palette.textSecondaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
