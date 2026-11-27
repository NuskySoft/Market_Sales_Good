package es.nuskysoftware.marketsales.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object CalendarDates {
    private val visible = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)
    fun epochDay(value: String): Long? = runCatching {
        (if (value.length == 10 && value[4] == '-') LocalDate.parse(value) else LocalDate.parse(value, visible)).toEpochDay()
    }.getOrNull()
    fun display(day: Long): String = LocalDate.ofEpochDay(day).format(visible)
}
