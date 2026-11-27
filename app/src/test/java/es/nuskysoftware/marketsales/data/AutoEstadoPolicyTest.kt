package es.nuskysoftware.marketsales.data

// Finalidad de las pruebas: Prueba las transiciones temporales de estados, sus límites horarios y fechas inválidas.

import es.nuskysoftware.marketsales.data.repository.AutoEstadoPolicy
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoEstadoPolicyTest {
    @Test fun medianocheYFinDeVentana() {
        val date = "28-11-2026"
        assertEquals(2, AutoEstadoPolicy.next(2, date, Instant.parse("2026-11-27T22:59:59Z"), false))
        assertEquals(3, AutoEstadoPolicy.next(2, date, Instant.parse("2026-11-27T23:00:00Z"), false))
        assertEquals(3, AutoEstadoPolicy.next(3, date, Instant.parse("2026-11-29T03:59:59Z"), true))
        assertEquals(4, AutoEstadoPolicy.next(3, date, Instant.parse("2026-11-29T04:00:00Z"), true))
        assertEquals(7, AutoEstadoPolicy.next(2, date, Instant.parse("2026-11-30T08:00:00Z"), false))
        for (state in 4..7) assertEquals(state, AutoEstadoPolicy.next(state, date, Instant.parse("2026-12-01T10:00:00Z"), false))
    }

    @Test fun fechaInvalidaNoSeModifica() {
        assertEquals(2, AutoEstadoPolicy.next(2, "31-02-2026", Instant.parse("2026-12-01T00:00:00Z"), false))
    }
}
