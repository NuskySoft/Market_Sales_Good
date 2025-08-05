// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/LineaGastoEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "LineasGastos",
    primaryKeys = ["idMercadillo", "numeroLinea"],
    indices = [
        Index(name = "idx_gastos_mercadillo", value = ["idMercadillo"]),
        Index(name = "idx_gastos_usuario", value = ["idUsuario"])
    ]
)
data class LineaGastoEntity(
    val idMercadillo: String,
    val idUsuario: String,
    val numeroLinea: String,   // zero-padded "0001"
    val descripcion: String,
    val importe: Double,       // siempre positivo
    val fechaHora: Long,
    val formaPago: String      // "efectivo" | "bizum" | "tarjeta"
)


//package es.nuskysoftware.marketsales.data.local.entity
//
//import androidx.room.Entity
//import androidx.room.Index
//
//@Entity(
//    tableName = "LineasGastos",
//    primaryKeys = ["idMercadillo", "numeroLinea"],
//    indices = [
//        Index(name = "idx_gastos_mercadillo", value = ["idMercadillo"]),
//        Index(name = "idx_gastos_usuario", value = ["idUsuario"])
//    ]
//)
//data class LineaGastoEntity(
//    val idMercadillo: String,
//    val idUsuario: String,
//    val numeroLinea: String,   // zero-padded "0001"
//    val descripcion: String,
//    val importe: Double,       // siempre positivo
//    val fechaHora: Long
//)
