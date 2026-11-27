package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Obligación de ajuste de stock vinculada a una venta; persiste para permitir reintentos sin repetir el descuento.

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/** Durable obligation created with a confirmed sale. Applied rows are kept for deduplication. */
@Entity(tableName = "stock_movements", indices = [Index(value = ["aplicado"])])
data class StockMovementEntity(
    @PrimaryKey val idMovimiento: String = "",
    // Identificador del propietario de los datos.
    val idUsuario: String = "",
    // Identificador del recibo asociado.
    val idRecibo: String = "",
    // Identificador estable del artículo.
    val idArticulo: String = "",
    // Número de unidades de la línea.
    val cantidad: Int = 0,
    val creadoEn: Long = 0,
    // Indica si la obligación ya ha sido aplicada localmente.
    val aplicado: Boolean = false,
    // Indica si esta revisión local está confirmada en Firebase.
    val sincronizadoFirebase: Boolean = false,
    val incorporadoEnBase: Boolean = false
)
