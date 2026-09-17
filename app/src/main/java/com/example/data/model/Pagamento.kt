package com.example.data.model

import java.time.LocalDate
import java.util.UUID

data class Pagamento(
    val id: String = UUID.randomUUID().toString(),
    val data: LocalDate,
    val valor: Double
)
