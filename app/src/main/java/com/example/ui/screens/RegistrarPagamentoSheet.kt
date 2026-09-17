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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ordem
import com.example.ui.theme.JakeroTheme
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarPagamentoSheet(
    ordem: Ordem,
    onDismiss: () -> Unit,
    onConfirmar: (Double, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    var dataPagamento by remember { mutableStateOf(LocalDate.now()) }
    var valorStr by remember { mutableStateOf(String.format(Locale("pt", "BR"), "%.2f", ordem.falta)) }

    val valorNum = valorStr.replace(",", ".").toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surfaceBackground,
        modifier = modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = palette.brandTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Registrar Pagamento",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textColor
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            // Resumo da OS e Saldo
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.cardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "OS Nº ${ordem.numeroFormatado} · ${ordem.cliente}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                    Text(
                        text = ordem.servico,
                        fontSize = 12.5.sp,
                        color = palette.textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "FALTA RECEBER:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondaryColor
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.falta),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8A2B06)
                        )
                    }
                }
            }

            // Data do Pagamento
            DataSelectorField(
                label = "Data do Pagamento",
                data = dataPagamento,
                onDataSelecionada = { dataPagamento = it ?: LocalDate.now() },
                modifier = Modifier.fillMaxWidth()
            )

            // Campo Valor
            OutlinedTextField(
                value = valorStr,
                onValueChange = { valorStr = it },
                label = { Text("Valor Recebido (R$) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            // Atalho: Quitar Restante
            OutlinedButton(
                onClick = {
                    valorStr = String.format(Locale("pt", "BR"), "%.2f", ordem.falta)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quitar o restante (${String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.falta)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.brandTeal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botão Confirmar
            Button(
                onClick = {
                    if (valorNum > 0.0) {
                        onConfirmar(valorNum, dataPagamento)
                    }
                },
                enabled = valorNum > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brandLime,
                    contentColor = palette.brandLimeText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Confirmar Pagamento",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
