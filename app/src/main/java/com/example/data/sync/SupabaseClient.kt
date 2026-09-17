package com.example.data.sync

import android.util.Log
import com.example.data.local.Converters
import com.example.data.model.Ordem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class SupabaseClient(
    private val baseUrl: String,
    private val anonKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    fun cleanBaseUrl(): String {
        return baseUrl.trim().removeSuffix("/")
    }

    /**
     * Garante que o bucket público `fotos` exista no Supabase Storage.
     */
    suspend fun ensureBucketExists(bucketName: String = "fotos"): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))
        try {
            val url = "${cleanBaseUrl()}/storage/v1/bucket"
            val json = JSONObject().apply {
                put("id", bucketName)
                put("name", bucketName)
                put("public", true)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .post(body)
                .build()
            client.newCall(request).execute().close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envia uma ordem para o Supabase (Upsert via PostgREST).
     */
    suspend fun upsertOrdem(ordem: Ordem): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))

        try {
            val converters = Converters()
            val dadosObj = JSONObject().apply {
                put("cliente", ordem.cliente)
                put("telefone", ordem.telefone)
                put("servico", ordem.servico)
                put("quantidade", ordem.quantidade)
                put("valorUnitario", ordem.valorUnitario)
                put("entrada", ordem.entrada)
                put("desconto", ordem.desconto)
                put("observacoes", ordem.observacoes)
                put("status", ordem.status.name)
                put("data", ordem.data.toString())
                put("entregaPrevista", ordem.entregaPrevista?.toString())
                put("entregaReal", ordem.entregaReal?.toString())
                put("vencimento", ordem.vencimento?.toString())
                put("plano", converters.fromPlano(ordem.plano))
                put("pagamentos", converters.fromPagamentos(ordem.pagamentos))
                put("fotos", converters.fromStringList(ordem.fotos))
            }

            val payload = JSONObject().apply {
                if (!ordem.cloudId.isNullOrBlank()) {
                    put("id", ordem.cloudId)
                }
                put("num", ordem.num)
                put("dados", dadosObj)
                put("removido", ordem.removido)
                put("atualizado_em", Instant.ofEpochMilli(ordem.atualizadoEm).toString())
            }

            val url = "${cleanBaseUrl()}/rest/v1/ordens"
            val body = payload.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    var cloudId = ordem.cloudId ?: ""
                    try {
                        val array = JSONArray(respBody)
                        if (array.length() > 0) {
                            cloudId = array.getJSONObject(0).getString("id")
                        }
                    } catch (_: Exception) {}
                    Result.success(cloudId)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao enviar ordem", e)
            Result.failure(e)
        }
    }

    /**
     * Busca ordens atualizadas após determinado timestamp ISO 8601.
     */
    suspend fun fetchOrdensModificadas(ultimoSyncMillis: Long): Result<List<Ordem>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))

        try {
            val isoSince = Instant.ofEpochMilli(ultimoSyncMillis).toString()
            val url = "${cleanBaseUrl()}/rest/v1/ordens?select=*&atualizado_em=gt.$isoSince&order=atualizado_em.asc"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val bodyStr = response.body?.string() ?: "[]"
                val array = JSONArray(bodyStr)
                val converters = Converters()
                val list = mutableListOf<Ordem>()

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val cloudId = item.getString("id")
                    val num = item.getInt("num")
                    val removido = item.getBoolean("removido")
                    val atualizadoEmStr = item.getString("atualizado_em")
                    val atualizadoEm = try {
                        Instant.parse(atualizadoEmStr).toEpochMilli()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }

                    val dados = item.getJSONObject("dados")
                    val plano = converters.toPlano(dados.optString("plano", null))
                    val pagamentos = converters.toPagamentos(dados.optString("pagamentos", "[]"))
                    val fotos = converters.toStringList(dados.optString("fotos", "[]"))

                    val data = try {
                        LocalDate.parse(dados.getString("data"))
                    } catch (_: Exception) {
                        LocalDate.now()
                    }

                    val entregaPrevista = dados.optString("entregaPrevista", null)?.let {
                        try { LocalDate.parse(it) } catch (_: Exception) { null }
                    }
                    val entregaReal = dados.optString("entregaReal", null)?.let {
                        try { LocalDate.parse(it) } catch (_: Exception) { null }
                    }
                    val vencimento = dados.optString("vencimento", null)?.let {
                        try { LocalDate.parse(it) } catch (_: Exception) { null }
                    }

                    val ordem = Ordem(
                        num = num,
                        data = data,
                        cliente = dados.optString("cliente", ""),
                        telefone = dados.optString("telefone", ""),
                        servico = dados.optString("servico", ""),
                        quantidade = dados.optDouble("quantidade", 1.0),
                        valorUnitario = dados.optDouble("valorUnitario", 0.0),
                        entrada = dados.optDouble("entrada", 0.0),
                        desconto = dados.optDouble("desconto", 0.0),
                        observacoes = dados.optString("observacoes", ""),
                        status = converters.toStatusOrdem(dados.optString("status")),
                        entregaPrevista = entregaPrevista,
                        entregaReal = entregaReal,
                        vencimento = vencimento,
                        plano = plano,
                        pagamentos = pagamentos,
                        fotos = fotos,
                        atualizadoEm = atualizadoEm,
                        removido = removido,
                        sincronizado = true,
                        cloudId = cloudId
                    )
                    list.add(ordem)
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar ordens", e)
            Result.failure(e)
        }
    }

    /**
     * Faz upload de arquivo para o Storage bucket `fotos`.
     */
    suspend fun uploadFoto(file: File, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase não configurado"))

        try {
            ensureBucketExists("fotos")
            val url = "${cleanBaseUrl()}/storage/v1/object/fotos/$fileName"
            val body = file.readBytes().toRequestBody("image/jpeg".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey.trim())
                .addHeader("Authorization", "Bearer ${anonKey.trim()}")
                .addHeader("x-upsert", "true")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val publicUrl = "${cleanBaseUrl()}/storage/v1/object/public/fotos/$fileName"
                    Result.success(publicUrl)
                } else {
                    Result.failure(Exception("Erro no upload da foto: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
