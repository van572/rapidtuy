package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class SupabaseSyncState {
    object Idle : SupabaseSyncState()
    object Loading : SupabaseSyncState()
    object Synced : SupabaseSyncState()
    data class Error(val message: String) : SupabaseSyncState()
}

object SupabaseSyncManager {
    private const val TAG = "SupabaseSyncManager"
    
    // Default fallback Supabase credentials for quick playground testing
    // Users can customize these credentials in the UI settings
    private val _supabaseUrl = MutableStateFlow("https://tapwqojfbrcflykmpadq.supabase.co")
    val supabaseUrl = _supabaseUrl.asStateFlow()

    private val _supabaseKey = MutableStateFlow("sb_publishable_NEFvHWpbituYJf2O05RT8g_9h1XRHon")
    val supabaseKey = _supabaseKey.asStateFlow()

    private val _syncState = MutableStateFlow<SupabaseSyncState>(SupabaseSyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated = _isOfflineSimulated.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled = _isEnabled.asStateFlow()

    private val _realtimeMotorizados = MutableStateFlow<List<MotorizadoEntity>>(emptyList())
    val realtimeMotorizados = _realtimeMotorizados.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var pollJob: Job? = null
    private var isInitialized = false
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext
        Log.d(TAG, "SupabaseSyncManager initialized.")
        startPolling()
    }

    fun updateCredentials(url: String, key: String) {
        _supabaseUrl.value = url.trim().removeSuffix("/")
        _supabaseKey.value = key.trim()
        _syncState.value = SupabaseSyncState.Idle
        Log.d(TAG, "Supabase credentials updated: $url")
        startPolling()
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        if (!enabled) {
            pollJob?.cancel()
            _syncState.value = SupabaseSyncState.Idle
            _realtimeMotorizados.value = emptyList()
        } else {
            startPolling()
        }
    }

    fun setSimulationOffline(offline: Boolean) {
        _isOfflineSimulated.value = offline
        if (offline) {
            pollJob?.cancel()
            _syncState.value = SupabaseSyncState.Error("Conexión con Supabase simulada fuera de línea.")
        } else {
            _syncState.value = SupabaseSyncState.Idle
            startPolling()
        }
    }

    fun startPolling() {
        pollJob?.cancel()
        if (!_isEnabled.value || _isOfflineSimulated.value) return

        pollJob = coroutineScope.launch {
            while (true) {
                if (_isEnabled.value && !_isOfflineSimulated.value) {
                    fetchRealtimeMotorizados()
                }
                delay(4000) // Poll every 4 seconds for real-time changes
            }
        }
    }

    fun forceSingleFetch(onComplete: () -> Unit = {}) {
        if (!_isEnabled.value || _isOfflineSimulated.value) {
            onComplete()
            return
        }
        coroutineScope.launch {
            _syncState.value = SupabaseSyncState.Loading
            fetchRealtimeMotorizados()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private suspend fun fetchRealtimeMotorizados() {
        try {
            val url = "${_supabaseUrl.value}/rest/v1/motorizados?select=*&order=id.asc"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", _supabaseKey.value)
                .addHeader("Authorization", "Bearer ${_supabaseKey.value}")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "[]"
                val jsonArray = org.json.JSONArray(responseBody)
                val list = mutableListOf<MotorizadoEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val motorizado = MotorizadoEntity(
                        id = obj.optInt("id"),
                        nombre = obj.optString("nombre", ""),
                        placa = obj.optString("placa", ""),
                        telefono = obj.optString("telefono", ""),
                        estado = obj.optInt("estado", 1),
                        latitud = obj.optDouble("latitud", 0.0),
                        longitud = obj.optDouble("longitud", 0.0),
                        fechaVencimiento = obj.optLong("fecha_vencimiento", 0L),
                        ultimoPagoMonto = obj.optDouble("ultimo_pago_monto", 0.0),
                        ultimoPagoFecha = obj.optLong("ultimo_pago_fecha", 0L),
                        comentarios = obj.optString("comentarios", "")
                    )
                    list.add(motorizado)
                }
                _realtimeMotorizados.value = list
                _syncState.value = SupabaseSyncState.Synced
                appContext?.let { ctx ->
                    try {
                        val dao = AppDatabase.getDatabase(ctx, coroutineScope).rapidTuyDao()
                        dao.insertMotorizados(list)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error writing synced motorizados to Room: ${e.message}")
                    }
                }
            } else {
                val errorMsg = "HTTP ${response.code}: ${response.body?.string()?.take(100) ?: "Error"}"
                _syncState.value = SupabaseSyncState.Error("Fallo lectura Supabase: $errorMsg")
                Log.e(TAG, "Error fetching motorizados from Supabase: $errorMsg")
            }
            response.close()
        } catch (e: Exception) {
            _syncState.value = SupabaseSyncState.Error("Fallo red Supabase: ${e.message}")
            Log.e(TAG, "Exception fetching motorizados from Supabase: ${e.message}", e)
        }
    }

    fun syncMotorizado(driver: MotorizadoEntity) {
        if (!_isEnabled.value) return
        if (_isOfflineSimulated.value) {
            _syncState.value = SupabaseSyncState.Error("Supabase Offline: Motorizado #${driver.id} no pudo sincronizarse.")
            return
        }

        coroutineScope.launch {
            _syncState.value = SupabaseSyncState.Loading
            try {
                val json = JSONObject().apply {
                    put("id", driver.id)
                    put("nombre", driver.nombre)
                    put("placa", driver.placa)
                    put("telefono", driver.telefono)
                    put("estado", driver.estado)
                    put("latitud", driver.latitud)
                    put("longitud", driver.longitud)
                    put("fecha_vencimiento", driver.fechaVencimiento)
                    put("ultimo_pago_monto", driver.ultimoPagoMonto)
                    put("ultimo_pago_fecha", driver.ultimoPagoFecha)
                    put("comentarios", driver.comentarios)
                    put("updated_at", System.currentTimeMillis())
                }

                val url = "${_supabaseUrl.value}/rest/v1/motorizados"
                val requestBody = json.toString().toRequestBody(jsonMediaType)

                // Use PostgREST upsert headers:
                // - Prefer: resolution=merge-duplicates, return=minimal
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("apikey", _supabaseKey.value)
                    .addHeader("Authorization", "Bearer ${_supabaseKey.value}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates, return=minimal")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    _syncState.value = SupabaseSyncState.Synced
                    Log.d(TAG, "Motorizado #${driver.id} upserted to Supabase successfully.")
                    // Refetch instantly so list stays fresh
                    fetchRealtimeMotorizados()
                } else {
                    val errMsg = "HTTP ${response.code}: ${response.body?.string() ?: "Response Error"}"
                    _syncState.value = SupabaseSyncState.Error("Error en Supabase: $errMsg")
                    Log.e(TAG, "Supabase upsert failed: $errMsg")
                }
                response.close()
            } catch (e: Exception) {
                _syncState.value = SupabaseSyncState.Error("Fallo de red: ${e.message}")
                Log.e(TAG, "Network failure syncing motorizado to Supabase: ${e.message}", e)
            }
        }
    }

    fun deleteMotorizado(id: Int) {
        if (!_isEnabled.value || _isOfflineSimulated.value) return
        coroutineScope.launch {
            try {
                val url = "${_supabaseUrl.value}/rest/v1/motorizados?id=eq.$id"
                val request = Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("apikey", _supabaseKey.value)
                    .addHeader("Authorization", "Bearer ${_supabaseKey.value}")
                    .build()

                val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
                if (response.isSuccessful) {
                    Log.d(TAG, "Motorizado #$id deleted from Supabase.")
                    fetchRealtimeMotorizados()
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed deleting motorizado #$id from Supabase: ${e.message}")
            }
        }
    }

    fun deleteAllMotorizados() {
        if (!_isEnabled.value || _isOfflineSimulated.value) return
        coroutineScope.launch {
            try {
                val url = "${_supabaseUrl.value}/rest/v1/motorizados?id=gt.0"
                val request = Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("apikey", _supabaseKey.value)
                    .addHeader("Authorization", "Bearer ${_supabaseKey.value}")
                    .build()

                val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
                if (response.isSuccessful) {
                    Log.d(TAG, "All motorizados deleted from Supabase.")
                    fetchRealtimeMotorizados()
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed deleting all motorizados from Supabase: ${e.message}")
            }
        }
    }

    fun syncTrip(trip: TripEntity) {
        if (!_isEnabled.value) return
        if (_isOfflineSimulated.value) {
            _syncState.value = SupabaseSyncState.Error("Supabase Offline: Viaje #${trip.id} no pudo sincronizarse.")
            return
        }

        coroutineScope.launch {
            _syncState.value = SupabaseSyncState.Loading
            try {
                val json = JSONObject().apply {
                    put("id", trip.id)
                    put("origen", trip.origen)
                    put("destino", trip.destino)
                    put("monto", trip.monto)
                    put("estado", trip.estado)
                    put("motorizado_id", trip.motorizadoId)
                    put("segundos_restantes", trip.segundosRestantes)
                    put("intentos_asignacion", trip.intentosAsignacion)
                    put("lista_negra_ids", trip.listaNegraIds)
                    put("created_at", trip.createdAt)
                    put("updated_at", System.currentTimeMillis())
                }

                val url = "${_supabaseUrl.value}/rest/v1/trips"
                val requestBody = json.toString().toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("apikey", _supabaseKey.value)
                    .addHeader("Authorization", "Bearer ${_supabaseKey.value}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates, return=minimal")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    _syncState.value = SupabaseSyncState.Synced
                    Log.d(TAG, "Trip #${trip.id} upserted to Supabase successfully.")
                } else {
                    val errMsg = "HTTP ${response.code}: ${response.body?.string() ?: "Response Error"}"
                    _syncState.value = SupabaseSyncState.Error("Error en Supabase: $errMsg")
                    Log.e(TAG, "Supabase upsert failed: $errMsg")
                }
                response.close()
            } catch (e: Exception) {
                _syncState.value = SupabaseSyncState.Error("Fallo de red: ${e.message}")
                Log.e(TAG, "Network failure syncing trip to Supabase: ${e.message}", e)
            }
        }
    }
}
