// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/LineaVentaEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Líneas de recibos de venta y abono; conservan artículo, cantidades, precios y relación con la operación.

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
    // Identificador del recibo asociado.
    val idRecibo: String,
    // Identificador del mercadillo al que pertenece la operación.
    val idMercadillo: String,
    // Identificador del propietario de los datos.
    val idUsuario: String,
    // Número de línea dentro del mercadillo.
    val numeroLinea: Int,
    val tipoLinea: String,
    val descripcion: String,
    val idProducto: String? = null,
    // Número de unidades de la línea.
    val cantidad: Int,
    // Precio de una unidad.
    val precioUnitario: Double,
    // Importe de la línea, incluyendo su signo.
    val subtotal: Double,
    // Referencia a la línea original de venta que se devuelve.
    val idLineaOriginalAbonada: String? = null,
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