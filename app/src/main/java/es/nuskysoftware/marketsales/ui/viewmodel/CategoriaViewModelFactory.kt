// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/CategoriaViewModelFactory.kt
package es.nuskysoftware.marketsales.ui.viewmodel

// Responsabilidad: Construye el ViewModel de categorías con el repositorio correspondiente.

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.nuskysoftware.marketsales.data.repository.CategoriaRepository

/**
 * Factory para CategoriaViewModel que inyecta CategoriaRepository con Context
 * ✅ FINAL: Context requerido para CategoriaRepository (AppDatabase + ConnectivityObserver)
 */
class CategoriaViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    // Construye el ViewModel con las dependencias del flujo.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriaViewModel::class.java)) {
            return CategoriaViewModel(CategoriaRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}