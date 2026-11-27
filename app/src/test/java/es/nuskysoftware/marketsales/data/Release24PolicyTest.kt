package es.nuskysoftware.marketsales.data
import es.nuskysoftware.marketsales.utils.CalendarDates
import es.nuskysoftware.marketsales.utils.Money
import org.junit.Assert.*
import org.junit.Test
class Release24PolicyTest {
    @Test fun monetaryPrecision() {
        assertEquals(0.30, Money.sum(listOf(0.1, 0.2)), 0.0)
        assertEquals(1.24, Money.round(1.235), 0.0)
        assertEquals(-1.24, Money.round(-1.235), 0.0)
        assertEquals(0.87, Money.times(0.29, 3), 0.0)
        assertFalse(Money.validInput("1,234"))
        assertTrue(Money.validInput("1,23"))
    }
    @Test fun calendarDatesAcrossYears() {
        val november = CalendarDates.epochDay("30-11-2026")!!
        val december = CalendarDates.epochDay("02-12-2026")!!
        assertTrue(november < december)
        assertEquals("02-12-2026", CalendarDates.display(december))
        assertNull(CalendarDates.epochDay("31-02-2026"))
    }
}
