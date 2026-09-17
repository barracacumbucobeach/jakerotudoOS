package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.Configuracoes
import com.example.data.model.FrequenciaLembrete
import com.example.data.model.TemaApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jakero_config")

class PreferencesManager(private val context: Context) {

    companion object {
        const val DEFAULT_SUPABASE_URL = "https://muxvtacywjfgclfvtgia.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im11eHZ0YWN5d2pmZ2NsZnZ0Z2lhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkwMzk2NTcsImV4cCI6MjEwNDYxNTY1N30.PWBDf6SlY7vPv5bz0irTsdGVypXOdoJQYE7554_0ye8"
    }

    private val KEY_FREQUENCIA_LEMBRETE = stringPreferencesKey("frequencia_lembrete")
    private val KEY_ANTECEDENCIA_VENCIMENTO = intPreferencesKey("antecedencia_vencimento")
    private val KEY_ULTIMO_LEMBRETE = stringPreferencesKey("ultimo_lembrete")
    private val KEY_TEMA = stringPreferencesKey("tema")
    private val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
    private val KEY_SUPABASE_ANON_KEY = stringPreferencesKey("supabase_anon_key")
    private val KEY_ULTIMO_SYNC = longPreferencesKey("ultimo_sync")

    val configFlow: Flow<Configuracoes> = context.dataStore.data.map { prefs ->
        val freqStr = prefs[KEY_FREQUENCIA_LEMBRETE] ?: FrequenciaLembrete.TRES_DIAS.name
        val freq = try {
            FrequenciaLembrete.valueOf(freqStr)
        } catch (_: Exception) {
            FrequenciaLembrete.TRES_DIAS
        }

        val antecedencia = prefs[KEY_ANTECEDENCIA_VENCIMENTO] ?: 1

        val ultimoLembreteStr = prefs[KEY_ULTIMO_LEMBRETE]
        val ultimoLembrete = ultimoLembreteStr?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }

        val temaStr = prefs[KEY_TEMA] ?: TemaApp.CLARO.name
        val tema = try {
            TemaApp.valueOf(temaStr)
        } catch (_: Exception) {
            TemaApp.CLARO
        }

        val supabaseUrl = prefs[KEY_SUPABASE_URL]?.takeIf { it.isNotBlank() } ?: DEFAULT_SUPABASE_URL
        val supabaseAnonKey = prefs[KEY_SUPABASE_ANON_KEY]?.takeIf { it.isNotBlank() } ?: DEFAULT_SUPABASE_ANON_KEY
        val ultimoSync = prefs[KEY_ULTIMO_SYNC] ?: 0L

        Configuracoes(
            frequenciaLembrete = freq,
            antecedenciaVencimento = antecedencia,
            ultimoLembrete = ultimoLembrete,
            tema = tema,
            supabaseUrl = supabaseUrl,
            supabaseAnonKey = supabaseAnonKey,
            ultimoSync = ultimoSync
        )
    }

    suspend fun setFrequenciaLembrete(frequencia: FrequenciaLembrete) {
        context.dataStore.edit { it[KEY_FREQUENCIA_LEMBRETE] = frequencia.name }
    }

    suspend fun setAntecedenciaVencimento(dias: Int) {
        context.dataStore.edit { it[KEY_ANTECEDENCIA_VENCIMENTO] = dias }
    }

    suspend fun setUltimoLembrete(data: LocalDate) {
        context.dataStore.edit { it[KEY_ULTIMO_LEMBRETE] = data.toString() }
    }

    suspend fun setTema(tema: TemaApp) {
        context.dataStore.edit { it[KEY_TEMA] = tema.name }
    }

    suspend fun setSupabaseConfig(url: String, anonKey: String) {
        context.dataStore.edit {
            it[KEY_SUPABASE_URL] = url.trim()
            it[KEY_SUPABASE_ANON_KEY] = anonKey.trim()
        }
    }

    suspend fun setUltimoSync(timestamp: Long) {
        context.dataStore.edit { it[KEY_ULTIMO_SYNC] = timestamp }
    }
}
