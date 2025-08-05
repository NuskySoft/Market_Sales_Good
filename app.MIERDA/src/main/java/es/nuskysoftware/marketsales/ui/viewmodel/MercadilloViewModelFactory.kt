// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/MercadilloViewModelFactory.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository

/**
 * Factory para MercadilloViewModel que inyecta MercadilloRepository con Context
 * ✅ FINAL: Context requerido para MercadilloRepository (AppDatabase + ConnectivityObserver)
 */

// pseudo-código del factory
class MercadilloViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = MercadilloRepository(context.applicationContext)
        return MercadilloViewModel(repo, context.applicationContext) as T
    }
}


