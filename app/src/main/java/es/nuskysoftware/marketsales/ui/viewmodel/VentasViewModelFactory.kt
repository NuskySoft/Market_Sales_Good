package es.nuskysoftware.marketsales.ui.viewmodel

// Responsabilidad: Construye el ViewModel de ventas con sus dependencias.

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.nuskysoftware.marketsales.data.repository.ArticuloRepository
import es.nuskysoftware.marketsales.data.repository.VentasRepository
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel

class VentasViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    // Construye el ViewModel con las dependencias del flujo.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VentasViewModel::class.java)) {
            val ventasRepository = VentasRepository(context)
            val articuloRepository = ArticuloRepository(context)
            return VentasViewModel(ventasRepository, articuloRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}