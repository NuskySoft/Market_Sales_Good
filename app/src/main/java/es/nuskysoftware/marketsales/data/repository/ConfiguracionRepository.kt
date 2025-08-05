// app/src/main/java/es/nuskysoftware/marketsales/data/repository/ConfiguracionRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.ConfiguracionDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*

class ConfiguracionRepository(
    context: Context
) {
    private val configuracionDao: ConfiguracionDao = AppDatabase.getDatabase(context).configuracionDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Observador de red
    private val connectivityObserver = ConnectivityObserver(context)

    // Exponer configuración como StateFlow
    val configuracion = configuracionDao.getConfiguracionFlow()
        .stateIn(repositoryScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Cuando volvemos a estar online, reintentamos enviar pendientes
        repositoryScope.launch {
            connectivityObserver.isConnected.collect { online ->
                if (online) {
                    val cfg = configuracionDao.getConfiguracionSync()
                    if (cfg?.pendienteSync == true) {
                        sincronizarConFirebase(cfg)
                    }
                }
            }
        }
    }

    suspend fun crearConfiguracionPorDefecto() = withContext(Dispatchers.IO) {
        val existente = configuracionDao.getConfiguracion()
        if (existente == null) {
            val porDefecto = ConfiguracionEntity(
                numeroVersion = "V1.0",
                ultimoDispositivo = android.os.Build.MODEL,
                usuarioId = "usuario_default",
                fechaUltimaSync = dateFormat.format(Date()),
                pendienteSync = true
            )
            configuracionDao.insertOrUpdate(porDefecto)
            sincronizarConFirebase(porDefecto)
        } else {
            if (existente.fechaUltimaSync == null || existente.pendienteSync) {
                sincronizarConFirebase(existente)
            }
        }
    }

    suspend fun actualizarIdioma(idioma: String) = withContext(Dispatchers.IO) {
        configuracionDao.updateIdioma(idioma)
        configuracionDao.updatePendienteSync(true)
        sincronizarCampoEspecifico("idioma", idioma)
    }

    suspend fun actualizarTema(esTemaOscuro: Boolean) = withContext(Dispatchers.IO) {
        configuracionDao.updateTemaOscuro(esTemaOscuro)
        configuracionDao.updatePendienteSync(true)
        sincronizarCampoEspecifico("temaOscuro", esTemaOscuro)
    }

    suspend fun actualizarFuente(fuente: String) = withContext(Dispatchers.IO) {
        configuracionDao.updateFuente(fuente)
        configuracionDao.updatePendienteSync(true)
        sincronizarCampoEspecifico("fuente", fuente)
    }

    suspend fun actualizarPassword(password: String) = withContext(Dispatchers.IO) {
        configuracionDao.updateUsuarioPassword(password)
        configuracionDao.updatePendienteSync(true)
        sincronizarCampoEspecifico("usuarioPassword", password)
    }

    suspend fun actualizarMoneda(moneda: String) = withContext(Dispatchers.IO) {
        configuracionDao.updateMoneda(moneda)
        configuracionDao.updatePendienteSync(true)
        sincronizarCampoEspecifico("moneda", moneda)
    }

    suspend fun actualizarVersionApp(version: Int) = withContext(Dispatchers.IO) {
        configuracionDao.updateVersionApp(version)
        configuracionDao.updatePendienteSync(true)
        sincronizarCampoEspecifico("versionApp", version)
    }

    suspend fun actualizarUsuarioEmail(email: String) = withContext(Dispatchers.IO) {
        configuracionDao.updateUsuarioEmail(email)
        configuracionDao.updatePendienteSync(true)
        sincronizarCampoEspecifico("usuarioEmail", email)
    }

    suspend fun sincronizar() = withContext(Dispatchers.IO) {
        val cfg = configuracionDao.getConfiguracionSync() ?: return@withContext
        sincronizarConFirebase(cfg)
    }

    /**
     * Sincroniza la configuración completa con Firebase.
     * Marca o limpia pendienteSync según el resultado.
     */
    private fun sincronizarConFirebase(config: ConfiguracionEntity) {
        try {
            val datos = mapOf(
                "versionApp"      to config.versionApp,
                "numeroVersion"   to config.numeroVersion,
                "ultimoDispositivo" to config.ultimoDispositivo,
                "usuarioEmail"    to config.usuarioEmail,
                "usuarioId"       to config.usuarioId,
                "usuarioPassword" to config.usuarioPassword,
                "idioma"          to config.idioma,
                "temaOscuro"      to config.temaOscuro,
                "fuente"          to config.fuente,
                "moneda"          to config.moneda,
                "fechaUltimaSync" to dateFormat.format(Date()),
                "dispositivo"     to android.os.Build.MODEL
            )
            val docRef = firestore.collection("configuraciones")
                .document(config.usuarioId ?: "usuario_default")

            docRef.set(datos)
                .addOnSuccessListener {
                    repositoryScope.launch {
                        configuracionDao.updateFechaUltimaSync(dateFormat.format(Date()))
                        configuracionDao.updatePendienteSync(false)
                    }
                }
                .addOnFailureListener {
                    repositoryScope.launch {
                        configuracionDao.updatePendienteSync(true)
                    }
                }
        } catch (e: Exception) {
            repositoryScope.launch {
                configuracionDao.updatePendienteSync(true)
            }
        }
    }

    /**
     * Sincroniza un solo campo con Firebase.
     * Limpia pendienteSync sólo al éxito global.
     */
    private suspend fun sincronizarCampoEspecifico(campo: String, valor: Any) {
        try {
            val cfg = configuracionDao.getConfiguracionSync() ?: return
            val documentoId = cfg.usuarioId ?: "usuario_default"
            val datos = mapOf(campo to valor, "fechaUltimaSync" to dateFormat.format(Date()))
            firestore.collection("configuraciones")
                .document(documentoId)
                .update(datos)
                .addOnSuccessListener {
                    repositoryScope.launch {
                        configuracionDao.updateFechaUltimaSync(dateFormat.format(Date()))
                        configuracionDao.updatePendienteSync(false)
                    }
                }
                .addOnFailureListener {
                    repositoryScope.launch {
                        configuracionDao.updatePendienteSync(true)
                    }
                }
        } catch (_: Exception) {
            repositoryScope.launch {
                configuracionDao.updatePendienteSync(true)
            }
        }
    }
}

