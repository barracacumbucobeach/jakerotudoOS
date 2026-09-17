package com.example.data.model

import java.time.LocalDate

enum class TipoAviso {
    ATRASO_PAGAMENTO,   // Vermelho
    ENTREGA_ATRASADA,   // Azul
    A_VENCER,           // Azul
    SERVICO_A_CONCLUIR  // Neutro
}

data class AvisoItem(
    val id: String,
    val ordemId: Long,
    val ordemNum: Int,
    val cliente: String,
    val tipo: TipoAviso,
    val titulo: String,
    val descricao: String,
    val valorPendente: Double? = null,
    val dataReferencia: LocalDate? = null,
    val diasDiferenca: Long = 0
) {
    val isUrgente: Boolean
        get() = tipo == TipoAviso.ATRASO_PAGAMENTO || tipo == TipoAviso.ENTREGA_ATRASADA
}
