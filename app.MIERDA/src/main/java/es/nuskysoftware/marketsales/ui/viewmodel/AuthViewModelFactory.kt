// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/AuthViewModelFactory.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.nuskysoftware.marketsales.data.repository.AuthRepository
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository

/**
 * Factory para AuthViewModel que inyecta:
 *  - AuthRepository
 *  - MercadilloRepository (para recalcular estados tras sync)
 */
class AuthViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val appContext = context.applicationContext
            val authRepository = AuthRepository(appContext)
            val mercadilloRepository = MercadilloRepository(appContext)
            return AuthViewModel(
                authRepository = authRepository,
                mercadilloRepository = mercadilloRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

