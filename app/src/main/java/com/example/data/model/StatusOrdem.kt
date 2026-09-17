package com.example.data.model

enum class StatusOrdem {
    ABERTA,
    ANDAMENTO,
    ENTREGUE;

    val label: String
        get() = when (this) {
            ABERTA -> "Aberta"
            ANDAMENTO -> "Em andamento"
            ENTREGUE -> "Entregue"
        }
}
