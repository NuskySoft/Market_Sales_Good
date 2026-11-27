package es.nuskysoftware.marketsales.ui.viewmodel

// Responsabilidad: Expone los datos de empresa y coordina el guardado desde el formulario.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.repository.EmpresaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmpresaViewModel(private val repository: EmpresaRepository) : ViewModel() {

    val empresa = repository.getEmpresaFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun guardarOActualizar(
        nif: String,
        nombre: String,
        razon: String,
        direccion: String,
        poblacion: String,
        codigoPostal: String,
        provincia: String,
        pais: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.guardarOActualizar(nif, nombre, razon, direccion, poblacion, codigoPostal, provincia, pais)
                onComplete()
            } catch (_: Exception) { es.nuskysoftware.marketsales.utils.DemoAccess.show("local_save_failed") }
        }
    }
}
