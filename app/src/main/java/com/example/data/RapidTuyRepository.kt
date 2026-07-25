package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.*

class RapidTuyRepository(private val dao: RapidTuyDao) {

    val allMotorizados: Flow<List<MotorizadoEntity>> = dao.getAllMotorizados()
    val allTrips: Flow<List<TripEntity>> = dao.getAllTrips()
    val allPaymentLogs: Flow<List<PaymentLogEntity>> = dao.getAllPaymentLogs()

    suspend fun getMotorizadoById(id: Int): MotorizadoEntity? = dao.getMotorizadoById(id)

    suspend fun getTripById(id: Int): TripEntity? = dao.getTripById(id)

    suspend fun updateMotorizado(motorizado: MotorizadoEntity) {
        dao.updateMotorizado(motorizado)
        FirestoreSyncManager.syncMotorizado(motorizado)
        SupabaseSyncManager.syncMotorizado(motorizado)
    }

    suspend fun insertMotorizado(motorizado: MotorizadoEntity) {
        dao.insertMotorizados(listOf(motorizado))
        FirestoreSyncManager.syncMotorizado(motorizado)
        SupabaseSyncManager.syncMotorizado(motorizado)
    }

    suspend fun updateMotorizadoEstado(id: Int, nuevoEstado: Int) {
        dao.updateMotorizadoEstado(id, nuevoEstado)
        dao.getMotorizadoById(id)?.let {
            FirestoreSyncManager.syncMotorizado(it)
            SupabaseSyncManager.syncMotorizado(it)
        }
    }

    suspend fun deleteMotorizado(id: Int) {
        dao.deleteMotorizado(id)
        dao.deletePaymentLogsForMotorizado(id)
        FirestoreSyncManager.deleteMotorizado(id)
        SupabaseSyncManager.deleteMotorizado(id)
    }

    suspend fun clearAllMotorizadosYDatos() {
        dao.clearAllMotorizados()
        dao.clearAllPaymentLogs()
        FirestoreSyncManager.deleteAllMotorizados()
        SupabaseSyncManager.deleteAllMotorizados()
    }

    suspend fun insertTrip(trip: TripEntity): Long {
        val id = dao.insertTrip(trip)
        val insertedTrip = trip.copy(id = id.toInt())
        FirestoreSyncManager.syncTrip(insertedTrip)
        SupabaseSyncManager.syncTrip(insertedTrip)
        return id
    }

    suspend fun updateTrip(trip: TripEntity) {
        dao.updateTrip(trip)
        FirestoreSyncManager.syncTrip(trip)
        SupabaseSyncManager.syncTrip(trip)
    }

    suspend fun clearAllTrips() = dao.clearAllTrips()

    suspend fun insertPaymentLog(log: PaymentLogEntity) = dao.insertPaymentLog(log)

    // Simulates a PostGIS spatial query by calculating spherical distance (Haversine Formula)
    // Returns list of available (estado = 1) motorizados sorted by distance to target point
    suspend fun findClosestAvailableMotorizados(
        lat: Double,
        lng: Double,
        blacklistIds: List<Int> = emptyList()
    ): List<Pair<MotorizadoEntity, Double>> {
        val drivers = dao.getAllMotorizados().first()
        
        return drivers
            .filter { driver ->
                // Filter: must be Available (estado = 1), not in blacklist, and not blocked (estado = 4)
                driver.estado == 1 && !blacklistIds.contains(driver.id)
            }
            .map { driver ->
                val distance = calculateDistanceKm(lat, lng, driver.latitud, driver.longitud)
                Pair(driver, distance)
            }
            .sortedBy { it.second }
    }

    // Cron Job Simulator: Runs at "midnight" to verify payment expirations.
    // If a driver's expiration timestamp is in the past and they are not already blocked,
    // their state is set to "Bloqueado por Impago" (4).
    suspend fun runMidnightPaymentVerificationCron(): Int {
        val now = System.currentTimeMillis()
        val drivers = dao.getAllMotorizados().first()
        var blockedCount = 0

        for (driver in drivers) {
            // If payment expired and they aren't currently blocked/inactive (or even if they were active, block them)
            if (driver.fechaVencimiento < now && driver.estado != 4) {
                val updatedDriver = driver.copy(
                    estado = 4,
                    comentarios = "Bloqueado por saldo semanal vencido"
                )
                dao.updateMotorizado(updatedDriver)
                blockedCount++
            }
        }
        return blockedCount
    }

    // Unblocks a driver by recording a payment, extending their expiration by 7 days,
    // and setting their status back to "Disponible" (1)
    suspend fun processDriverWeeklyPayment(driverId: Int, reference: String, amount: Double) {
        val driver = dao.getMotorizadoById(driverId) ?: return
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        
        // Extend expiration: if already expired, extend from now. If not, extend from current expiration.
        val baseTime = if (driver.fechaVencimiento < now) now else driver.fechaVencimiento
        val newExpiration = baseTime + sevenDaysMs

        val updatedDriver = driver.copy(
            estado = 1, // Set back to Disponible
            fechaVencimiento = newExpiration,
            ultimoPagoMonto = amount,
            ultimoPagoFecha = now,
            comentarios = "Suscripción al día. Pago verificado: $reference"
        )
        dao.updateMotorizado(updatedDriver)

        // Insert payment log
        val log = PaymentLogEntity(
            motorizadoId = driverId,
            conductorNombre = driver.nombre,
            monto = amount,
            fecha = now,
            referencia = reference
        )
        dao.insertPaymentLog(log)
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
