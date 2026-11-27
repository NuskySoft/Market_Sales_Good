package es.nuskysoftware.marketsales.ui.viewmodel

// Responsabilidad: Construye el ViewModel de gastos con sus dependencias.

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.nuskysoftware.marketsales.data.repository.GastosRepository

class GastosViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    // Construye el ViewModel con las dependencias del flujo.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GastosViewModel::class.java))
        val repo = GastosRepository(context.applicationContext)
        return GastosViewModel(repo) as T
    }
}
