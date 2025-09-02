// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/ProximosMercadillosViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProximosMercadillosViewModel(
    private val repository: MercadilloRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val error: String? = null,
        val items: List<MercadilloEntity> = emptyList()
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    // Helpers de i18n
    private val lang get() = ConfigurationManager.idioma.value
    private fun tr(key: String) = StringResourceManager.getString(key, lang)
    private fun tr1(key: String, p0: String) = tr(key).replace("{0}", p0)

    fun cargar() {
        viewModelScope.launch {
            _ui.value = UiState(loading = true)
            try {
                val userId = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
                val lista = withContext(Dispatchers.IO) {
                    repository.getMercadillosDesdeHoy(userId)
                }
                    // 🚫 fuera los EN_CURSO (3)
                    .filter { it.estado != EstadosMercadillo.Estado.EN_CURSO.codigo }
                    .sortedWith(compareBy<MercadilloEntity> { it.fecha }.thenBy { it.horaInicio })

                _ui.value = UiState(loading = false, items = lista)
            } catch (e: Exception) {
                _ui.value = UiState(
                    loading = false,
                    error = tr1("error_cargando_proximos_mercadillos", e.message ?: "—")
                )
            }
        }
    }
}

class ProximosMercadillosViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProximosMercadillosViewModel(
            repository = es.nuskysoftware.marketsales.data.repository.MercadilloRepository(context)
        ) as T
    }
}
