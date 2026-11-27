// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/MercadilloEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Mercadillo y su planificación, estado, información económica y control de sincronización.

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * MercadilloEntity V11 - Market Sales
 *
 * ARQUITECTURA HÍBRIDA "RELOJ SUIZO":
 * - Compatible con sincronización Firebase + Room offline-first
 * - Campo userId para vincular con usuario
 * - Estados automáticos según lógica de negocio
 * - Gestión completa de saldos y arqueos
 * - Campos calculados almacenados en BD
 */
@Entity(tableName = "mercadillos")
data class MercadilloEntity(
    @PrimaryKey
    // Identificador del mercadillo al que pertenece la operación.
    val idMercadillo: String = UUID.randomUUID().toString(),

    // ✅ Campo para vincular con usuario
    // Identificador de la cuenta propietaria.
    val userId: String = "",

    // ========== DATOS BÁSICOS (MÍNIMOS REQUERIDOS) ==========
    val fecha: String = "",
    val fechaEpochDay: Long? = es.nuskysoftware.marketsales.utils.CalendarDates.epochDay(fecha), // Formato "dd-MM-yyyy"
    val lugar: String = "",
    val organizador: String = "",

    // ========== CONFIGURACIÓN DEL MERCADILLO ==========
    val esGratis: Boolean = true,
    val importeSuscripcion: Double = 0.0, // Solo si esGratis = false
    val requiereMesa: Boolean = true,
    val requiereCarpa: Boolean = true,
    val hayPuntoLuz: Boolean = false,
    val horaInicio: String = "09:00", // Formato "HH:mm"
    val horaFin: String = "14:00", // Formato "HH:mm"

    // ========== ESTADOS Y CONTROL ==========
    val estado: Int = 1, // Según EstadosMercadillo.Estado.codigo (automático)
    val pendienteArqueo: Boolean = false,
    val pendienteAsignarSaldo: Boolean = false,

    // ========== GESTIÓN FINANCIERA ==========
    // null en saldoInicial = PROGRAMADO_PARCIAL, asignado = PROGRAMADO_TOTAL
    val saldoInicial: Double? = null,
    val saldoFinal: Double? = null,

    // Campos calculados pero almacenados:
    val arqueoCaja: Double? = null, // saldoInicial + ventasEfectivo - gastosEfectivo
    val totalVentas: Double = 0.0, // Ventas en cualquier forma de pago (calculado desde tabla ventas)
    val totalGastos: Double = 0.0, // Gastos en cualquier forma de pago (calculado desde tabla gastos)
    val arqueoMercadillo: Double? = null, // saldoInicial + totalVentas - totalGastos - importeSuscripcion

    // ========== CAMPOS DE CONTROL ==========
    // Marca de actividad; permite distinguir datos vigentes de bajas.
    val activo: Boolean = true,

    // ========== CAMPOS SINCRONIZACIÓN HÍBRIDA ==========
    // Revisión del registro utilizada para el control de cambios.
    val version: Long = 1,
    // Marca temporal de la modificación; no debe confundirse con la hora de subida.
    val lastModified: Long = System.currentTimeMillis(),
    // Indica si esta revisión local está confirmada en Firebase.
    val sincronizadoFirebase: Boolean = false
) {
    // Constructor vacío para Firestore
    constructor() : this(
        idMercadillo = "",
        userId = "",
        fecha = "",
        lugar = "",
        organizador = "",
        esGratis = true,
        importeSuscripcion = 0.0,
        requiereMesa = true,
        requiereCarpa = true,
        hayPuntoLuz = false,
        horaInicio = "09:00",
        horaFin = "14:00",
        estado = 1,
        pendienteArqueo = false,
        pendienteAsignarSaldo = false,
        saldoInicial = null,
        saldoFinal = null,
        arqueoCaja = null,
        totalVentas = 0.0,
        totalGastos = 0.0,
        arqueoMercadillo = null,
        activo = true,
        version = 1,
        lastModified = System.currentTimeMillis(),
        sincronizadoFirebase = false
    )
}
