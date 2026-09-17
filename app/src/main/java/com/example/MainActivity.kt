package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Ordem
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppHeader
import com.example.ui.screens.AvisosDialog
import com.example.ui.screens.ClientesScreen
import com.example.ui.screens.ConfiguracoesDialog
import com.example.ui.screens.DetalheOrdemScreen
import com.example.ui.screens.FinanceiroScreen
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.NovaOrdemSheet
import com.example.ui.screens.OrdensScreen
import com.example.ui.screens.RegistrarPagamentoSheet
import com.example.ui.screens.RelatorioDialog
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WhatsAppShareDialog
import com.example.ui.theme.JakeroTudoTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.JakeroViewModel
import com.example.ui.viewmodel.JakeroViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: JakeroViewModel by viewModels {
        JakeroViewModelFactory(application as JakeroApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val config by viewModel.configuracoes.collectAsState()

            JakeroTudoTheme(tema = config.tema) {
                JakeroApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun JakeroApp(viewModel: JakeroViewModel) {
    val context = LocalContext.current
    var mostrarSplash by remember { mutableStateOf(true) }

    // Solicitar permissão de notificação no Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (mostrarSplash) {
        SplashScreen(onFinished = { mostrarSplash = false })
        return
    }

    // Estados observados do ViewModel
    val ordens by viewModel.ordens.collectAsState()
    val config by viewModel.configuracoes.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val ordemDetalheId by viewModel.ordemDetalheId.collectAsState()

    val isCriandoNovaOrdem by viewModel.isCriandoNovaOrdem.collectAsState()
    val ordemParaEditar by viewModel.ordemParaEditar.collectAsState()
    val ordemParaPagamento by viewModel.ordemParaPagamento.collectAsState()
    val ordemParaWhatsApp by viewModel.ordemParaWhatsApp.collectAsState()

    val mostrarAvisosDialog by viewModel.mostrarAvisosDialog.collectAsState()
    val mostrarConfiguracoesDialog by viewModel.mostrarConfiguracoesDialog.collectAsState()
    val mostrarRelatorioDialog by viewModel.mostrarRelatorioDialog.collectAsState()

    val filtroOrdens by viewModel.filtroOrdens.collectAsState()
    val termoBusca by viewModel.termoBusca.collectAsState()
    val filtroCliente by viewModel.filtroCliente.collectAsState()

    val caixaHoje by viewModel.caixaHoje.collectAsState()
    val totalHoje by viewModel.totalHoje.collectAsState()
    val contadores by viewModel.contadores.collectAsState()
    val avisos by viewModel.avisos.collectAsState()
    val temAvisoUrgente by viewModel.temAvisoUrgente.collectAsState()

    // Estado para exportação de relatório
    var relatorioTitulo by remember { mutableStateOf("") }
    var relatorioOrdens by remember { mutableStateOf<List<Ordem>>(emptyList()) }
    var relatorioCaixa by remember { mutableDoubleStateOf(0.0) }
    var relatorioTotal by remember { mutableDoubleStateOf(0.0) }

    // Tela de Detalhe da OS
    val ordemDetalhe = ordens.firstOrNull { it.id == ordemDetalheId }
    if (ordemDetalhe != null) {
        DetalheOrdemScreen(
            ordem = ordemDetalhe,
            onVoltar = { viewModel.fecharDetalhe() },
            onAvancarStatus = { viewModel.avancarStatus(ordemDetalhe) },
            onRegistrarPagamento = { viewModel.abrirRegistrarPagamento(ordemDetalhe) },
            onDesfazerPagamento = { pagId -> viewModel.desfazerPagamento(ordemDetalhe, pagId) },
            onEditar = { viewModel.abrirEditarOrdem(ordemDetalhe) },
            onExcluir = { viewModel.excluirOrdem(ordemDetalhe.id) },
            onEnviarWhatsApp = { viewModel.abrirWhatsAppModal(ordemDetalhe) },
            onGerarPdf = { viewModel.gerarECompartilharPdfOrdem(context, ordemDetalhe) },
            onFotoAdicionada = { caminho -> viewModel.anexarFoto(ordemDetalhe.id, caminho) },
            onFotoRemovida = { caminho -> viewModel.removerFoto(ordemDetalhe.id, caminho) }
        )
    } else {
        Scaffold(
            topBar = {
                AppHeader(
                    tituloTela = selectedTab.label,
                    syncState = syncState,
                    numeroAvisos = avisos.size,
                    temAvisoUrgente = temAvisoUrgente,
                    onSyncClick = { viewModel.forcarSync() },
                    onAvisosClick = { viewModel.abrirAvisos() },
                    onConfigClick = { viewModel.abrirConfiguracoes() }
                )
            },
            bottomBar = {
                AppBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selecionarAba(it) },
                    onNovaOrdemClick = { viewModel.abrirNovaOrdem() }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { targetTab ->
                when (targetTab) {
                    AppTab.INICIO -> {
                        InicioScreen(
                            caixaHoje = caixaHoje,
                            totalHoje = totalHoje,
                            contadores = contadores,
                            ultimasOrdens = ordens,
                            avisos = avisos,
                            temAvisoUrgente = temAvisoUrgente,
                            onAvisosClick = { viewModel.abrirAvisos() },
                            onContadorClick = { filtro -> viewModel.navegarParaOrdensComFiltro(filtro) },
                            onVerTodasOrdens = { viewModel.navegarParaOrdensComFiltro(com.example.ui.viewmodel.FiltroOrdens.TODAS) },
                            onOrdemClick = { id -> viewModel.abrirDetalhe(id) }
                        )
                    }
                    AppTab.ORDENS -> {
                        OrdensScreen(
                            ordens = ordens,
                            filtroAtual = filtroOrdens,
                            termoBusca = termoBusca,
                            clienteFiltro = filtroCliente,
                            onFiltroChange = { viewModel.setFiltroOrdens(it) },
                            onBuscaChange = { viewModel.setTermoBusca(it) },
                            onLimparClienteFiltro = { viewModel.limparFiltroCliente() },
                            onOrdemClick = { id -> viewModel.abrirDetalhe(id) }
                        )
                    }
                    AppTab.CLIENTES -> {
                        ClientesScreen(
                            ordens = ordens,
                            onClienteClick = { cliente -> viewModel.navegarParaOrdensCliente(cliente) }
                        )
                    }
                    AppTab.FINANCEIRO -> {
                        FinanceiroScreen(
                            ordens = ordens,
                            onOrdemClick = { id -> viewModel.abrirDetalhe(id) },
                            onExportarRelatorio = { titulo, lista, caixa, total ->
                                relatorioTitulo = titulo
                                relatorioOrdens = lista
                                relatorioCaixa = caixa
                                relatorioTotal = total
                                viewModel.abrirRelatorio()
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Nova / Editar Ordem
    if (isCriandoNovaOrdem) {
        NovaOrdemSheet(
            ordemExistente = ordemParaEditar,
            onDismiss = { viewModel.fecharSheetOrdem() },
            onSalvar = { ordem -> viewModel.salvarOrdem(ordem) }
        )
    }

    // Modal Registrar Pagamento
    if (ordemParaPagamento != null) {
        RegistrarPagamentoSheet(
            ordem = ordemParaPagamento!!,
            onDismiss = { viewModel.fecharRegistrarPagamento() },
            onConfirmar = { valor, data ->
                viewModel.confirmarPagamento(ordemParaPagamento!!, valor, data)
            }
        )
    }

    // Modal Enviar WhatsApp
    if (ordemParaWhatsApp != null) {
        WhatsAppShareDialog(
            ordem = ordemParaWhatsApp!!,
            onDismiss = { viewModel.fecharWhatsAppModal() }
        )
    }

    // Diálogo de Avisos
    if (mostrarAvisosDialog) {
        AvisosDialog(
            avisos = avisos,
            frequenciaDias = config.frequenciaLembrete.dias,
            onDismiss = { viewModel.fecharAvisos() },
            onOrdemClick = { id -> viewModel.abrirDetalhe(id) },
            onReiniciarLembretes = { viewModel.reiniciarLembretes() }
        )
    }

    // Diálogo de Configurações
    if (mostrarConfiguracoesDialog) {
        ConfiguracoesDialog(
            config = config,
            onDismiss = { viewModel.fecharConfiguracoes() },
            onTemaChange = { viewModel.setTema(it) },
            onFrequenciaChange = { viewModel.setFrequenciaLembrete(it) },
            onAntecedenciaChange = { viewModel.setAntecedenciaVencimento(it) },
            onSupabaseSave = { url, key -> viewModel.setSupabaseConfig(url, key) },
            onExportarBackup = { viewModel.exportarBackupJson(context) },
            onImportarBackup = { json -> viewModel.importarBackupJson(json) { _ -> } }
        )
    }

    // Diálogo de Relatório Financeiro
    if (mostrarRelatorioDialog) {
        RelatorioDialog(
            tituloPeriodo = relatorioTitulo,
            ordens = relatorioOrdens,
            caixaRecebido = relatorioCaixa,
            valorTotal = relatorioTotal,
            onDismiss = { viewModel.fecharRelatorio() },
            onExportarPdf = {
                viewModel.exportarRelatorioPdf(context, relatorioTitulo, relatorioOrdens, relatorioCaixa, relatorioTotal)
            },
            onExportarCsv = {
                viewModel.exportarRelatorioCsv(context, relatorioTitulo, relatorioOrdens, relatorioCaixa, relatorioTotal)
            }
        )
    }
}
