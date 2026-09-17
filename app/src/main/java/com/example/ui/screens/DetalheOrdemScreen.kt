package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.Ordem
import com.example.data.model.ParcelaStatus
import com.example.service.ImageStorageHelper
import com.example.ui.components.StepProgressBar
import com.example.ui.theme.JakeroTheme
import kotlinx.coroutines.launch
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheOrdemScreen(
    ordem: Ordem,
    onVoltar: () -> Unit,
    onAvancarStatus: () -> Unit,
    onRegistrarPagamento: () -> Unit,
    onDesfazerPagamento: (String) -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    onEnviarWhatsApp: () -> Unit,
    onGerarPdf: () -> Unit,
    onFotoAdicionada: (String) -> Unit,
    onFotoRemovida: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    var mostrarConfirmarExclusao by remember { mutableStateOf(false) }

    // Picker de foto
    val fotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val caminho = ImageStorageHelper.salvarFotoComprimida(context, uri)
                if (caminho != null) {
                    onFotoAdicionada(caminho)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ordem de Serviço Nº ${ordem.numeroFormatado}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onEditar) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { mostrarConfirmarExclusao = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFD32F2F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.surfaceBackground,
                    titleContentColor = palette.textColor
                )
            )
        },
        bottomBar = {
            Surface(
                color = palette.surfaceBackground,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botão Pagamento destacado (se ainda não estiver quitada)
                    if (!ordem.quitada) {
                        Button(
                            onClick = onRegistrarPagamento,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.brandLime,
                                contentColor = palette.brandLimeText
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Registrar Pagamento (Falta ${String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.falta)})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Linha com WhatsApp, PDF e Apagar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Botão WhatsApp
                        Button(
                            onClick = onEnviarWhatsApp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Botão PDF
                        Button(
                            onClick = onGerarPdf,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.brandTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF da OS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Botão Apagar
                        Button(
                            onClick = { mostrarConfirmarExclusao = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEBEE),
                                contentColor = Color(0xFFD32F2F)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apagar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(palette.pageBackground)
        ) {
            // 1. Bloco de Destaque Superior
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.darkBlockBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ORDEM Nº ${ordem.numeroFormatado}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.brandLime,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = ordem.data.format(dateFormatter),
                                fontSize = 12.sp,
                                color = Color(0xFFA49C92)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = ordem.cliente,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF3F0EA)
                        )

                        if (ordem.telefone.isNotBlank()) {
                            Text(
                                text = ordem.telefone,
                                fontSize = 13.sp,
                                color = Color(0xFFA49C92)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "VALOR DO SERVIÇO",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA49C92)
                                )
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.valor),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF3F0EA)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (ordem.quitada) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2E7D32))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "QUITADA",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "FALTA RECEBER",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF8A75)
                                    )
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.falta),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFF8A75)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Trilha de Entrega (Aberta -> Em andamento -> Entregue)
            item {
                StepProgressBar(
                    status = ordem.status,
                    entregaReal = ordem.entregaReal,
                    onAvancarEtapa = onAvancarStatus
                )
            }

            // 3. Detalhes do Serviço & Valores
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "DADOS DO SERVIÇO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondaryColor,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = ordem.servico,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        ItemLinha("Quantidade", String.format(Locale("pt", "BR"), "%.2f", ordem.quantidade))
                        ItemLinha("Valor Unitário", String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.valorUnitario))
                        if (ordem.desconto > 0) {
                            ItemLinha("Desconto", String.format(Locale("pt", "BR"), "- R$ %,.2f", ordem.desconto))
                        }
                        if (ordem.entrada > 0) {
                            ItemLinha("Entrada no Pedido", String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.entrada))
                        }
                        ItemLinha("Total Pago até agora", String.format(Locale("pt", "BR"), "R$ %,.2f", ordem.totalPago))
                        ItemLinha("Previsão de Entrega", ordem.entregaPrevista?.format(dateFormatter) ?: "Não informada")
                        if (ordem.entregaReal != null) {
                            ItemLinha("Entrega Realizada", ordem.entregaReal.format(dateFormatter))
                        }
                    }
                }
            }

            // 4. Parcelamento (se houver)
            if (ordem.isParcelado) {
                item {
                    val parcelas = ordem.parcelasCalculadas()
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.cardBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "PARCELAMENTO (${ordem.plano?.parcelas}x ${ordem.plano?.frequencia?.label})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )

                            parcelas.forEach { p ->
                                val (statusColor, statusBg) = when (p.status) {
                                    ParcelaStatus.PAGA -> Color(0xFF2E7D32) to Color(0x224CAF50)
                                    ParcelaStatus.ATRASADA -> Color(0xFFD32F2F) to Color(0x22D32F2F)
                                    ParcelaStatus.PARCIAL -> Color(0xFFE65100) to Color(0x22E65100)
                                    ParcelaStatus.EM_ABERTO -> palette.textSecondaryColor to palette.borderColor
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "Parcela ${p.numero}/${p.totalParcelas} · ${String.format(Locale("pt", "BR"), "R$ %,.2f", p.valor)}",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textColor
                                        )
                                        Text(
                                            text = "Vencimento: ${p.vencimento.format(dateFormatter)}",
                                            fontSize = 11.5.sp,
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
                                            text = if (p.status == ParcelaStatus.PARCIAL) "Parcial" else p.status.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Histórico de Pagamentos
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "HISTÓRICO FINANCEIRO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondaryColor,
                            letterSpacing = 0.5.sp
                        )

                        if (ordem.entrada > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Entrada em ${ordem.data.format(dateFormatter)}",
                                    fontSize = 13.sp,
                                    color = palette.textColor
                                )
                                Text(
                                    text = String.format(Locale("pt", "BR"), "+ R$ %,.2f", ordem.entrada),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        if (ordem.pagamentos.isEmpty() && ordem.entrada <= 0) {
                            Text(
                                text = "Nenhum pagamento registrado ainda.",
                                fontSize = 12.5.sp,
                                color = palette.textSecondaryColor
                            )
                        } else {
                            ordem.pagamentos.forEach { pag ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "Pago em ${pag.data.format(dateFormatter)}",
                                            fontSize = 13.sp,
                                            color = palette.textColor
                                        )
                                        Text(
                                            text = String.format(Locale("pt", "BR"), "+ R$ %,.2f", pag.valor),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }

                                    // Botão desfazer pagamento
                                    IconButton(
                                        onClick = { onDesfazerPagamento(pag.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Desfazer pagamento",
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Fotos da Ordem (até 6 fotos)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "FOTOS DE REFERÊNCIA (${ordem.fotos.size}/6)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )

                            if (ordem.fotos.size < 6) {
                                TextButton(
                                    onClick = {
                                        fotoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Anexar", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (ordem.fotos.isEmpty()) {
                            Text(
                                text = "Nenhuma foto anexada. Toque em Anexar para adicionar até 6 fotos.",
                                fontSize = 12.5.sp,
                                color = palette.textSecondaryColor
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(ordem.fotos) { caminho ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, palette.borderColor, RoundedCornerShape(8.dp))
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(File(caminho)),
                                            contentDescription = "Foto da OS",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .clickable { onFotoRemovida(caminho) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Observações
            if (ordem.observacoes.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.cardBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "OBSERVAÇÕES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = ordem.observacoes,
                                fontSize = 13.5.sp,
                                color = palette.textColor
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo Confirmar Exclusão
    if (mostrarConfirmarExclusao) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarExclusao = false },
            title = { Text("Excluir Ordem de Serviço") },
            text = { Text("Tem certeza que deseja excluir a OS Nº ${ordem.numeroFormatado} do cliente ${ordem.cliente}?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmarExclusao = false
                        onExcluir()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarConfirmarExclusao = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ItemLinha(titulo: String, valor: String) {
    val palette = JakeroTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = titulo, fontSize = 13.sp, color = palette.textSecondaryColor)
        Text(text = valor, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = palette.textColor)
    }
}
