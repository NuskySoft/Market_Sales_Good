// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/ReciboEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Cabecera de recibo; vincula la operación con la cuenta y el mercadillo.

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "recibos")
data class ReciboEntity(
    @PrimaryKey val idRecibo: String,
    // Identificador del mercadillo al que pertenece la operación.
    val idMercadillo: String,
    // Identificador del propietario de los datos.
    val idUsuario: String,
    // Instante de la operación.
    val fechaHora: Long,
    // Método de pago asociado al recibo.
    val metodoPago: String,
    val totalTicket: Double,
    // Código de estado del registro según las reglas del flujo correspondiente.
    val estado: String = "COMPLETADO",
    // Revisión del registro utilizada para el control de cambios.
    val version: Long = 1L,
    // Marca temporal de la modificación; no debe confundirse con la hora de subida.
    val lastModified: Long = System.currentTimeMillis(),
    // Indica si esta revisión local está confirmada en Firebase.
    val sincronizadoFirebase: Boolean = false,
    // Marca de actividad; permite distinguir datos vigentes de bajas.
    val activo: Boolean = true,
    // Solo se sincroniza este indicador de elegibilidad, nunca el archivo ni sus datos de envío.
    @ColumnInfo(defaultValue = "0") val pdfOriginal: Boolean = false

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