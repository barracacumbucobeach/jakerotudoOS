package com.example.data.model

import java.time.LocalDate
import kotlin.math.roundToLong

enum class FrequenciaPlano {
    MENSAL,
    QUINZENAL,
    SEMANAL;

    val label: String
        get() = when (this) {
            MENSAL -> "Mensal"
            QUINZENAL -> "Quinzenal"
            SEMANAL -> "Semanal"
        }

    fun proximaData(dataBase: LocalDate, indice: Long): LocalDate {
        return when (this) {
            MENSAL -> dataBase.plusMonths(indice)
            QUINZENAL -> dataBase.plusDays(15 * indice)
            SEMANAL -> dataBase.plusWeeks(indice)
        }
    }
}

data class Plano(
    val parcelas: Int,
    val primeiroVencimento: LocalDate,
    val frequencia: FrequenciaPlano
) {
    fun calcularParcelas(valorTotal: Double, totalPago: Double, hoje: LocalDate = LocalDate.now()): List<ParcelaInfo> {
        if (parcelas <= 0 || valorTotal <= 0.0) return emptyList()

        val totalCentavos = (valorTotal * 100).roundToLong()
        val valorBaseCentavos = totalCentavos / parcelas
        val sobraCentavos = totalCentavos % parcelas

        var saldoDisponivelCentavos = (totalPago * 100).roundToLong()

        val lista = mutableListOf<ParcelaInfo>()
        for (i in 0 until parcelas) {
            val parcelaCentavos = valorBaseCentavos + (if (i == parcelas - 1) sobraCentavos else 0L)
            val valorParcela = parcelaCentavos / 100.0
            val vencimento = frequencia.proximaData(primeiroVencimento, i.toLong())

            val pagoNestaParcelaCentavos = when {
                saldoDisponivelCentavos >= parcelaCentavos -> {
                    saldoDisponivelCentavos -= parcelaCentavos
                    parcelaCentavos
                }
                saldoDisponivelCentavos > 0 -> {
                    val pago = saldoDisponivelCentavos
                    saldoDisponivelCentavos = 0
                    pago
                }
                else -> 0L
            }

            val valorPagoParcela = pagoNestaParcelaCentavos / 100.0
            val status = when {
                pagoNestaParcelaCentavos >= parcelaCentavos -> ParcelaStatus.PAGA
                pagoNestaParcelaCentavos > 0 -> ParcelaStatus.PARCIAL
                hoje.isAfter(vencimento) -> ParcelaStatus.ATRASADA
                else -> ParcelaStatus.EM_ABERTO
            }

            lista.add(
                ParcelaInfo(
                    numero = i + 1,
                    totalParcelas = parcelas,
                    vencimento = vencimento,
                    valor = valorParcela,
                    valorPago = valorPagoParcela,
                    status = status
                )
            )
        }
        return lista
    }
}

enum class ParcelaStatus {
    PAGA,
    PARCIAL,
    EM_ABERTO,
    ATRASADA;

    val label: String
        get() = when (this) {
            PAGA -> "Paga"
            PARCIAL -> "Parcial"
            EM_ABERTO -> "Em aberto"
            ATRASADA -> "Atrasada"
        }
}

data class ParcelaInfo(
    val numero: Int,
    val totalParcelas: Int,
    val vencimento: LocalDate,
    val valor: Double,
    val valorPago: Double,
    val status: ParcelaStatus
)
