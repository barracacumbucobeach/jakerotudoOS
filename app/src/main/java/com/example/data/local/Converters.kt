package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.FrequenciaPlano
import com.example.data.model.Pagamento
import com.example.data.model.Plano
import com.example.data.model.StatusOrdem
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromStatusOrdem(status: StatusOrdem?): String = status?.name ?: StatusOrdem.ABERTA.name

    @TypeConverter
    fun toStatusOrdem(value: String?): StatusOrdem = try {
        StatusOrdem.valueOf(value ?: StatusOrdem.ABERTA.name)
    } catch (_: Exception) {
        StatusOrdem.ABERTA
    }

    @TypeConverter
    fun fromPlano(plano: Plano?): String? {
        if (plano == null) return null
        val obj = JSONObject().apply {
            put("parcelas", plano.parcelas)
            put("primeiroVencimento", plano.primeiroVencimento.toString())
            put("frequencia", plano.frequencia.name)
        }
        return obj.toString()
    }

    @TypeConverter
    fun toPlano(value: String?): Plano? {
        if (value.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(value)
            val parcelas = obj.getInt("parcelas")
            val primeiroVencimento = LocalDate.parse(obj.getString("primeiroVencimento"))
            val frequencia = FrequenciaPlano.valueOf(obj.getString("frequencia"))
            Plano(parcelas, primeiroVencimento, frequencia)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromPagamentos(list: List<Pagamento>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        list.forEach { pag ->
            val obj = JSONObject().apply {
                put("id", pag.id)
                put("data", pag.data.toString())
                put("valor", pag.valor)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toPagamentos(value: String?): List<Pagamento> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            val result = mutableListOf<Pagamento>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    Pagamento(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        data = LocalDate.parse(obj.getString("data")),
                        valor = obj.getDouble("valor")
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            val result = mutableListOf<String>()
            for (i in 0 until array.length()) {
                result.add(array.getString(i))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }
}
