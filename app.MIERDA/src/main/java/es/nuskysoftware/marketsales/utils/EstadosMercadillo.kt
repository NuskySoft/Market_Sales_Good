// app/src/main/java/es/nuskysoftware/marketsales/utils/EstadosMercadillo.kt
package es.nuskysoftware.marketsales.utils

import android.util.Log
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * EstadosMercadillo V11 - Market Sales
 *
 * ✅ Sistema centralizado para gestionar todos los estados de mercadillos
 * ✅ Incluye colores, descripciones y lógica relacionada
 * ✅ Estados automáticos basados en condiciones de negocio
 * ✅ Compatible con arquitectura híbrida "Reloj Suizo"
 */
object EstadosMercadillo {

    /**
     * Enum con todos los estados posibles de un mercadillo
     * Los códigos coinciden con el campo 'estado' en MercadilloEntity
     */
    enum class Estado(val codigo: Int, val descripcion: String) {
        PROGRAMADO_PARCIAL(1, "Programado parcialmente"),
        PROGRAMADO_TOTAL(2, "Programado totalmente"),
        EN_CURSO(3, "En curso"),
        PENDIENTE_ARQUEO(4, "Terminado (pendiente arqueo)"),
        PENDIENTE_ASIGNAR_SALDO(5, "Arqueo realizado (pendiente asignar saldo)"),
        CERRADO_COMPLETO(6, "Cerrado completamente"),
        CANCELADO(7, "Cancelado");

        companion object {
            /**
             * Obtiene el estado por su código
             */
            fun fromCodigo(codigo: Int): Estado? = values().find { it.codigo == codigo }

            /**
             * Obtiene el estado por defecto para nuevos mercadillos
             */
            fun getEstadoInicial(): Estado = PROGRAMADO_PARCIAL
        }
    }

    /**
     * Obtiene el color asociado a cada estado para la UI
     */
    fun obtenerColor(estado: Estado): Color {
        return when (estado) {
            Estado.PROGRAMADO_PARCIAL -> Color(0xFF81C7E8)      // Azul claro
            Estado.PROGRAMADO_TOTAL -> Color(0xFF1976D2)        // Azul oscuro
            Estado.EN_CURSO -> Color(0xFF4CAF50)                // Verde
            Estado.PENDIENTE_ARQUEO -> Color(0xFFFF9800)        // Naranja ⚠️
            Estado.PENDIENTE_ASIGNAR_SALDO -> Color(0xFFF44336) // Rojo ⚠️
            Estado.CERRADO_COMPLETO -> Color(0xFF2E7D32)        // Verde oscuro
            Estado.CANCELADO -> Color(0xFF757575)               // Gris
        }
    }

    /**
     * Obtiene el color de texto más apropiado para cada fondo
     */
    fun obtenerColorTexto(estado: Estado): Color {
        return when (estado) {
            Estado.PROGRAMADO_PARCIAL -> Color.Black
            Estado.PROGRAMADO_TOTAL -> Color.White
            Estado.EN_CURSO -> Color.White
            Estado.PENDIENTE_ARQUEO -> Color.Black
            Estado.PENDIENTE_ASIGNAR_SALDO -> Color.White
            Estado.CERRADO_COMPLETO -> Color.White
            Estado.CANCELADO -> Color.White
        }
    }

    /**
     * Devuelve todos los estados para mostrar en la leyenda
     */
    fun obtenerTodosLosEstados(): List<Estado> {
        return Estado.values().toList()
    }

    /**
     * Obtiene el icono/emoji representativo de cada estado
     */
    fun obtenerIcono(estado: Estado): String {
        return when (estado) {
            Estado.PROGRAMADO_PARCIAL -> "📋"
            Estado.PROGRAMADO_TOTAL -> "📅"
            Estado.EN_CURSO -> "🟢"
            Estado.PENDIENTE_ARQUEO -> "⚠️"
            Estado.PENDIENTE_ASIGNAR_SALDO -> "🔴"
            Estado.CERRADO_COMPLETO -> "✅"
            Estado.CANCELADO -> "❌"
        }
    }

    /**
     * Indica si el estado requiere atención urgente del usuario
     */
    fun requiereAtencion(estado: Estado): Boolean {
        return estado == Estado.PENDIENTE_ARQUEO || estado == Estado.PENDIENTE_ASIGNAR_SALDO
    }

    /**
     * Obtiene el orden de prioridad para mostrar en calendario cuando hay múltiples mercadillos
     * Menor número = mayor prioridad
     */
    fun obtenerPrioridad(estado: Estado): Int {
        return when (estado) {
            Estado.EN_CURSO -> 1                    // Máxima prioridad
            Estado.PENDIENTE_ARQUEO -> 2            // Urgente
            Estado.PENDIENTE_ASIGNAR_SALDO -> 3     // Urgente
            Estado.PROGRAMADO_TOTAL -> 4
            Estado.PROGRAMADO_PARCIAL -> 5
            Estado.CERRADO_COMPLETO -> 6
            Estado.CANCELADO -> 7                   // Mínima prioridad
        }
    }

    /**
     * Indica si un mercadillo puede ser cancelado
     * Solo se puede cancelar si no tiene ventas asociadas
     */
    fun puedeSerCancelado(estado: Estado, tieneVentas: Boolean): Boolean {
        return !tieneVentas && estado != Estado.CANCELADO && estado != Estado.CERRADO_COMPLETO
    }

    /**
     * Indica si se pueden asignar ventas a un mercadillo
     * Solo se pueden asignar ventas a mercadillos EN_CURSO
     */
    fun puedeRecibirVentas(estado: Estado): Boolean {
        return estado == Estado.EN_CURSO
    }

    /**
     * Calcula el estado automático basado en fecha, hora y datos del mercadillo
     */
    fun calcularEstadoAutomatico(
        saldoInicial: Double?,
        fecha: String,
        horaFin: String,
        arqueoCaja: Double?,
        pendienteAsignarSaldo: Boolean,
        fechaActual: String,
        horaActual: String
    ): Estado {
        try {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val fechaMercadillo = dateFormat.parse(fecha)
            val fechaHoy = dateFormat.parse(fechaActual)
            val horaFinMercadillo = timeFormat.parse(horaFin)
            val horaAhora = timeFormat.parse(horaActual)

            if (fechaMercadillo == null || fechaHoy == null || horaFinMercadillo == null || horaAhora == null) {
                return Estado.PROGRAMADO_PARCIAL
            }

            // Calcular ayer
            val calendar = Calendar.getInstance()
            calendar.time = fechaHoy
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val ayer = calendar.time

            // Si es anterior a ayer → mantener estado actual
            if (fechaMercadillo.before(ayer)) {
                return when {
                    arqueoCaja != null -> Estado.CERRADO_COMPLETO
                    pendienteAsignarSaldo -> Estado.PENDIENTE_ASIGNAR_SALDO
                    else -> if (saldoInicial != null) Estado.PROGRAMADO_TOTAL else Estado.PROGRAMADO_PARCIAL
                }
            }

            // Si es mañana o futuro → PROGRAMADO
            if (fechaMercadillo.after(fechaHoy)) {
                return if (saldoInicial != null) Estado.PROGRAMADO_TOTAL else Estado.PROGRAMADO_PARCIAL
            }

            // Si es HOY → ACTUAL
            if (fechaMercadillo.equals(fechaHoy)) {
                return Estado.EN_CURSO
            }

            // Si es AYER y son menos de las 5:00am → ACTUAL (mercadillos nocturnos)
            calendar.time = fechaHoy
            calendar.set(Calendar.HOUR_OF_DAY, 5)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val cincoAM = timeFormat.format(calendar.time)
            val cincoAMTime = timeFormat.parse(cincoAM)

            if (fechaMercadillo.equals(ayer) && horaAhora != null && cincoAMTime != null && horaAhora.before(cincoAMTime)) {
                return Estado.EN_CURSO
            }

            // Si es AYER y son las 5:00am o más tarde → PENDIENTE_ARQUEO
            if (fechaMercadillo.equals(ayer)) {
                return when {
                    arqueoCaja != null && pendienteAsignarSaldo -> Estado.PENDIENTE_ASIGNAR_SALDO
                    arqueoCaja != null -> Estado.CERRADO_COMPLETO
                    else -> Estado.PENDIENTE_ARQUEO
                }
            }

            // Por defecto
            return when {
                arqueoCaja != null && !pendienteAsignarSaldo -> Estado.CERRADO_COMPLETO
                arqueoCaja != null && pendienteAsignarSaldo -> Estado.PENDIENTE_ASIGNAR_SALDO
                else -> Estado.PENDIENTE_ARQUEO
            }

        } catch (e: Exception) {
            Log.e("EstadosMercadillo", "Error calculando estado automático", e)
            return if (saldoInicial != null) Estado.PROGRAMADO_TOTAL else Estado.PROGRAMADO_PARCIAL
        }
    }
}
