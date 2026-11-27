package es.nuskysoftware.marketsales.utils

// Responsabilidad: Convierte fechas visibles a un valor cronológicamente ordenable y vuelve a formatearlas para la interfaz.

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object CalendarDates {
    // La zona ya utilizada por los estados automáticos define el día comercial de toda la app.
    val marketZone: ZoneId = ZoneId.of("Europe/Madrid")

    /** Día de celebración actual, sin horas; admite un instante explícito para pruebas de medianoche. */
    fun today(now: Instant = Instant.now()): LocalDate = now.atZone(marketZone).toLocalDate()

    private val visible = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)
    // Convierte la fecha visible en un día numérico ordenable; devuelve null ante una fecha no válida.
    fun epochDay(value: String): Long? = runCatching {
        (if (value.length == 10 && value[4] == '-') LocalDate.parse(value) else LocalDate.parse(value, visible)).toEpochDay()
    }.getOrNull()
    // Convierte el día numérico a la representación visible.
    fun display(day: Long): String = LocalDate.ofEpochDay(day).format(visible)
}
