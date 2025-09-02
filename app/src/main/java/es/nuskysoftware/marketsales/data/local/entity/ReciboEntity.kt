// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/ReciboEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recibos")
data class ReciboEntity(
    @PrimaryKey val idRecibo: String,
    val idMercadillo: String,
    val idUsuario: String,
    val fechaHora: Long,
    val metodoPago: String,
    val totalTicket: Double,
    val estado: String = "COMPLETADO",
    val version: Long = 1L,
    val lastModified: Long = System.currentTimeMillis(),
    val sincronizadoFirebase: Boolean = false,
    val activo: Boolean = true

){
    // Constructor vacío requerido por Firebase/Room
    constructor() : this(
        idRecibo = "",
        idMercadillo = "",
        idUsuario = "",
        fechaHora = 0L,
        metodoPago = "",
        totalTicket = 0.0,
        estado = "COMPLETADO",
        version = 1L,
        lastModified = System.currentTimeMillis(),
        sincronizadoFirebase = false,
        activo = true
    )
}