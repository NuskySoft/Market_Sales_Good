// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/SaldoGuardadoEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Saldo reservado para un mercadillo posterior; conserva su origen y disponibilidad.

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saldos_guardados")
data class SaldoGuardadoEntity(
    @PrimaryKey val idRegistro: String,
    // Identificador del propietario de los datos.
    val idUsuario: String = "",
    // Mercadillo del que procede el saldo.
    val idMercadilloOrigen: String = "",
    val fechaMercadillo: String = "",
    val lugarMercadillo: String = "",
    val organizadorMercadillo: String = "",
    val horaInicioMercadillo: String = "",
    // Importe reservado para una futura asignación.
    val saldoInicialGuardado: Double = 0.0,
    // Indica que el saldo ya no está disponible para otra asignación.
    val consumido: Boolean = false,
    // Revisión del registro utilizada para el control de cambios.
    val version: Long = 1L,
    // Marca temporal de la modificación; no debe confundirse con la hora de subida.
    val lastModified: Long = System.currentTimeMillis(),
    // Indica si esta revisión local está confirmada en Firebase.
    val sincronizadoFirebase: Boolean = false,
    val notas: String? = null,
    // Marca de retirada del registro.
    val eliminado: Boolean = false,
    val idMercadilloDestino: String? = null,
    val fechaEpochDay: Long? = es.nuskysoftware.marketsales.utils.CalendarDates.epochDay(fechaMercadillo)
)
