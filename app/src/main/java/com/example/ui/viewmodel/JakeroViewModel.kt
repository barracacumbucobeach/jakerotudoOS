package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.JakeroApplication
import com.example.data.local.PreferencesManager
import com.example.data.model.AvisoItem
import com.example.data.model.Configuracoes
import com.example.data.model.FrequenciaLembrete
import com.example.data.model.Ordem
import com.example.data.model.StatusOrdem
import com.example.data.model.TemaApp
import com.example.data.repository.OrdemRepository
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncState
import com.example.service.ExportHelper
import com.example.service.LembreteWorker
import com.example.service.PdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppTab(val label: String) {
    INICIO("Início"),
    ORDENS("Ordens"),
    CLIENTES("Clientes"),
    FINANCEIRO("Financeiro")
}

enum class FiltroOrdens(val label: String) {
    TODAS("Todas"),
    ABERTAS("Abertas"),
    ANDAMENTO("Em andamento"),
    ENTREGUES("Entregues"),
    A_RECEBER("A receber")
}

class JakeroViewModel(
    private val repository: OrdemRepository,
    private val preferencesManager: PreferencesManager,
    private val syncManager: SyncManager
) : ViewModel() {

    val ordens: StateFlow<List<Ordem>> = repository.allActiveOrdens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val configuracoes: StateFlow<Configuracoes> = preferencesManager.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Configuracoes())

    val syncState: StateFlow<SyncState> = syncManager.syncState

    private val _selectedTab = MutableStateFlow(AppTab.INICIO)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _ordemDetalheId = MutableStateFlow<Long?>(null)
    val ordemDetalheId: StateFlow<Long?> = _ordemDetalheId.asStateFlow()

    private val _ordemParaEditar = MutableStateFlow<Ordem?>(null)
    val ordemParaEditar: StateFlow<Ordem?> = _ordemParaEditar.asStateFlow()
    private val _isCriandoNovaOrdem = MutableStateFlow(false)
    val isCriandoNovaOrdem: StateFlow<Boolean> = _isCriandoNovaOrdem.asStateFlow()

    private val _ordemParaPagamento = MutableStateFlow<Ordem?>(null)
    val ordemParaPagamento: StateFlow<Ordem?> = _ordemParaPagamento.asStateFlow()

    private val _ordemParaWhatsApp = MutableStateFlow<Ordem?>(null)
    val ordemParaWhatsApp: StateFlow<Ordem?> = _ordemParaWhatsApp.asStateFlow()

    private val _mostrarAvisosDialog = MutableStateFlow(false)
    val mostrarAvisosDialog: StateFlow<Boolean> = _mostrarAvisosDialog.asStateFlow()

    private val _mostrarConfiguracoesDialog = MutableStateFlow(false)
    val mostrarConfiguracoesDialog: StateFlow<Boolean> = _mostrarConfiguracoesDialog.asStateFlow()

    private val _mostrarRelatorioDialog = MutableStateFlow(false)
    val mostrarRelatorioDialog: StateFlow<Boolean> = _mostrarRelatorioDialog.asStateFlow()

    private val _filtroOrdens = MutableStateFlow(FiltroOrdens.TODAS)
    val filtroOrdens: StateFlow<FiltroOrdens> = _filtroOrdens.asStateFlow()

    private val _termoBusca = MutableStateFlow("")
    val termoBusca: StateFlow<String> = _termoBusca.asStateFlow()

    private val _filtroCliente = MutableStateFlow<String?>(null)
    val filtroCliente: StateFlow<String?> = _filtroCliente.asStateFlow()

    // Estatísticas de Hoje
    val caixaHoje: StateFlow<Double> = ordens.combine(MutableStateFlow(LocalDate.now())) { list, hoje ->
        var total = 0.0
        for (o in list) {
            if (o.data == hoje && o.entrada > 0) {
                total += o.entrada
            }
            for (p in o.pagamentos) {
                if (p.data == hoje) {
                    total += p.valor
                }
            }
        }
        total
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalHoje: StateFlow<Double> = ordens.combine(MutableStateFlow(LocalDate.now())) { list, hoje ->
        list.filter { it.data == hoje }.sumOf { it.valor }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Contadores Início: A Fazer (Abertas), Em Andamento, A Receber (falta > 0)
    val contadores: StateFlow<Triple<Int, Int, Int>> = ordens.combine(MutableStateFlow(Unit)) { list, _ ->
        val aFazer = list.count { it.status == StatusOrdem.ABERTA }
        val andamento = list.count { it.status == StatusOrdem.ANDAMENTO }
        val aReceber = list.count { !it.quitada }
        Triple(aFazer, andamento, aReceber)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0, 0, 0))

    val avisos: StateFlow<List<AvisoItem>> = combine(ordens, configuracoes) { list, config ->
        LembreteWorker.calcularAvisos(list, config, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val temAvisoUrgente: StateFlow<Boolean> = avisos.combine(MutableStateFlow(Unit)) { list, _ ->
        list.any { it.isUrgente }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Navegação
    fun selecionarAba(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun navegarParaOrdensComFiltro(filtro: FiltroOrdens) {
        _filtroCliente.value = null
        _filtroOrdens.value = filtro
        _selectedTab.value = AppTab.ORDENS
    }

    fun navegarParaOrdensCliente(cliente: String) {
        _filtroCliente.value = cliente
        _filtroOrdens.value = FiltroOrdens.TODAS
        _selectedTab.value = AppTab.ORDENS
    }

    fun setFiltroOrdens(filtro: FiltroOrdens) {
        _filtroOrdens.value = filtro
    }

    fun setTermoBusca(termo: String) {
        _termoBusca.value = termo
    }

    fun limparFiltroCliente() {
        _filtroCliente.value = null
    }

    // Detalhe OS
    fun abrirDetalhe(id: Long) {
        _ordemDetalheId.value = id
    }

    fun fecharDetalhe() {
        _ordemDetalheId.value = null
    }

    // Sheet Nova/Editar OS
    fun abrirNovaOrdem() {
        _ordemParaEditar.value = null
        _isCriandoNovaOrdem.value = true
    }

    fun abrirEditarOrdem(ordem: Ordem) {
        _ordemParaEditar.value = ordem
        _isCriandoNovaOrdem.value = true
    }

    fun fecharSheetOrdem() {
        _ordemParaEditar.value = null
        _isCriandoNovaOrdem.value = false
    }

    fun salvarOrdem(ordem: Ordem) {
        viewModelScope.launch {
            repository.salvarOrdem(ordem)
            fecharSheetOrdem()
        }
    }

    fun avancarStatus(ordem: Ordem) {
        viewModelScope.launch {
            repository.avancarStatus(ordem)
        }
    }

    // Registrar Pagamento
    fun abrirRegistrarPagamento(ordem: Ordem) {
        _ordemParaPagamento.value = ordem
    }

    fun fecharRegistrarPagamento() {
        _ordemParaPagamento.value = null
    }

    fun confirmarPagamento(ordem: Ordem, valor: Double, data: LocalDate) {
        viewModelScope.launch {
            repository.adicionarPagamento(ordem, valor, data)
            fecharRegistrarPagamento()
        }
    }

    fun desfazerPagamento(ordem: Ordem, pagamentoId: String) {
        viewModelScope.launch {
            repository.removerPagamento(ordem, pagamentoId)
        }
    }

    // Fotos
    fun anexarFoto(ordemId: Long, caminho: String) {
        viewModelScope.launch {
            repository.adicionarFoto(ordemId, caminho)
        }
    }

    fun removerFoto(ordemId: Long, caminho: String) {
        viewModelScope.launch {
            repository.removerFoto(ordemId, caminho)
        }
    }

    fun excluirOrdem(ordemId: Long) {
        viewModelScope.launch {
            repository.excluirOrdem(ordemId)
            if (_ordemDetalheId.value == ordemId) {
                _ordemDetalheId.value = null
            }
        }
    }

    // Diálogos
    fun abrirAvisos() { _mostrarAvisosDialog.value = true }
    fun fecharAvisos() { _mostrarAvisosDialog.value = false }

    fun abrirConfiguracoes() { _mostrarConfiguracoesDialog.value = true }
    fun fecharConfiguracoes() { _mostrarConfiguracoesDialog.value = false }

    fun abrirRelatorio() { _mostrarRelatorioDialog.value = true }
    fun fecharRelatorio() { _mostrarRelatorioDialog.value = false }

    fun abrirWhatsAppModal(ordem: Ordem) { _ordemParaWhatsApp.value = ordem }
    fun fecharWhatsAppModal() { _ordemParaWhatsApp.value = null }

    // Configurações
    fun setTema(tema: TemaApp) {
        viewModelScope.launch { preferencesManager.setTema(tema) }
    }

    fun setFrequenciaLembrete(freq: FrequenciaLembrete) {
        viewModelScope.launch { preferencesManager.setFrequenciaLembrete(freq) }
    }

    fun setAntecedenciaVencimento(dias: Int) {
        viewModelScope.launch { preferencesManager.setAntecedenciaVencimento(dias) }
    }

    fun setSupabaseConfig(url: String, key: String) {
        viewModelScope.launch {
            preferencesManager.setSupabaseConfig(url, key)
            syncManager.sincronizarTudo()
        }
    }

    fun reiniciarLembretes() {
        viewModelScope.launch {
            preferencesManager.setUltimoLembrete(LocalDate.now())
            fecharAvisos()
        }
    }

    fun forcarSync() {
        syncManager.forcarSincronizacao()
    }

    // PDF e Compartilhamento
    fun gerarECompartilharPdfOrdem(context: Context, ordem: Ordem) {
        viewModelScope.launch {
            val file = PdfGenerator.gerarPdfOrdem(context, ordem)
            ExportHelper.compartilharArquivo(
                context,
                file,
                "application/pdf",
                "Ordem de Serviço Nº ${ordem.numeroFormatado} - Jakero Tudo"
            )
        }
    }

    fun exportarRelatorioPdf(context: Context, tituloPeriodo: String, ordens: List<Ordem>, caixa: Double, total: Double) {
        viewModelScope.launch {
            val file = PdfGenerator.gerarPdfRelatorio(context, tituloPeriodo, ordens, caixa, total)
            ExportHelper.compartilharArquivo(
                context,
                file,
                "application/pdf",
                "Relatório $tituloPeriodo - Jakero Tudo"
            )
        }
    }

    fun exportarRelatorioCsv(context: Context, tituloPeriodo: String, ordens: List<Ordem>, caixa: Double, total: Double) {
        viewModelScope.launch {
            val file = ExportHelper.gerarCsvRelatorio(context, tituloPeriodo, ordens, caixa, total)
            ExportHelper.compartilharArquivo(
                context,
                file,
                "text/csv",
                "Relatório $tituloPeriodo - Jakero Tudo"
            )
        }
    }

    fun exportarBackupJson(context: Context) {
        viewModelScope.launch {
            val json = repository.exportarBackupJson()
            val file = java.io.File(context.cacheDir, "backup_jakero_tudo_${System.currentTimeMillis()}.json")
            file.writeText(json)
            ExportHelper.compartilharArquivo(
                context,
                file,
                "application/json",
                "Backup Jakero Tudo (JSON)"
            )
        }
    }

    fun importarBackupJson(json: String, onConcluido: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val importados = repository.importarBackupJson(json)
                onConcluido(importados)
            } catch (_: Exception) {
                onConcluido(0)
            }
        }
    }
}

class JakeroViewModelFactory(
    private val app: JakeroApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return JakeroViewModel(app.repository, app.preferencesManager, app.syncManager) as T
    }
}
