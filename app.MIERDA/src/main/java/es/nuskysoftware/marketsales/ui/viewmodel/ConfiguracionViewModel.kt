// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/ConfiguracionViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity
import es.nuskysoftware.marketsales.data.repository.ConfiguracionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConfiguracionViewModel(
    private val repository: ConfiguracionRepository
) : ViewModel() {

    val configuracion: StateFlow<ConfiguracionEntity?> = repository.configuracion

    init {
        // Crear configuración por defecto si no existe
        viewModelScope.launch {
            repository.crearConfiguracionPorDefecto()
        }
    }

    /**
     * Actualiza la contraseña del usuario
     */
    fun actualizarPassword(nuevaPassword: String) {
        viewModelScope.launch {
            repository.actualizarPassword(nuevaPassword)
        }
    }

    /**
     * Actualiza el tema (claro/oscuro)
     */
    fun actualizarTema(esTemaOscuro: Boolean) {
        viewModelScope.launch {
            repository.actualizarTema(esTemaOscuro)
        }
    }

    /**
     * Actualiza la fuente de la aplicación
     */
    fun actualizarFuente(fuente: String) {
        viewModelScope.launch {
            repository.actualizarFuente(fuente)
        }
    }

    /**
     * Actualiza el idioma de la aplicación
     */
    fun actualizarIdioma(idioma: String) {
        viewModelScope.launch {
            repository.actualizarIdioma(idioma)
        }
    }

    /**
     * Actualiza la moneda de la aplicación
     */
    fun actualizarMoneda(moneda: String) {
        viewModelScope.launch {
            repository.actualizarMoneda(moneda)
        }
    }

    /**
     * Actualiza la versión de la app (0=FREE, 1=PREMIUM)
     */
    fun actualizarVersion(version: Int) {
        viewModelScope.launch {
            repository.actualizarVersionApp(version)
        }
    }

    /**
     * Actualiza el email del usuario
     */
    fun actualizarEmail(email: String) {
        viewModelScope.launch {
            repository.actualizarUsuarioEmail(email)
        }
    }

    /**
     * Fuerza la sincronización con Firebase
     */
    fun sincronizar() {
        viewModelScope.launch {
            repository.sincronizar()
        }
    }
}