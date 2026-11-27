package es.nuskysoftware.marketsales.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountPlanTest {
    private fun checkSession(block: () -> Unit) {
        val owner = ConfigurationManager.getCurrentUserId()
        val premium = ConfigurationManager.getIsPremium()
        val authenticated = ConfigurationManager.isAuthenticated.value
        try { block() } finally {
            ConfigurationManager.setUsuarioLogueado(owner)
            if (owner != null) ConfigurationManager.applyAccountFromRoom(owner, premium)
            ConfigurationManager.updateUserConfiguration(usuarioId = owner, isAuthenticated = authenticated, planUsuario = if (premium) "PREMIUM" else "FREE")
        }
    }
    @Test fun committedPremiumSurvivesFreeFallback() = checkSession {
        ConfigurationManager.setUsuarioLogueado("account-a")
        ConfigurationManager.applyAccountFromRoom("account-a", true)
        ConfigurationManager.updateUserConfiguration(usuarioId = "account-a", isAuthenticated = true, planUsuario = "FREE")
        ConfigurationManager.updateUserPremium(false)
        assertTrue(ConfigurationManager.getIsPremium())
        assertTrue(ConfigurationManager.esPremiumReady.value)
    }
    @Test fun accountSwitchNeverReusesAnotherOwnersPlan() = checkSession {
        ConfigurationManager.setUsuarioLogueado("account-a")
        ConfigurationManager.applyAccountFromRoom("account-a", true)
        ConfigurationManager.setUsuarioLogueado("account-b")
        ConfigurationManager.applyAccountFromRoom("account-a", true)
        assertFalse(ConfigurationManager.getIsPremium())
        assertFalse(ConfigurationManager.esPremiumReady.value)
        ConfigurationManager.applyAccountFromRoom("account-b", false)
        assertFalse(ConfigurationManager.getIsPremium())
        assertTrue(ConfigurationManager.esPremiumReady.value)
    }
    @Test fun newerCommittedLocalPlanCanBecomeFree() = checkSession {
        ConfigurationManager.setUsuarioLogueado("account-a")
        ConfigurationManager.applyAccountFromRoom("account-a", true)
        ConfigurationManager.applyAccountFromRoom("account-a", false)
        assertFalse(ConfigurationManager.getIsPremium())
    }
}
