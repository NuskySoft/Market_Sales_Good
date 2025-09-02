// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/LineaVentaEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
/**
 * LineaVentaEntity V15 - incluye campos de versión y sincronización.
 */

@Entity(
    tableName = "lineas_venta",
    primaryKeys = ["idMercadillo", "idLinea"] // PK compuesta
)
data class LineaVentaEntity(
    val idLinea: String,            // "0001", "0002"... reinicio por mercadillo
    val idRecibo: String,
    val idMercadillo: String,
    val idUsuario: String,
    val numeroLinea: Int,
    val tipoLinea: String,
    val descripcion: String,
    val idProducto: String? = null,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val idLineaOriginalAbonada: String? = null,
    val version: Long = 1L,
    val lastModified: Long = System.currentTimeMillis(),
    val sincronizadoFirebase: Boolean = false,
    val activo: Boolean = true
) {
    @Suppress("unused")
    constructor() : this(
        idLinea = "",
        idRecibo = "",
        idMercadillo = "",
        idUsuario = "",
        numeroLinea = 0,
        tipoLinea = "MANUAL",
        descripcion = "",
        idProducto = null,
        cantidad = 0,
        precioUnitario = 0.0,
        subtotal = 0.0,
        idLineaOriginalAbonada = null,
        version = 1L,
        lastModified = System.currentTimeMillis(),
        sincronizadoFirebase = false,
        activo = true
    )
}