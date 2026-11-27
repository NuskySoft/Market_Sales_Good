package es.nuskysoftware.marketsales.utils

import java.time.LocalDate
import java.time.Instant

/**
 * Política común para alta y cambios de fecha. Hoy y futuro son válidos;
 * las horas orientativas de inicio y fin no intervienen en el alta.
 * Las fechas se interpretan estrictamente y en la zona comercial ya acordada.
 */
object MercadillosValidator {
    /** Admite ISO y dd-MM-uuuu; devuelve null para días inexistentes. */
    fun parseLocalDate(fecha: String): LocalDate? =
        CalendarDates.epochDay(fecha)?.let { LocalDate.ofEpochDay(it) }

    /** No usa milisegundos de medianoche ni formateadores mutables compartidos. */
    fun puedeCrearConFecha(fecha: String, now: Instant = Instant.now()): Boolean =
        parseLocalDate(fecha)?.let { !it.isBefore(CalendarDates.today(now)) } ?: false

    /** Estado inicial: hoy en curso (3); futuro programado parcial (1) o total (2). */
    fun estadoPlanificado(fecha: LocalDate, saldoInicial: Double?, now: Instant = Instant.now()): Int {
        val today = CalendarDates.today(now)
        require(!fecha.isBefore(today)) { "Fecha anterior a hoy" }
        return if (fecha == today) 3 else if (saldoInicial != null) 2 else 1
    }
}
