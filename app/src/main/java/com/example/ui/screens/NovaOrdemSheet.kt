package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.FrequenciaPlano
import com.example.data.model.Ordem
import com.example.data.model.Plano
import com.example.data.model.StatusOrdem
import com.example.service.ImageStorageHelper
import com.example.ui.theme.JakeroTheme
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaOrdemSheet(
    ordemExistente: Ordem?,
    onDismiss: () -> Unit,
    onSalvar: (Ordem) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Estados dos campos
    var dataPedido by remember { mutableStateOf(ordemExistente?.data ?: LocalDate.now()) }
    var entregaPrevista by remember { mutableStateOf<LocalDate?>(ordemExistente?.entregaPrevista) }
    var cliente by remember { mutableStateOf(ordemExistente?.cliente ?: "") }
    var telefone by remember { mutableStateOf(ordemExistente?.telefone ?: "") }
    var servico by remember { mutableStateOf(ordemExistente?.servico ?: "") }

    var quantidadeStr by remember { mutableStateOf(ordemExistente?.let { String.format(Locale("pt", "BR"), "%.2f", it.quantidade) } ?: "1") }
    var valorUnitarioStr by remember { mutableStateOf(ordemExistente?.let { String.format(Locale("pt", "BR"), "%.2f", it.valorUnitario) } ?: "") }
    var entradaStr by remember { mutableStateOf(ordemExistente?.let { if (it.entrada > 0) String.format(Locale("pt", "BR"), "%.2f", it.entrada) else "" } ?: "") }
    var descontoStr by remember { mutableStateOf(ordemExistente?.let { if (it.desconto > 0) String.format(Locale("pt", "BR"), "%.2f", it.desconto) else "" } ?: "") }
    var observacoes by remember { mutableStateOf(ordemExistente?.observacoes ?: "") }

    var fotos by remember { mutableStateOf(ordemExistente?.fotos ?: emptyList()) }

    // Modalidade de pagamento
    var isParcelado by remember { mutableStateOf(ordemExistente?.isParcelado ?: false) }
    var parcelasQtd by remember { mutableIntStateOf(ordemExistente?.plano?.parcelas ?: 2) }
    var frequenciaPlano by remember { mutableStateOf(ordemExistente?.plano?.frequencia ?: FrequenciaPlano.MENSAL) }
    var primeiroVencimento by remember { mutableStateOf(ordemExistente?.plano?.primeiroVencimento ?: LocalDate.now().plusMonths(1)) }
    var vencimentoAvista by remember { mutableStateOf<LocalDate?>(ordemExistente?.vencimento) }

    // Parsing numérico para cálculo dinâmico
    val qtdNum = quantidadeStr.replace(",", ".").toDoubleOrNull() ?: 1.0
    val vuNum = valorUnitarioStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val descNum = descontoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val entNum = entradaStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val valorTotalCalculado = max(0.0, (qtdNum * vuNum) - descNum)

    // Picker de fotos
    val fotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && fotos.size < 6) {
            scope.launch {
                val caminho = ImageStorageHelper.salvarFotoComprimida(context, uri)
                if (caminho != null) {
                    fotos = fotos + caminho
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surfaceBackground,
        modifier = modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Topo da Sheet
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (ordemExistente == null) "Nova Ordem de Serviço" else "Editar OS Nº ${ordemExistente.numeroFormatado}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Datas: Data do Pedido & Previsão de Entrega
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Data Pedido
                        DataSelectorField(
                            label = "Data do Pedido",
                            data = dataPedido,
                            onDataSelecionada = { dataPedido = it },
                            modifier = Modifier.weight(1f)
                        )

                        // Previsão Entrega
                        DataSelectorField(
                            label = "Previsão Entrega",
                            data = entregaPrevista,
                            onDataSelecionada = { entregaPrevista = it },
                            placeholder = "Definir",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Cliente
                item {
                    OutlinedTextField(
                        value = cliente,
                        onValueChange = { cliente = it },
                        label = { Text("Nome do Cliente *") },
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

                // Telefone / WhatsApp
                item {
                    OutlinedTextField(
                        value = telefone,
                        onValueChange = { telefone = it },
                        label = { Text("WhatsApp / Telefone") },
                        placeholder = { Text("(85) 9 9999-9999") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

                // Serviço
                item {
                    OutlinedTextField(
                        value = servico,
                        onValueChange = { servico = it },
                        label = { Text("Descrição do Serviço *") },
                        placeholder = { Text("Ex: Troca de tela, Manutenção hidráulica, Pintura…") },
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

                // Linha de Valores: Qtd, Valor Unitário, Entrada, Desconto
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = quantidadeStr,
                            onValueChange = { quantidadeStr = it },
                            label = { Text("Qtd") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.cardBackground,
                                unfocusedContainerColor = palette.cardBackground,
                                focusedBorderColor = palette.brandLime,
                                unfocusedBorderColor = palette.borderColor
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = valorUnitarioStr,
                            onValueChange = { valorUnitarioStr = it },
                            label = { Text("Valor Unit. (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.cardBackground,
                                unfocusedContainerColor = palette.cardBackground,
                                focusedBorderColor = palette.brandLime,
                                unfocusedBorderColor = palette.borderColor
                            ),
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = entradaStr,
                            onValueChange = { entradaStr = it },
                            label = { Text("Entrada (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.cardBackground,
                                unfocusedContainerColor = palette.cardBackground,
                                focusedBorderColor = palette.brandLime,
                                unfocusedBorderColor = palette.borderColor
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = descontoStr,
                            onValueChange = { descontoStr = it },
                            label = { Text("Desconto (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.cardBackground,
                                unfocusedContainerColor = palette.cardBackground,
                                focusedBorderColor = palette.brandLime,
                                unfocusedBorderColor = palette.borderColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Modalidade de Pagamento (À Vista / Parcelado)
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.cardBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "MODALIDADE DE PAGAMENTO",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = !isParcelado,
                                    onClick = { isParcelado = false },
                                    label = { Text("À vista", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.brandLime,
                                        selectedLabelColor = palette.brandLimeText
                                    )
                                )

                                FilterChip(
                                    selected = isParcelado,
                                    onClick = { isParcelado = true },
                                    label = { Text("Parcelado", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.brandLime,
                                        selectedLabelColor = palette.brandLimeText
                                    )
                                )
                            }

                            if (!isParcelado) {
                                DataSelectorField(
                                    label = "Vencimento (opcional)",
                                    data = vencimentoAvista,
                                    onDataSelecionada = { vencimentoAvista = it },
                                    placeholder = "No ato / Sem data",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Nº de parcelas
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Quantidade de parcelas:", fontSize = 13.sp, color = palette.textColor)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            (2..6).forEach { n ->
                                                FilterChip(
                                                    selected = parcelasQtd == n,
                                                    onClick = { parcelasQtd = n },
                                                    label = { Text("${n}x", fontSize = 12.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = palette.brandLime,
                                                        selectedLabelColor = palette.brandLimeText
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Frequência
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Frequência:", fontSize = 13.sp, color = palette.textColor)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FrequenciaPlano.values().forEach { freq ->
                                                FilterChip(
                                                    selected = frequenciaPlano == freq,
                                                    onClick = { frequenciaPlano = freq },
                                                    label = { Text(freq.label, fontSize = 11.5.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = palette.brandLime,
                                                        selectedLabelColor = palette.brandLimeText
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // 1º Vencimento
                                    DataSelectorField(
                                        label = "1º Vencimento",
                                        data = primeiroVencimento,
                                        onDataSelecionada = { primeiroVencimento = it ?: LocalDate.now().plusMonths(1) },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Estimativa do valor de cada parcela
                                    val saldoParcelar = max(0.0, valorTotalCalculado - entNum)
                                    val valorPorParcela = saldoParcelar / parcelasQtd
                                    Text(
                                        text = "Estimativa: ${parcelasQtd}x de ${String.format(Locale("pt", "BR"), "R$ %,.2f", valorPorParcela)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.brandTeal
                                    )
                                }
                            }
                        }
                    }
                }

                // Fotos de Referência (até 6)
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.cardBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "FOTOS DE REFERÊNCIA (${fotos.size}/6)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textSecondaryColor,
                                    letterSpacing = 0.5.sp
                                )

                                if (fotos.size < 6) {
                                    Button(
                                        onClick = {
                                            fotoLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.brandTeal,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Adicionar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (fotos.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(fotos) { caminho ->
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(File(caminho)),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(3.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .clickable { fotos = fotos.filterNot { it == caminho } },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remover",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Observações
                item {
                    OutlinedTextField(
                        value = observacoes,
                        onValueChange = { observacoes = it },
                        label = { Text("Observações (opcional)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.cardBackground,
                            unfocusedContainerColor = palette.cardBackground,
                            focusedBorderColor = palette.brandLime,
                            unfocusedBorderColor = palette.borderColor
                        ),
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Rodapé fixo com Valor Total e Botão Salvar
            Surface(
                color = palette.surfaceBackground,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "VALOR TOTAL",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondaryColor
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", valorTotalCalculado),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textColor
                        )
                    }

                    Button(
                        onClick = {
                            if (cliente.isBlank() || servico.isBlank()) return@Button

                            val planoFinal = if (isParcelado) {
                                Plano(
                                    parcelas = parcelasQtd,
                                    primeiroVencimento = primeiroVencimento,
                                    frequencia = frequenciaPlano
                                )
                            } else null

                            val ordemSalvar = Ordem(
                                id = ordemExistente?.id ?: 0L,
                                num = ordemExistente?.num ?: 0,
                                data = dataPedido,
                                cliente = cliente.trim(),
                                telefone = telefone.trim(),
                                servico = servico.trim(),
                                quantidade = qtdNum,
                                valorUnitario = vuNum,
                                entrada = entNum,
                                desconto = descNum,
                                observacoes = observacoes.trim(),
                                status = ordemExistente?.status ?: StatusOrdem.ABERTA,
                                entregaPrevista = entregaPrevista,
                                entregaReal = ordemExistente?.entregaReal,
                                vencimento = if (isParcelado) null else vencimentoAvista,
                                plano = planoFinal,
                                pagamentos = ordemExistente?.pagamentos ?: emptyList(),
                                fotos = fotos
                            )
                            onSalvar(ordemSalvar)
                        },
                        enabled = cliente.isNotBlank() && servico.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.brandLime,
                            contentColor = palette.brandLimeText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(start = 16.dp)
                    ) {
                        Text("Salvar Ordem", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DataSelectorField(
    label: String,
    data: LocalDate?,
    onDataSelecionada: (LocalDate?) -> Unit,
    placeholder: String = "Selecionar",
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.cardBackground,
        modifier = modifier
            .border(1.dp, palette.borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                val ref = data ?: LocalDate.now()
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDataSelecionada(LocalDate.of(year, month + 1, day))
                    },
                    ref.year,
                    ref.monthValue - 1,
                    ref.dayOfMonth
                ).show()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Column {
                Text(text = label, fontSize = 11.sp, color = palette.textSecondaryColor)
                Text(
                    text = data?.format(dateFormatter) ?: placeholder,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (data != null) palette.textColor else palette.textSecondaryColor
                )
            }
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = palette.brandTeal,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
