// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/LineaVentaEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity

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
    val idLineaOriginalAbonada: String? = null
)
