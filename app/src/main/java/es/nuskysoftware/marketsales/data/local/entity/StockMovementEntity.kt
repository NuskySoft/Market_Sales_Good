package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/** Durable obligation created with a confirmed sale. Applied rows are kept for deduplication. */
@Entity(tableName = "stock_movements", indices = [Index(value = ["aplicado"])])
data class StockMovementEntity(
    @PrimaryKey val idMovimiento: String = "",
    val idUsuario: String = "",
    val idRecibo: String = "",
    val idArticulo: String = "",
    val cantidad: Int = 0,
    val creadoEn: Long = 0,
    val aplicado: Boolean = false,
    val sincronizadoFirebase: Boolean = false,
    val incorporadoEnBase: Boolean = false
)
