package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.Ordem
import com.example.service.ImageStorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class SyncState {
    object Sincronizado : SyncState()
    object Enviando : SyncState()
    data class Offline(val pendentes: Int) : SyncState()
    object Desconectado : SyncState()
}

class SyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ordemDao = database.ordemDao()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Sincronizado)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var realTimeWebSocket: WebSocket? = null

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun start() {
        scope.launch {
            atualizarContadorPendentes()
            sincronizarTudo()
            conectarRealtime()
            iniciarLoopPolling()
        }
    }

    private fun iniciarLoopPolling() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(20_000L) // Sincroniza a cada 20 segundos
                if (isOnline()) {
                    try {
                        sincronizarTudo()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    suspend fun atualizarContadorPendentes() {
        val pendentes = ordemDao.getPendingSync().size
        if (!isOnline()) {
            _syncState.value = SyncState.Offline(pendentes)
        } else if (pendentes == 0) {
            _syncState.value = SyncState.Sincronizado
        } else {
            _syncState.value = SyncState.Offline(pendentes)
        }
    }

    fun forcarSincronizacao() {
        scope.launch {
            sincronizarTudo()
        }
    }

    suspend fun sincronizarTudo() = withContext(Dispatchers.IO) {
        val config = preferencesManager.configFlow.firstOrNull() ?: return@withContext
        val client = SupabaseClient(config.supabaseUrl, config.supabaseAnonKey)

        val pendentesCount = ordemDao.getPendingSync().size
        if (!isOnline()) {
            _syncState.value = SyncState.Offline(pendentesCount)
            return@withContext
        }

        if (!client.isConfigured) {
            _syncState.value = if (pendentesCount > 0) SyncState.Offline(pendentesCount) else SyncState.Sincronizado
            return@withContext
        }

        _syncState.value = SyncState.Enviando

        try {
            // 1. Enviar alterações locais pendentes
            val pendentes = ordemDao.getPendingSync()
            for (ordem in pendentes) {
                val res = client.upsertOrdem(ordem)
                if (res.isSuccess) {
                    val cloudId = res.getOrNull()
                    ordemDao.update(ordem.copy(sincronizado = true, cloudId = cloudId ?: ordem.cloudId))
                }
            }

            // 2. Baixar novidades do Supabase
            val resBaixar = if (config.ultimoSync == 0L) {
                client.fetchTodasOrdens()
            } else {
                val syncSince = kotlin.math.max(0L, config.ultimoSync - 60_000L)
                client.fetchOrdensModificadas(syncSince)
            }

            if (resBaixar.isSuccess) {
                val ordensRemotas = resBaixar.getOrNull() ?: emptyList()
                var maxAtualizado = config.ultimoSync

                for (remota in ordensRemotas) {
                    val local = (if (!remota.cloudId.isNullOrBlank()) {
                        ordemDao.getByCloudId(remota.cloudId)
                    } else null) ?: (if (remota.num > 0) {
                        ordemDao.getByNum(remota.num)
                    } else null)

                    if (local == null) {
                        if (!remota.removido) {
                            ordemDao.insert(remota.copy(id = 0, sincronizado = true))
                        }
                    } else {
                        if (remota.removido) {
                            ordemDao.update(local.copy(removido = true, sincronizado = true, atualizadoEm = remota.atualizadoEm, cloudId = remota.cloudId))
                        } else {
                            // Conflito: Last-write-wins baseado em atualizadoEm
                            if (remota.atualizadoEm >= local.atualizadoEm || local.sincronizado) {
                                ordemDao.update(remota.copy(id = local.id, sincronizado = true))
                            }
                        }
                    }

                    if (remota.atualizadoEm > maxAtualizado) {
                        maxAtualizado = remota.atualizadoEm
                    }
                }

                preferencesManager.setUltimoSync(maxAtualizado)
            }

            val restante = ordemDao.getPendingSync().size
            if (restante == 0) {
                _syncState.value = SyncState.Sincronizado
            } else {
                _syncState.value = SyncState.Offline(restante)
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Erro na sincronização", e)
            val restante = ordemDao.getPendingSync().size
            _syncState.value = SyncState.Offline(restante)
        }
    }

    private fun conectarRealtime() {
        scope.launch {
            val config = preferencesManager.configFlow.firstOrNull() ?: return@launch
            if (config.supabaseUrl.isBlank() || config.supabaseAnonKey.isBlank()) return@launch

            try {
                val cleanUrl = config.supabaseUrl.trim()
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removeSuffix("/")
                val wsUrl = "wss://$cleanUrl/realtime/v1/websocket?apikey=${config.supabaseAnonKey.trim()}&vsn=1.0.0"

                val okHttpClient = OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build()

                val request = Request.Builder().url(wsUrl).build()

                realTimeWebSocket?.cancel()
                realTimeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        // Entrar no canal de realtime da tabela ordens
                        val joinMsg = JSONObject().apply {
                            put("topic", "realtime:public:ordens")
                            put("event", "phx_join")
                            put("payload", JSONObject())
                            put("ref", "1")
                        }
                        webSocket.send(joinMsg.toString())
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val json = JSONObject(text)
                            val event = json.optString("event")
                            if (event == "INSERT" || event == "UPDATE" || event == "DELETE" || event == "broadcast") {
                                scope.launch {
                                    sincronizarTudo()
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.d("SyncManager", "Realtime WS reconectará oportunamente")
                    }
                })
            } catch (e: Exception) {
                Log.d("SyncManager", "Erro ao conectar realtime", e)
            }
        }
    }
}
