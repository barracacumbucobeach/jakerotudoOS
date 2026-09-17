package com.example.data.model

import java.time.LocalDate

enum class FrequenciaLembrete(val dias: Int, val label: String) {
    DIARIO(1, "Todos os dias"),
    DOIS_DIAS(2, "A cada 2 dias"),
    TRES_DIAS(3, "A cada 3 dias"),
    SEMANAL(7, "Uma vez por semana"),
    DESLIGADO(0, "Não lembrar")
}

enum class TemaApp(val label: String) {
    CLARO("Claro"),
    ESCURO("Escuro"),
    JAKERO("Jakero Tudo")
}

data class Configuracoes(
    val frequenciaLembrete: FrequenciaLembrete = FrequenciaLembrete.TRES_DIAS,
    val antecedenciaVencimento: Int = 1, // 0, 1, 3, 7 dias
    val ultimoLembrete: LocalDate? = null,
    val tema: TemaApp = TemaApp.CLARO,
    val supabaseUrl: String = "https://muxvtacywjfgclfvtgia.supabase.co",
    val supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im11eHZ0YWN5d2pmZ2NsZnZ0Z2lhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkwMzk2NTcsImV4cCI6MjEwNDYxNTY1N30.PWBDf6SlY7vPv5bz0irTsdGVypXOdoJQYE7554_0ye8",
    val ultimoSync: Long = 0L
)
