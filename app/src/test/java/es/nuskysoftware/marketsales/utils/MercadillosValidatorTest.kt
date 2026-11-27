package es.nuskysoftware.marketsales.utils

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

/** Regresión del alta directa de hoy y la reprogramación a hoy, sin depender del reloj de la prueba. */
class MercadillosValidatorTest {
    private val now = Instant.parse("2026-11-27T18:00:00Z")

    @Test fun hoyPermitidoDuranteTodoElDia() {
        for (instant in listOf("2026-11-26T23:00:00Z", "2026-11-27T12:00:00Z", "2026-11-27T22:59:59Z")) {
            val time = Instant.parse(instant)
            assertTrue(MercadillosValidator.puedeCrearConFecha("27-11-2026", time))
            assertEquals(3, MercadillosValidator.estadoPlanificado(LocalDate.of(2026,11,27), null, time))
            assertEquals(3, MercadillosValidator.estadoPlanificado(LocalDate.of(2026,11,27), 10.0, time))
        }
    }

    @Test fun futuroConYSinSaldo() {
        val date = LocalDate.of(2026,11,28)
        assertTrue(MercadillosValidator.puedeCrearConFecha("28-11-2026", now))
        assertEquals(1, MercadillosValidator.estadoPlanificado(date, null, now))
        assertEquals(2, MercadillosValidator.estadoPlanificado(date, 0.0, now))
    }

    @Test fun pasadoEInvalidoNoPermitidos() {
        assertFalse(MercadillosValidator.puedeCrearConFecha("26-11-2026", now))
        assertFalse(MercadillosValidator.puedeCrearConFecha("31-02-2027", now))
        assertFalse(MercadillosValidator.puedeCrearConFecha("27-11-2026", Instant.parse("2026-11-27T23:00:00Z")))
        assertTrue(MercadillosValidator.puedeCrearConFecha("2026-11-27", now))
    }
}
