package es.nuskysoftware.marketsales.utils

import org.junit.Assert.*
import org.junit.Test

class AppModePolicyTest {
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
    }
}
