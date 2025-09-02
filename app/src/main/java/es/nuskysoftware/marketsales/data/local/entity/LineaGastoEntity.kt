// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/LineaGastoEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.Index
/**
 * LineaGastoEntity V15 - incorpora control de versión y sincronización.
 */
@Entity(
    tableName = "LineasGastos",
    primaryKeys = ["idMercadillo", "numeroLinea"],
    indices = [
        Index(name = "idx_gastos_mercadillo", value = ["idMercadillo"]),
        Index(name = "idx_gastos_usuario", value = ["idUsuario"])
    ]
)
data class  LineaGastoEntity(
    val idMercadillo: String,
    val idUsuario: String,
    val numeroLinea: String,   // zero-padded "0001"
    val descripcion: String,
    val importe: Double,       // siempre positivo
    val fechaHora: Long,
    val formaPago: String,      // "efectivo" | "bizum" | "tarjeta"
    val version: Long = 1L,
    val lastModified: Long = System.currentTimeMillis(),
    val sincronizadoFirebase: Boolean = false,
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
        version = 1L,
        lastModified = System.currentTimeMillis(),
        sincronizadoFirebase = false,
        activo = true
    )
}

