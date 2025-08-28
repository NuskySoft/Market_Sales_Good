package es.nuskysoftware.marketsales.ui.viewmodel

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
            repository.guardarOActualizar(nif, nombre, razon, direccion, poblacion, codigoPostal, provincia, pais)
            onComplete()
        }
    }
}