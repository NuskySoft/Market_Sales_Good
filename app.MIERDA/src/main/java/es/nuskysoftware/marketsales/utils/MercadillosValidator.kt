package es.nuskysoftware.marketsales.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Validador de fechas para Mercadillos.
 * Regla clave para ALTA/EDICIÓN:
 *   - Se permite crear en "hoy" y en futuro.
 *   - Se rechaza cualquier fecha anterior a hoy.
 *   - Se IGNORAN horas de inicio/fin para la validación de alta.
 *
 * Soporta dos formatos de fecha habituales:
 *   - "yyyy-MM-dd" (ISO)
 *   - "dd-MM-yyyy"
 */
object MercadillosValidator {

    @RequiresApi(Build.VERSION_CODES.O)
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE
    @RequiresApi(Build.VERSION_CODES.O)
    private val dmy = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /** true si la fecha es hoy o futuro; false si es pasado o inválida */
    @RequiresApi(Build.VERSION_CODES.O)
    fun puedeCrearConFecha(fecha: String): Boolean {
        val f = parseLocalDate(fecha) ?: return false
        val hoy = LocalDate.now()
        return !f.isBefore(hoy) // hoy y futuro permitido
    }

    /** Intenta parsear con ISO y con dd-MM-yyyy */
    @RequiresApi(Build.VERSION_CODES.O)
    fun parseLocalDate(fecha: String): LocalDate? {
        return tryParse(fecha, iso) ?: tryParse(fecha, dmy)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun tryParse(s: String, fmt: DateTimeFormatter): LocalDate? {
        return try { LocalDate.parse(s, fmt) } catch (_: DateTimeParseException) { null }
    }
}
