package es.nuskysoftware.marketsales.utils

// Finalidad de las pruebas: Prueba las modalidades y restricciones de acceso definidas por las reglas de negocio.

import org.junit.Assert.*
import org.junit.Test

class AppModePolicyTest {
    // Comprueba unidades, no número de referencias ni tickets; manual y abonos no consumen cupo.
    @Test fun freeCatalogueStopsAtTenUnitsButManualSalesRemainAvailable() {
        assertTrue(AppModePolicy.canUseCatalog(AppMode.FREE, 0, 10))
        assertTrue(AppModePolicy.canUseCatalog(AppMode.FREE, 9, 1))
        assertFalse(AppModePolicy.canUseCatalog(AppMode.FREE, 9, 2))
        assertFalse(AppModePolicy.canUseCatalog(AppMode.FREE, 10, 1))
        assertTrue(AppModePolicy.canUseCatalog(AppMode.FREE, 12, 0))
        assertTrue(AppModePolicy.canUseCatalog(AppMode.PREMIUM, 100, 100))
        assertFalse(AppModePolicy.canUseCatalog(AppMode.DEMO, 0, 1))
        assertEquals(0L, AppModePolicy.catalogRemaining(15))
    }

    @Test fun registrationAndPremiumAreIndependent() {
        assertEquals(AppMode.DEMO, AppModePolicy.mode(false, true))
        assertEquals(AppMode.FREE, AppModePolicy.mode(true, false))
        assertEquals(AppMode.PREMIUM, AppModePolicy.mode(true, true))
    }
    @Test fun demoCannotReachCloudEvenDuringSessionTransitions() {
        for (uid in listOf(null, "", "usuario_default", AppModePolicy.DEMO_USER_ID)) {
            assertFalse(AppModePolicy.canSync(uid, uid))
            assertFalse(AppModePolicy.canSync(uid, "real-account"))
        }
        assertTrue(AppModePolicy.canSync("alice", "alice"))
        assertFalse(AppModePolicy.canSync("alice", "bob"))
        assertFalse(AppModePolicy.canSync("alice", null))
    }
    @Test fun tenthTicketIsAllowedButEleventhIsNot() {
        assertNull(AppModePolicy.limitKey(AppMode.DEMO, "ventas", 9))
        assertEquals("demo_limite_ventas", AppModePolicy.limitKey(AppMode.DEMO, "ventas", 10))
        assertNull(AppModePolicy.limitKey(AppMode.FREE, "ventas", 1000))
        assertNull(AppModePolicy.limitKey(AppMode.PREMIUM, "ventas", 1000))
    }
    @Test fun singleItemCapsAreDemoOnly() {
        for (resource in listOf("mercadillos", "categorias", "articulos")) {
            assertNull(AppModePolicy.limitKey(AppMode.DEMO, resource, 0))
            assertNotNull(AppModePolicy.limitKey(AppMode.DEMO, resource, 1))
            assertNull(AppModePolicy.limitKey(AppMode.FREE, resource, 1))
        }
    }
    @Test fun salesStateExceptionDoesNotLeakIntoRegisteredModes() {
        for (stateAllows in listOf(true, false)) assertTrue(AppModePolicy.canSell(AppMode.DEMO, stateAllows))
        assertFalse(AppModePolicy.canSell(AppMode.FREE, false))
        assertFalse(AppModePolicy.canSell(AppMode.PREMIUM, false))
    }
    @Test fun demoRefundAndBalanceAllocationAreForbidden() {
        assertFalse(AppModePolicy.canRefund(AppMode.DEMO))
        assertFalse(AppModePolicy.canAssignBalance(AppMode.DEMO))
        assertTrue(AppModePolicy.canRefund(AppMode.FREE))
        assertTrue(AppModePolicy.canRefund(AppMode.PREMIUM))
        assertFalse(AppModePolicy.canAssignBalance(AppMode.FREE))
        assertTrue(AppModePolicy.canAssignBalance(AppMode.PREMIUM))
    }
}
