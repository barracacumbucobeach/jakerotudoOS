package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import kotlin.math.max

@Entity(tableName = "ordens")
data class Ordem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val num: Int = 0, // 0 = provisório ("OS —")
    val data: LocalDate,
    val cliente: String,
    val telefone: String = "",
    val servico: String,
    val quantidade: Double = 1.0,
    val valorUnitario: Double = 0.0,
    val entrada: Double = 0.0,
    val desconto: Double = 0.0,
    val observacoes: String = "",
    val status: StatusOrdem = StatusOrdem.ABERTA,
    val entregaPrevista: LocalDate? = null,
    val entregaReal: LocalDate? = null,
    val vencimento: LocalDate? = null,
    val plano: Plano? = null,
    val pagamentos: List<Pagamento> = emptyList(),
    val fotos: List<String> = emptyList(),
    val atualizadoEm: Long = System.currentTimeMillis(),
    val removido: Boolean = false,
    val sincronizado: Boolean = false,
    val cloudId: String? = null
) {
    val valor: Double
        get() = max(0.0, (quantidade * valorUnitario) - desconto)

    val totalPago: Double
        get() = entrada + pagamentos.sumOf { it.valor }

    val falta: Double
        get() = max(0.0, valor - totalPago)

    val quitada: Boolean
        get() = falta <= 0.005

    val numeroFormatado: String
        get() = if (num > 0) String.format("%06d", num) else "—"

    val isParcelado: Boolean
        get() = plano != null && plano.parcelas >= 2

    fun parcelasCalculadas(hoje: LocalDate = LocalDate.now()): List<ParcelaInfo> {
        return plano?.calcularParcelas(valor, totalPago, hoje) ?: emptyList()
    }

    /**
     * Retorna a data em que foi quitado (data do último pagamento que quitou, ou data do pedido se quitado na entrada).
     */
    val dataQuitacao: LocalDate?
        get() {
            if (!quitada) return null
            if (pagamentos.isNotEmpty()) {
                return pagamentos.last().data
            }
            return data
        }

    /**
     * Retorna o vencimento relevante (próxima parcela pendente ou vencimento à vista).
     */
    fun proximoVencimento(hoje: LocalDate = LocalDate.now()): LocalDate? {
        if (quitada) return null
        if (isParcelado) {
            val parcelas = parcelasCalculadas(hoje)
            val pendente = parcelas.firstOrNull { it.status == ParcelaStatus.EM_ABERTO || it.status == ParcelaStatus.ATRASADA || it.status == ParcelaStatus.PARCIAL }
            return pendente?.vencimento
        }
        return vencimento
    }
}
