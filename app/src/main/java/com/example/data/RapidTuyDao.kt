package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RapidTuyDao {

    // Motorizados
    @Query("SELECT * FROM motorizados ORDER BY id ASC")
    fun getAllMotorizados(): Flow<List<MotorizadoEntity>>

    @Query("SELECT * FROM motorizados WHERE id = :id")
    suspend fun getMotorizadoById(id: Int): MotorizadoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotorizados(motorizados: List<MotorizadoEntity>)

    @Update
    suspend fun updateMotorizado(motorizado: MotorizadoEntity)

    @Query("UPDATE motorizados SET estado = :nuevoEstado WHERE id = :id")
    suspend fun updateMotorizadoEstado(id: Int, nuevoEstado: Int)

    @Query("DELETE FROM motorizados WHERE id = :id")
    suspend fun deleteMotorizado(id: Int)

    @Query("DELETE FROM motorizados")
    suspend fun clearAllMotorizados()

    @Query("DELETE FROM payment_logs WHERE motorizadoId = :id")
    suspend fun deletePaymentLogsForMotorizado(id: Int)

    @Query("DELETE FROM payment_logs")
    suspend fun clearAllPaymentLogs()

    // Trips / Viajes
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Int): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()

    // Payment Logs
    @Query("SELECT * FROM payment_logs ORDER BY fecha DESC")
    fun getAllPaymentLogs(): Flow<List<PaymentLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentLog(log: PaymentLogEntity)
}
