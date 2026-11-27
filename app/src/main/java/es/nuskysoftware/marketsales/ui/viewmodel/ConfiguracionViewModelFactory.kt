package es.nuskysoftware.marketsales.ui.viewmodel

// Responsabilidad: Construye el ViewModel de configuración con el repositorio correspondiente.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.nuskysoftware.marketsales.data.repository.ConfiguracionRepository

class ConfiguracionViewModelFactory(
    private val repository: ConfiguracionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    // Construye el ViewModel con las dependencias del flujo.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConfiguracionViewModel::class.java)) {
            return ConfiguracionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}