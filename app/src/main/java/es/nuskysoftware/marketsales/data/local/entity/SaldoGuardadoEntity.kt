// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/SaldoGuardadoEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saldos_guardados")
data class SaldoGuardadoEntity(
    @PrimaryKey val idRegistro: String,
    val idUsuario: String = "",
    val idMercadilloOrigen: String = "",
    val fechaMercadillo: String = "",
    val lugarMercadillo: String = "",
    val organizadorMercadillo: String = "",
    val horaInicioMercadillo: String = "",
    val saldoInicialGuardado: Double = 0.0,
    val consumido: Boolean = false,
    val version: Long = 1L,
    val lastModified: Long = System.currentTimeMillis(),
    val sincronizadoFirebase: Boolean = false,
    val notas: String? = null,
    val eliminado: Boolean = false,
    val idMercadilloDestino: String? = null,
    val fechaEpochDay: Long? = es.nuskysoftware.marketsales.utils.CalendarDates.epochDay(fechaMercadillo)
)
