package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class FirestoreSyncState {
    object Idle : FirestoreSyncState()
    object Loading : FirestoreSyncState()
    object Connected : FirestoreSyncState()
    data class Error(val message: String) : FirestoreSyncState()
}

object FirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"
    private var isFirebaseInitialized = false
    private var firestoreInstance: FirebaseFirestore? = null
    private var motorizadosListener: ListenerRegistration? = null
    private var defaultRolesProvisioned = false
    
    // Simulate connection error toggle for demonstration purposes
    private var isSimulationOffline = false

    private val _syncState = MutableStateFlow<FirestoreSyncState>(FirestoreSyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated = _isOfflineSimulated.asStateFlow()

    fun initialize(context: Context) {
        if (isFirebaseInitialized) return
        _syncState.value = FirestoreSyncState.Loading
        try {
            // First attempt: standard auto-initialization from google-services.json
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestoreInstance = FirebaseFirestore.getInstance()
                isFirebaseInitialized = true
                Log.d(TAG, "Firebase initialized successfully via google-services.json configuration.")
                startListening()
                return
            }

            // Second attempt: Manual initialization with fallback options to ensure stability
            // in developer environments where google-services.json may not be pre-configured.
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:437635375840:android:f93f772dfaad4e92a4ef25")
                .setProjectId("rapidtuy-f93f7")
                .setApiKey("AIzaSyD-dummy-key-for-ai-studio-build")
                .build()
            FirebaseApp.initializeApp(context.applicationContext, options)
            firestoreInstance = FirebaseFirestore.getInstance()
            isFirebaseInitialized = true
            Log.d(TAG, "Firebase manually initialized with fallback credentials successfully.")
            startListening()
        } catch (e: Exception) {
            _syncState.value = FirestoreSyncState.Error("Error de inicialización: ${e.message}")
            Log.e(TAG, "Error initializing Firebase Firestore: ${e.message}", e)
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        if (isSimulationOffline) return null
        return firestoreInstance
    }

    fun setSimulationOffline(offline: Boolean) {
        _isOfflineSimulated.value = offline
        isSimulationOffline = offline
        if (offline) {
            motorizadosListener?.remove()
            motorizadosListener = null
            _syncState.value = FirestoreSyncState.Error("Conexión simulada fuera de línea (Modo Offline Activado)")
        } else {
            _syncState.value = FirestoreSyncState.Loading
            startListening()
        }
    }

    private fun startListening() {
        val db = getFirestore()
        if (db == null) {
            if (isSimulationOffline) {
                _syncState.value = FirestoreSyncState.Error("Modo fuera de línea simulado activo.")
            } else {
                _syncState.value = FirestoreSyncState.Error("Instancia de Firestore no disponible.")
            }
            return
        }

        motorizadosListener?.remove()
        
        _syncState.value = FirestoreSyncState.Loading
        
        // Listen to motorizados collection to track connection stability/reads in real-time
        motorizadosListener = db.collection("motorizados")
            .addSnapshotListener { snapshots, error ->
                if (isSimulationOffline) {
                    _syncState.value = FirestoreSyncState.Error("Conexión interrumpida por simulación offline.")
                    return@addSnapshotListener
                }
                
                if (error != null) {
                    _syncState.value = FirestoreSyncState.Error("Error de lectura: ${error.message ?: "Sin conexión"}")
                    Log.e(TAG, "Firestore snapshot listener error: ${error.message}", error)
                    return@addSnapshotListener
                }

                _syncState.value = FirestoreSyncState.Connected
                Log.d(TAG, "Real-time sync connection confirmed. Documents read: ${snapshots?.size() ?: 0}")
                provisionDefaultRoles()
            }
    }

    fun syncMotorizado(driver: MotorizadoEntity) {
        val db = getFirestore()
        if (db == null) {
            Log.e(TAG, "Cannot sync motorizado: Firestore is offline or uninitialized.")
            return
        }
        val data = mapOf(
            "id" to driver.id,
            "nombre" to driver.nombre,
            "placa" to driver.placa,
            "telefono" to driver.telefono,
            "estado" to driver.estado,
            "latitud" to driver.latitud,
            "longitud" to driver.longitud,
            "fechaVencimiento" to driver.fechaVencimiento,
            "ultimoPagoMonto" to driver.ultimoPagoMonto,
            "ultimoPagoFecha" to driver.ultimoPagoFecha,
            "comentarios" to driver.comentarios,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("motorizados")
            .document(driver.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Motorizado #${driver.id} synced with Firestore successfully.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync Motorizado #${driver.id}: ${e.message}")
                _syncState.value = FirestoreSyncState.Error("Fallo de escritura: ${e.message}")
            }
    }

    fun deleteMotorizado(id: Int) {
        val db = getFirestore() ?: return
        db.collection("motorizados").document(id.toString()).delete()
            .addOnSuccessListener { Log.d(TAG, "Motorizado #$id deleted from Firestore.") }
        db.collection("roles").document("motorizado_$id").delete()
            .addOnSuccessListener { Log.d(TAG, "Role motorizado_$id deleted from Firestore.") }
    }

    fun deleteAllMotorizados() {
        val db = getFirestore() ?: return
        db.collection("motorizados").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
                Log.d(TAG, "All motorizados deleted from Firestore.")
            }
    }

    fun syncTrip(trip: TripEntity) {
        val db = getFirestore()
        if (db == null) {
            Log.e(TAG, "Cannot sync trip: Firestore is offline or uninitialized.")
            return
        }
        val data = mapOf(
            "id" to trip.id,
            "origen" to trip.origen,
            "destino" to trip.destino,
            "monto" to trip.monto,
            "estado" to trip.estado,
            "motorizadoId" to trip.motorizadoId,
            "segundosRestantes" to trip.segundosRestantes,
            "intentosAsignacion" to trip.intentosAsignacion,
            "listaNegraIds" to trip.listaNegraIds,
            "createdAt" to trip.createdAt,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("trips")
            .document(trip.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Trip #${trip.id} synced with Firestore successfully.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync Trip #${trip.id}: ${e.message}")
                _syncState.value = FirestoreSyncState.Error("Fallo de escritura de viaje: ${e.message}")
            }
    }

    fun provisionDefaultRoles() {
        if (defaultRolesProvisioned) return
        val db = getFirestore() ?: return
        
        Log.d(TAG, "Provisioning default roles in Firestore...")
        val rolesToCreate = listOf(
            mapOf(
                "id" to "winston",
                "nombre" to "Winston",
                "role" to "OPERATOR",
                "password" to "0000",
                "updatedAt" to System.currentTimeMillis()
            ),
            mapOf(
                "id" to "admin",
                "nombre" to "Administrador General",
                "role" to "OPERATOR",
                "password" to "tuy2026",
                "updatedAt" to System.currentTimeMillis()
            ),
            mapOf(
                "id" to "maria_tuy",
                "nombre" to "María Call Center",
                "role" to "OPERATOR",
                "password" to "123456",
                "updatedAt" to System.currentTimeMillis()
            ),
            mapOf(
                "id" to "ivan",
                "nombre" to "Ivan Admin",
                "role" to "OPERATOR",
                "password" to "2004",
                "updatedAt" to System.currentTimeMillis()
            )
        )
        
        var successCount = 0
        for (roleData in rolesToCreate) {
            val username = roleData["id"] as String
            db.collection("usuarios_roles")
                .document("operator_$username")
                .set(roleData, SetOptions.merge())
                .addOnSuccessListener {
                    successCount++
                    Log.d(TAG, "Default operator '$username' provisioned in Firestore successfully.")
                    if (successCount == rolesToCreate.size) {
                        defaultRolesProvisioned = true
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to provision operator '$username' in Firestore: ${e.message}")
                }
        }
    }

    fun syncUserRole(id: String, nombre: String, role: String, passwordSecured: String, additionalData: Map<String, Any?> = emptyMap()) {
        val db = getFirestore()
        if (db == null) {
            Log.e(TAG, "Cannot sync user role: Firestore is offline.")
            return
        }
        val docId = if (role == "OPERATOR") "operator_${id.lowercase()}" else "motorizado_$id"
        val roleData = mutableMapOf<String, Any>(
            "id" to id,
            "nombre" to nombre,
            "role" to role,
            "password" to passwordSecured,
            "updatedAt" to System.currentTimeMillis()
        )
        for ((key, value) in additionalData) {
            if (value != null) {
                roleData[key] = value
            }
        }

        db.collection("usuarios_roles")
            .document(docId)
            .set(roleData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User role '$docId' synced successfully to Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync user role '$docId' to Firestore: ${e.message}")
            }
    }

    suspend fun fetchUserRole(docId: String): Map<String, Any>? = suspendCancellableCoroutine { continuation ->
        val db = getFirestore()
        if (db == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        db.collection("usuarios_roles")
            .document(docId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    continuation.resume(document.data)
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user role for doc '$docId': ${e.message}", e)
                continuation.resume(null)
            }
    }
}
