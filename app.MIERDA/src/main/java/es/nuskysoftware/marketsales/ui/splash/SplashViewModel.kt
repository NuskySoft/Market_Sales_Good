// app/src/main/java/es/nuskysoftware/marketsales/ui/splash/SplashViewModel.kt
package es.nuskysoftware.marketsales.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.data.repository.VentasRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class SplashViewModel(app: Application) : AndroidViewModel(app) {

    sealed class UiState {
        data object Idle : UiState()
        data class Progreso(val mensaje: String) : UiState()
        data object Listo : UiState()
        data class Error(val mensaje: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val db = AppDatabase.getDatabase(app)
    private val recibosDao = db.recibosDao()
    private val lineasDao = db.lineasVentaDao()
    private val ventasRepo = VentasRepository(app.applicationContext)
    private val mercadilloRepo = MercadilloRepository(app.applicationContext)
    private val fs = FirebaseFirestore.getInstance()

    fun arrancar() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Progreso("Cargando configuración…")
                val uid = ConfigurationManager.getCurrentUserId().orEmpty()

                if (uid.isBlank()) {
                    _uiState.value = UiState.Error("No hay usuario autenticado")
                    return@launch
                }

                // ¿Room vacío para este usuario? (heurística con Recibos)
                _uiState.value = UiState.Progreso("Comprobando datos locales…")
                val hayRecibosLocales = withContext(Dispatchers.IO) {
                    recibosDao.observarRecibosUsuario(uid).first().isNotEmpty()
                }

                if (!hayRecibosLocales) {
                    // Cold start: importar ventas completas desde Firebase → Room
                    _uiState.value = UiState.Progreso("Descargando ventas del usuario…")
                    importarVentasDesdeFirebase(uid)
                } else {
                    // Normal: subir pendientes + pull selectivo de ventas
                    _uiState.value = UiState.Progreso("Sincronizando cambios pendientes…")
                    ventasRepo.pushPendientes(uid)
                    _uiState.value = UiState.Progreso("Actualizando últimos datos…")
                    ventasRepo.pullSelective(uid)
                }

                // Recalcular estados una sola vez
                _uiState.value = UiState.Progreso("Actualizando estados…")
                mercadilloRepo.actualizarEstadosAutomaticos(uid)

                _uiState.value = UiState.Listo
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private suspend fun importarVentasDesdeFirebase(uid: String) = withContext(Dispatchers.IO) {
        // Recibos del usuario
        val recibosSnap = fs.collection("recibos")
            .whereEqualTo("idUsuario", uid)
            .get().await()

        val recibos = recibosSnap.documents.mapNotNull { d ->
            try {
                ReciboEntity(
                    idRecibo = d.getString("idRecibo") ?: return@mapNotNull null,
                    idMercadillo = d.getString("idMercadillo") ?: "",
                    idUsuario = d.getString("idUsuario") ?: uid,
                    fechaHora = d.getLong("fechaHora") ?: System.currentTimeMillis(),
                    metodoPago = d.getString("metodoPago") ?: "DESCONOCIDO",
                    totalTicket = (d.getDouble("totalTicket") ?: 0.0),
                    estado = d.getString("estado") ?: "COMPLETADO",
                    sincronizadoFirebase = true,
                    version = d.getLong("version") ?: 1L,
                    lastModified = d.getLong("lastModified") ?: System.currentTimeMillis(),
                    syncError = null
                )
            } catch (_: Exception) { null }
        }

        // Upsert de recibos en Room
        if (recibos.isNotEmpty()) {
            recibosDao.upsertAll(recibos)
        }

        // Para cada recibo, descargar líneas
        val todasLineas = mutableListOf<LineaVentaEntity>()
        for (r in recibos) {
            val lineasSnap = fs.collection("lineas_venta")
                .whereEqualTo("idRecibo", r.idRecibo)
                .get().await()

            val lineas = lineasSnap.documents.mapNotNull { d ->
                try {
                    LineaVentaEntity(
                        idLinea = d.getString("idLinea") ?: return@mapNotNull null,
                        idRecibo = r.idRecibo,
                        idMercadillo = d.getString("idMercadillo") ?: r.idMercadillo,
                        idUsuario = d.getString("idUsuario") ?: uid,
                        numeroLinea = (d.getLong("numeroLinea") ?: 0L).toInt(),
                        tipoLinea = d.getString("tipoLinea") ?: "PRODUCTO",
                        descripcion = d.getString("descripcion") ?: "",
                        idProducto = d.getString("idProducto"),
                        cantidad = (d.getLong("cantidad") ?: 0L).toInt(),
                        precioUnitario = d.getDouble("precioUnitario") ?: 0.0,
                        subtotal = d.getDouble("subtotal") ?: 0.0,
                        idLineaOriginalAbonada = d.getString("idLineaOriginalAbonada"),
                        sincronizadoFirebase = true,
                        version = d.getLong("version") ?: 1L,
                        lastModified = d.getLong("lastModified") ?: System.currentTimeMillis(),
                        syncError = null
                    )
                } catch (_: Exception) { null }
            }
            todasLineas += lineas
        }

        if (todasLineas.isNotEmpty()) {
            lineasDao.upsertAll(todasLineas)
        }
    }
}
