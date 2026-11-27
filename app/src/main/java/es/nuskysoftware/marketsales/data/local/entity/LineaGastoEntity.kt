// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/LineaGastoEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Gasto individual, cuya clave combina el mercadillo y número de línea.

import androidx.room.Entity
import androidx.room.Index

/**
 * LineaGastoEntity V16 - incorpora control de versión, sincronización
 * y flag para saber si el gasto en efectivo debe descontarse de caja.
 */
@Entity(
    tableName = "LineasGastos",
    primaryKeys = ["idMercadillo", "numeroLinea"],
    indices = [
        Index(name = "idx_gastos_mercadillo", value = ["idMercadillo"]),
        Index(name = "idx_gastos_usuario", value = ["idUsuario"])
    ]
)
data class LineaGastoEntity(
    // Identificador del mercadillo al que pertenece la operación.
    val idMercadillo: String,
    // Identificador del propietario de los datos.
    val idUsuario: String,
    val numeroLinea: String,   // zero-padded "0001"
    val descripcion: String,
    val importe: Double,       // siempre positivo
    // Instante de la operación.
    val fechaHora: Long,
    val formaPago: String,     // "efectivo" | "bizum" | "tarjeta"
    val descontarDeCaja: Boolean = true,   // ✅ nuevo: solo estos cuentan para arqueo de caja
    // Revisión del registro utilizada para el control de cambios.
    val version: Long = 1L,
    // Marca temporal de la modificación; no debe confundirse con la hora de subida.
    val lastModified: Long = System.currentTimeMillis(),
    // Indica si esta revisión local está confirmada en Firebase.
    val sincronizadoFirebase: Boolean = false,
    // Marca de actividad; permite distinguir datos vigentes de bajas.
    val activo: Boolean = true
) {
    @Suppress("unused")
    constructor() : this(
        idMercadillo = "",
        idUsuario = "",
        numeroLinea = "",
        descripcion = "",
        importe = 0.0,
        fechaHora = 0L,
        formaPago = "efectivo",
        descontarDeCaja = true,
        version = 1L,
        lastModified = System.currentTimeMillis(),
        sincronizadoFirebase = false,
        activo = true
    )
}



//// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/LineaGastoEntity.kt
///**
// * LineaGastoEntity V15 - incorpora control de versión y sincronización.
// */
//        Index(name = "idx_gastos_mercadillo", value = ["idMercadillo"]),
//        Index(name = "idx_gastos_usuario", value = ["idUsuario"])
//) {
//    constructor() : this(
