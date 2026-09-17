package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.AvisoItem
import com.example.data.model.Configuracoes
import com.example.data.model.FrequenciaLembrete
import com.example.data.model.Ordem
import com.example.data.model.ParcelaStatus
import com.example.data.model.StatusOrdem
import com.example.data.model.TipoAviso
import kotlinx.coroutines.flow.firstOrNull
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

class LembreteWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val prefs = PreferencesManager(applicationContext)
        val config = prefs.configFlow.firstOrNull() ?: Configuracoes()

        if (config.frequenciaLembrete == FrequenciaLembrete.DESLIGADO) {
            return Result.success()
        }

        val ordens = database.ordemDao().getAllActive().firstOrNull() ?: emptyList()
        val hoje = LocalDate.now()

        val avisos = calcularAvisos(ordens, config, hoje)
        if (avisos.isNotEmpty()) {
            NotificationHelper.enviarNotificacaoLembretes(applicationContext, avisos)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "jakero_lembrete_diario"

        fun agendarVerificacaoDiaria(context: Context) {
            val agora = LocalDateTime.now()
            var proximoAs9 = agora.with(LocalTime.of(9, 0))
            if (agora.isAfter(proximoAs9)) {
                proximoAs9 = proximoAs9.plusDays(1)
            }
            val delayInicial = Duration.between(agora, proximoAs9).toMillis()

            val request = PeriodicWorkRequestBuilder<LembreteWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayInicial, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun calcularAvisos(ordens: List<Ordem>, config: Configuracoes, hoje: LocalDate = LocalDate.now()): List<AvisoItem> {
            val lista = mutableListOf<AvisoItem>()

            for (o in ordens) {
                // 1. Cobrança / Atraso de pagamento
                if (!o.quitada) {
                    if (o.isParcelado) {
                        val parcelas = o.parcelasCalculadas(hoje)
                        for (p in parcelas) {
                            if (p.status == ParcelaStatus.ATRASADA) {
                                val diasAtraso = ChronoUnit.DAYS.between(p.vencimento, hoje)
                                val desc = "${o.cliente} passou o dia de pagar a parcela ${p.numero}/${p.totalParcelas} · vencia ${formatarData(p.vencimento)} ($diasAtraso dias atrás)"
                                lista.add(
                                    AvisoItem(
                                        id = "pag_atraso_${o.id}_${p.numero}",
                                        ordemId = o.id,
                                        ordemNum = o.num,
                                        cliente = o.cliente,
                                        tipo = TipoAviso.ATRASO_PAGAMENTO,
                                        titulo = "Atraso de pagamento",
                                        descricao = desc,
                                        valorPendente = p.valor - p.valorPago,
                                        dataReferencia = p.vencimento,
                                        diasDiferenca = diasAtraso
                                    )
                                )
                            } else if (p.status == ParcelaStatus.EM_ABERTO || p.status == ParcelaStatus.PARCIAL) {
                                val diasAte = ChronoUnit.DAYS.between(hoje, p.vencimento)
                                if (diasAte in 0..config.antecedenciaVencimento && config.antecedenciaVencimento > 0) {
                                    val diasTxt = if (diasAte == 0L) "Vence hoje" else "Vence em $diasAte dia(s)"
                                    lista.add(
                                        AvisoItem(
                                            id = "pag_avencer_${o.id}_${p.numero}",
                                            ordemId = o.id,
                                            ordemNum = o.num,
                                            cliente = o.cliente,
                                            tipo = TipoAviso.A_VENCER,
                                            titulo = "Parcela a vencer",
                                            descricao = "$diasTxt · ${o.cliente} (Parcela ${p.numero}/${p.totalParcelas})",
                                            valorPendente = p.valor - p.valorPago,
                                            dataReferencia = p.vencimento,
                                            diasDiferenca = diasAte
                                        )
                                    )
                                }
                            }
                        }
                    } else if (o.vencimento != null) {
                        // À vista
                        if (hoje.isAfter(o.vencimento)) {
                            val diasAtraso = ChronoUnit.DAYS.between(o.vencimento, hoje)
                            lista.add(
                                AvisoItem(
                                    id = "pag_atraso_avista_${o.id}",
                                    ordemId = o.id,
                                    ordemNum = o.num,
                                    cliente = o.cliente,
                                    tipo = TipoAviso.ATRASO_PAGAMENTO,
                                    titulo = "Atraso de pagamento",
                                    descricao = "${o.cliente} passou o prazo de pagamento · vencia ${formatarData(o.vencimento)} ($diasAtraso dias atrás)",
                                    valorPendente = o.falta,
                                    dataReferencia = o.vencimento,
                                    diasDiferenca = diasAtraso
                                )
                            )
                        } else {
                            val diasAte = ChronoUnit.DAYS.between(hoje, o.vencimento)
                            if (diasAte in 0..config.antecedenciaVencimento && config.antecedenciaVencimento > 0) {
                                val diasTxt = if (diasAte == 0L) "Vence hoje" else "Vence em $diasAte dia(s)"
                                lista.add(
                                    AvisoItem(
                                        id = "pag_avencer_avista_${o.id}",
                                        ordemId = o.id,
                                        ordemNum = o.num,
                                        cliente = o.cliente,
                                        tipo = TipoAviso.A_VENCER,
                                        titulo = "Prazo a vencer",
                                        descricao = "$diasTxt · ${o.cliente} · R$ ${formatarMoeda(o.falta)}",
                                        valorPendente = o.falta,
                                        dataReferencia = o.vencimento,
                                        diasDiferenca = diasAte
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. Entrega atrasada
                if (o.status != StatusOrdem.ENTREGUE && o.entregaPrevista != null) {
                    if (hoje.isAfter(o.entregaPrevista)) {
                        val diasAtraso = ChronoUnit.DAYS.between(o.entregaPrevista, hoje)
                        lista.add(
                            AvisoItem(
                                id = "entrega_atrasada_${o.id}",
                                ordemId = o.id,
                                ordemNum = o.num,
                                cliente = o.cliente,
                                tipo = TipoAviso.ENTREGA_ATRASADA,
                                titulo = "Entrega atrasada",
                                descricao = "Entrega prevista em ${formatarData(o.entregaPrevista)} ($diasAtraso dias atrás) · ${o.servico}",
                                dataReferencia = o.entregaPrevista,
                                diasDiferenca = diasAtraso
                            )
                        )
                    }
                }

                // 3. Serviço a concluir (lembrete periódico)
                if (o.status != StatusOrdem.ENTREGUE && config.frequenciaLembrete != FrequenciaLembrete.DESLIGADO) {
                    val diasDesdeCriacao = ChronoUnit.DAYS.between(o.data, hoje)
                    if (diasDesdeCriacao >= config.frequenciaLembrete.dias) {
                        lista.add(
                            AvisoItem(
                                id = "servico_concluir_${o.id}",
                                ordemId = o.id,
                                ordemNum = o.num,
                                cliente = o.cliente,
                                tipo = TipoAviso.SERVICO_A_CONCLUIR,
                                titulo = "Serviço a concluir",
                                descricao = "${o.cliente} · ${o.servico} (Status: ${o.status.label})",
                                dataReferencia = o.data,
                                diasDiferenca = diasDesdeCriacao
                            )
                        )
                    }
                }
            }

            return lista
        }

        private fun formatarData(data: LocalDate): String {
            return String.format(Locale("pt", "BR"), "%02d/%02d/%04d", data.dayOfMonth, data.monthValue, data.year)
        }

        private fun formatarMoeda(valor: Double): String {
            return String.format(Locale("pt", "BR"), "%.2f", valor)
        }
    }
}
