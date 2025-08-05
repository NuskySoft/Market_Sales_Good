// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/SaldoGuardadoEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saldos_guardados")
data class SaldoGuardadoEntity(
    @PrimaryKey val idRegistro: String,
    val idUsuario: String,
    val idMercadilloOrigen: String,
    val fechaMercadillo: String,
    val lugarMercadillo: String,
    val organizadorMercadillo: String,
    val horaInicioMercadillo: String,
    val saldoInicialGuardado: Double,
    val consumido: Boolean = false,
    val version: Long = 1L,
    val lastModified: Long = System.currentTimeMillis(),
    val sincronizadoFirebase: Boolean = false,
    val notas: String? = null
)



//// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/SaldoGuardadoEntity.kt
//package es.nuskysoftware.marketsales.data.local.entity
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
///**
// * Registro de saldo final guardado para asignarlo más adelante.
// * Por ahora solo manejamos un registro "activo" (no consumido) por usuario.
// */
//@Entity(tableName = "saldos_guardados")
//data class SaldoGuardadoEntity(
//    @PrimaryKey val idRegistro: String,          // UUID
//    val idUsuario: String,
//    val idMercadilloOrigen: String,
//    val fechaMercadillo: String,                 // dd-MM-yyyy
//    val lugarMercadillo: String,
//    val organizadorMercadillo: String,
//    val horaInicioMercadillo: String,
//    val saldoInicialGuardado: Double,            // importe ajustado (retirar/añadir)
//    val consumido: Boolean = false,              // marcado cuando se asigna a un mercadillo
//    val version: Long = 1L,
//    val lastModified: Long = System.currentTimeMillis(),
//    val sincronizadoFirebase: Boolean = false,
//    val notas: String? = null
//)
