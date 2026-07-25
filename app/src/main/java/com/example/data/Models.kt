package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motorizados")
data class MotorizadoEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val placa: String,
    val telefono: String,
    val estado: Int, // 1 = Disponible, 2 = Ocupado, 3 = Fuera de Servicio, 4 = Bloqueado por Impago
    val latitud: Double,
    val longitud: Double,
    val fechaVencimiento: Long, // timestamp of next due payment
    val ultimoPagoMonto: Double = 0.0,
    val ultimoPagoFecha: Long = 0,
    val comentarios: String = ""
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val origen: String,
    val destino: String,
    val monto: Double,
    val estado: String, // "PENDIENTE", "REASIGNADO", "ACEPTADO", "RECHAZADO", "TIMEOUT", "COMPLETADO", "CANCELADO"
    val motorizadoId: Int?, // current candidate motorizado
    val segundosRestantes: Int = 15,
    val intentosAsignacion: Int = 0,
    val listaNegraIds: String = "", // comma-separated driver IDs that rejected or timed out
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_logs")
data class PaymentLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val motorizadoId: Int,
    val conductorNombre: String,
    val monto: Double,
    val fecha: Long,
    val referencia: String
)
