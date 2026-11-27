// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/ArticuloViewModelFactory.kt
package es.nuskysoftware.marketsales.ui.viewmodel

// Responsabilidad: Construye el ViewModel de artículos con las dependencias necesarias.

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.nuskysoftware.marketsales.data.repository.ArticuloRepository

/**
 * Factory para ArticuloViewModel que inyecta ArticuloRepository con Context
 * ✅ FINAL: Context requerido para ArticuloRepository (AppDatabase + ConnectivityObserver)
 */
class ArticuloViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    // Construye el ViewModel con las dependencias del flujo.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticuloViewModel::class.java)) {
            return ArticuloViewModel(ArticuloRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}