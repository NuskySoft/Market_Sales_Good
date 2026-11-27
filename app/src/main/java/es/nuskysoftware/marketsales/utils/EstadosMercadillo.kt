// app/src/main/java/es/nuskysoftware/marketsales/utils/EstadosMercadillo.kt
package es.nuskysoftware.marketsales.utils

// Responsabilidad: Define los códigos, presentación y reglas auxiliares de los estados de mercadillo.

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
     *
     * Nota: la propiedad 'descripcion' se mantiene por compatibilidad,
     * pero para UI multi-idioma usa:
     *   - obtenerDescripcionKey(estado)  -> clave
     *   - obtenerDescripcion(estado, language) -> texto localizado
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
            /** Obtiene el estado por su código */
            fun fromCodigo(codigo: Int): Estado? = values().find { it.codigo == codigo }

            /** Estado por defecto para nuevos mercadillos */
            fun getEstadoInicial(): Estado = PROGRAMADO_PARCIAL
        }
    }

    /** Devuelve la key de recursos para la descripción localizada de un estado */
    fun obtenerDescripcionKey(estado: Estado): String = when (estado) {
        Estado.PROGRAMADO_PARCIAL       -> "estado_programado_parcial"
        Estado.PROGRAMADO_TOTAL         -> "estado_programado_total"
        Estado.EN_CURSO                 -> "estado_en_curso"
        Estado.PENDIENTE_ARQUEO         -> "estado_pendiente_arqueo"
        Estado.PENDIENTE_ASIGNAR_SALDO  -> "estado_pendiente_asignar_saldo"
        Estado.CERRADO_COMPLETO         -> "estado_cerrado_completo"
        Estado.CANCELADO                -> "estado_cancelado"
    }

    /**
     * Devuelve la descripción localizada para mostrar en UI.
     * @param currentLanguage Idioma actual (p.ej. "es" o "en")
     */
    fun obtenerDescripcion(estado: Estado, currentLanguage: String): String =
        StringResourceManager.getString(obtenerDescripcionKey(estado), currentLanguage)

    /** Color asociado a cada estado para la UI */
    fun obtenerColor(estado: Estado): Color = when (estado) {
        Estado.PROGRAMADO_PARCIAL -> Color(0xFF81C7E8)      // Azul claro
        Estado.PROGRAMADO_TOTAL -> Color(0xFF1976D2)        // Azul oscuro
        Estado.EN_CURSO -> Color(0xFF4CAF50)                // Verde
        Estado.PENDIENTE_ARQUEO -> Color(0xFFFF9800)        // Naranja ⚠️
        Estado.PENDIENTE_ASIGNAR_SALDO -> Color(0xFFF44336) // Rojo ⚠️
        Estado.CERRADO_COMPLETO -> Color(0xFF7B4F7A)        // Morado/granate
        Estado.CANCELADO -> Color(0xFF757575)               // Gris
    }

    /** Color de texto más apropiado para cada fondo */
    fun obtenerColorTexto(estado: Estado): Color = when (estado) {
        Estado.PROGRAMADO_PARCIAL -> Color.Black
        Estado.PROGRAMADO_TOTAL -> Color.White
        Estado.EN_CURSO -> Color.White
        Estado.PENDIENTE_ARQUEO -> Color.Black
        Estado.PENDIENTE_ASIGNAR_SALDO -> Color.White
        Estado.CERRADO_COMPLETO -> Color.White
        Estado.CANCELADO -> Color.White
    }

    /** Todos los estados (leyenda/listas) */
    fun obtenerTodosLosEstados(): List<Estado> = Estado.values().toList()

    /** Emoji representativo de cada estado (no requiere traducción) */
    fun obtenerIcono(estado: Estado): String = when (estado) {
        Estado.PROGRAMADO_PARCIAL -> "📋"
        Estado.PROGRAMADO_TOTAL -> "📅"
        Estado.EN_CURSO -> "🟢"
        Estado.PENDIENTE_ARQUEO -> "⚠️"
        Estado.PENDIENTE_ASIGNAR_SALDO -> "🔴"
        Estado.CERRADO_COMPLETO -> "✅"
        Estado.CANCELADO -> "❌"
    }

    /** ¿Requiere atención urgente del usuario? */
    fun requiereAtencion(estado: Estado): Boolean =
        estado == Estado.PENDIENTE_ARQUEO || estado == Estado.PENDIENTE_ASIGNAR_SALDO

    /** Prioridad para mostrar en calendario: menor número = mayor prioridad */
    fun obtenerPrioridad(estado: Estado): Int = when (estado) {
        Estado.EN_CURSO -> 1
        Estado.PENDIENTE_ARQUEO -> 2
        Estado.PENDIENTE_ASIGNAR_SALDO -> 3
        Estado.PROGRAMADO_TOTAL -> 4
        Estado.PROGRAMADO_PARCIAL -> 5
        Estado.CERRADO_COMPLETO -> 6
        Estado.CANCELADO -> 7
    }

    /** ¿Puede ser cancelado? (si no tiene ventas y no está ya cancelado/cerrado) */
    fun puedeSerCancelado(estado: Estado, tieneVentas: Boolean): Boolean =
        !tieneVentas && estado != Estado.CANCELADO && estado != Estado.CERRADO_COMPLETO

    /** ¿Puede recibir ventas? (solo EN_CURSO) */
    fun puedeRecibirVentas(estado: Estado): Boolean = estado == Estado.EN_CURSO

    /** Cálculo del estado automático (fecha/hora/arqueo) */
    // Determina el código de estado a partir de las condiciones temporales y del mercadillo.
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

            // Si es anterior a ayer → mantener estado actual estimado
            if (fechaMercadillo.before(ayer)) {
                return when {
                    arqueoCaja != null -> Estado.CERRADO_COMPLETO
                    pendienteAsignarSaldo -> Estado.PENDIENTE_ASIGNAR_SALDO
                    else -> if (saldoInicial != null) Estado.PROGRAMADO_TOTAL else Estado.PROGRAMADO_PARCIAL
                }
            }

            // Futuro → PROGRAMADO
            if (fechaMercadillo.after(fechaHoy)) {
                return if (saldoInicial != null) Estado.PROGRAMADO_TOTAL else Estado.PROGRAMADO_PARCIAL
            }

            // Hoy → EN_CURSO
            if (fechaMercadillo.equals(fechaHoy)) {
                return Estado.EN_CURSO
            }

            // Ayer antes de 5:00 → EN_CURSO (nocturnos)
            calendar.time = fechaHoy
            calendar.set(Calendar.HOUR_OF_DAY, 5)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val cincoAM = timeFormat.format(calendar.time)
            val cincoAMTime = timeFormat.parse(cincoAM)

            if (fechaMercadillo.equals(ayer) && horaAhora != null && cincoAMTime != null && horaAhora.before(cincoAMTime)) {
                return Estado.EN_CURSO
            }

            // Ayer 5:00 o más → PENDIENTE_ARQUEO / CERRADO / PENDIENTE_ASIGNAR_SALDO
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
