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
    val estado: String = "COMPLETADO"
)