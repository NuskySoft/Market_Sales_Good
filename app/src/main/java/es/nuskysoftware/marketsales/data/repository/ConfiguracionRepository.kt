// app/src/main/java/es/nuskysoftware/marketsales/data/repository/ConfiguracionRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.ConfiguracionDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * ConfiguracionRepository V10 - SIMPLIFICADO PARA SISTEMA MONOUSUARIO
 *
 * CAMBIOS V10:
 * - Métodos actualizados para nueva estructura ConfiguracionEntity
 * - Agregados métodos para usuarioLogueado
 * - Sincronización con nuevos campos version/lastModified
 * - Eliminados métodos obsoletos (usuarioPassword, versionApp, etc.)
 */
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

    // ========== MÉTODOS PRINCIPALES V10 ==========

    /**
     * Obtiene la configuración actual
     */
    suspend fun getConfiguracion(): ConfiguracionEntity? = withContext(Dispatchers.IO) {
        configuracionDao.getConfiguracionSync()
    }

    /**
     * Crea configuración por defecto si no existe
     */
    suspend fun crearConfiguracionPorDefecto() = withContext(Dispatchers.IO) {
        val existente = configuracionDao.getConfiguracion()
        if (existente == null) {
            val porDefecto = ConfiguracionEntity(
                moneda = ConfigurationManager.getMoneda(),
                usuarioLogueado = "usuario_default",
                ultimoDispositivo = android.os.Build.MODEL,
                fechaUltimaSync = dateFormat.format(Date())
            )
            configuracionDao.insertOrUpdate(porDefecto)
            sincronizarConFirebase(porDefecto)
        } else {
            if (existente.fechaUltimaSync == null || existente.pendienteSync) {
                sincronizarConFirebase(existente)
            }
        }
    }

    // ========== CONFIGURACIÓN GLOBAL V10 ==========

    /**
     * Actualiza el idioma (común a todos los usuarios)
     */
    suspend fun updateIdioma(idioma: String) = withContext(Dispatchers.IO) {
        configuracionDao.updateIdioma(idioma)
        sincronizarCampoEspecifico("idioma", idioma)
    }

    /**
     * Actualiza el tema oscuro (común a todos los usuarios)
     */
    suspend fun updateTemaOscuro(esTemaOscuro: Boolean) = withContext(Dispatchers.IO) {
        configuracionDao.updateTemaOscuro(esTemaOscuro)
        sincronizarCampoEspecifico("temaOscuro", esTemaOscuro)
    }

    /**
     * Actualiza la fuente (común a todos los usuarios)
     */
    suspend fun updateFuente(fuente: String) = withContext(Dispatchers.IO) {
        configuracionDao.updateFuente(fuente)
        sincronizarCampoEspecifico("fuente", fuente)
    }

    /**
     * Actualiza la moneda (común a todos los usuarios)
     */
    suspend fun updateMoneda(moneda: String) = withContext(Dispatchers.IO) {
        configuracionDao.updateMoneda(moneda)
        sincronizarCampoEspecifico("moneda", moneda)
    }

    // ========== USUARIO LOGUEADO V10 ==========

    /**
     * Establece el usuario actualmente logueado
     */
    suspend fun setUsuarioLogueado(usuarioUid: String) = withContext(Dispatchers.IO) {
        configuracionDao.setUsuarioLogueado(usuarioUid)
        // No sincronizar este campo con Firebase (es solo local)
    }

    /**
     * Obtiene el usuario actualmente logueado
     */
    suspend fun getUsuarioLogueado(): String? = withContext(Dispatchers.IO) {
        configuracionDao.getUsuarioLogueado()
    }

    /**
     * Verifica si es usuario por defecto
     */
    suspend fun isUsuarioDefault(): Boolean = withContext(Dispatchers.IO) {
        configuracionDao.isUsuarioDefault()
    }

    // ========== SINCRONIZACIÓN V10 ==========

    /**
     * Sincroniza la configuración completa
     */
    suspend fun sincronizar() = withContext(Dispatchers.IO) {
        val cfg = configuracionDao.getConfiguracionSync() ?: return@withContext
        sincronizarConFirebase(cfg)
    }

    /**
     * Sincroniza la configuración completa con Firebase
     */
    private fun sincronizarConFirebase(config: ConfiguracionEntity) {
        try {
            val datos = mapOf(
                "idioma" to config.idioma,
                "temaOscuro" to config.temaOscuro,
                "fuente" to config.fuente,
                "moneda" to config.moneda,
                "numeroVersion" to config.numeroVersion,
                "ultimoDispositivo" to config.ultimoDispositivo,
                "fechaUltimaSync" to dateFormat.format(Date()),
                "version" to config.version,
                "lastModified" to System.currentTimeMillis(),
                "dispositivo" to android.os.Build.MODEL
            )

            // Usar "usuario_default" como documento para configuración global
            val docRef = firestore.collection("configuraciones")
                .document("configuracion_global")

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
     * Sincroniza un campo específico con Firebase
     */
    private suspend fun sincronizarCampoEspecifico(campo: String, valor: Any) {
        try {
            val datos = mapOf(
                campo to valor,
                "fechaUltimaSync" to dateFormat.format(Date()),
                "lastModified" to System.currentTimeMillis()
            )

            firestore.collection("configuraciones")
                .document("configuracion_global")
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
        } catch (e: Exception) {
            repositoryScope.launch {
                configuracionDao.updatePendienteSync(true)
            }
        }
    }
    suspend fun getHybridConfiguracion(): ConfiguracionEntity? = withContext(Dispatchers.IO) {
        try {
            // 1. Verificar si hay cambios pendientes en Room
            val roomConfig = configuracionDao.getConfiguracionSync()
            val hasPendingChanges = roomConfig?.pendienteSync == true

            if (hasPendingChanges) {
                // ✅ HAY CAMBIOS PENDIENTES → Leer de Room (fuente de verdad)
                Log.d("ConfiguracionRepository", "📱 Leyendo configuración de ROOM (cambios pendientes)")
                return@withContext roomConfig
            } else {
                // ✅ NO HAY CAMBIOS PENDIENTES → Leer de Firebase (más actualizado)
                Log.d("ConfiguracionRepository", "☁️ Leyendo configuración de FIREBASE (sin cambios pendientes)")

                if (connectivityObserver.isConnected.first()) {
                    try {
                        val firebaseDoc = firestore.collection("configuraciones")
                            .document("configuracion_global")
                            .get()
                            .await()

                        if (firebaseDoc.exists()) {
                            val firebaseData = firebaseDoc.data!!

                            // Crear entidad híbrida: Firebase + datos locales importantes
                            val hybridConfig = roomConfig?.copy(
                                // Datos de Firebase (comunes)
                                moneda = firebaseData["moneda"] as? String ?: roomConfig.moneda,
                                idioma = firebaseData["idioma"] as? String ?: roomConfig.idioma,
                                fuente = firebaseData["fuente"] as? String ?: roomConfig.fuente,
                                temaOscuro = firebaseData["temaOscuro"] as? Boolean ?: roomConfig.temaOscuro,

                                // Datos locales (específicos del dispositivo)
                                usuarioLogueado = roomConfig.usuarioLogueado, // Mantener local
                                pendienteSync = false // Marcar como sincronizado
                            ) ?: ConfiguracionEntity(
                               // moneda = firebaseData["moneda"] as? String ?: "€ Euro",
                                moneda = firebaseData["moneda"] as? String ?: ConfigurationManager.getMoneda(),
                                idioma = firebaseData["idioma"] as? String ?: "es",
                                fuente = firebaseData["fuente"] as? String ?: "Montserrat",
                                temaOscuro = firebaseData["temaOscuro"] as? Boolean ?: false
                            )

                            // Actualizar Room con datos frescos
                            configuracionDao.insertOrUpdate(hybridConfig)

                            Log.d("ConfiguracionRepository", "✅ Configuración fresca de Firebase aplicada")
                            return@withContext hybridConfig
                        }
                    } catch (e: Exception) {
                        Log.w("ConfiguracionRepository", "❌ Error leyendo Firebase, usando Room", e)
                    }
                }

                // Fallback: usar Room
                //return@withContext roomConfig ?: ConfiguracionEntity()
                return@withContext roomConfig ?: ConfiguracionEntity(moneda = ConfigurationManager.getMoneda())
            }

        } catch (e: Exception) {
            Log.e("ConfiguracionRepository", "Error en estrategia híbrida", e)
            // En caso de error, devolver lo que tengamos en Room o crear configuración por defecto
            val fallbackConfig = configuracionDao.getConfiguracionSync()
            //return@withContext fallbackConfig ?: ConfiguracionEntity()
            return@withContext fallbackConfig ?: ConfiguracionEntity(moneda = ConfigurationManager.getMoneda())
        }
    }
    // ========== MÉTODOS OBSOLETOS V10 (compatibilidad) ==========

    @Deprecated("Usar updateIdioma()", ReplaceWith("updateIdioma(idioma)"))
    suspend fun actualizarIdioma(idioma: String) = updateIdioma(idioma)

    @Deprecated("Usar updateTemaOscuro()", ReplaceWith("updateTemaOscuro(esTemaOscuro)"))
    suspend fun actualizarTema(esTemaOscuro: Boolean) = updateTemaOscuro(esTemaOscuro)

    @Deprecated("Usar updateFuente()", ReplaceWith("updateFuente(fuente)"))
    suspend fun actualizarFuente(fuente: String) = updateFuente(fuente)

    @Deprecated("Usar updateMoneda()", ReplaceWith("updateMoneda(moneda)"))
    suspend fun actualizarMoneda(moneda: String) = updateMoneda(moneda)

    @Deprecated("Campo eliminado en V10 - usar UserRepository", ReplaceWith("UserRepository.updateUserPassword()"))
    suspend fun actualizarPassword(password: String) {
        // No-op en V10
    }

    @Deprecated("Campo eliminado en V10 - usar UserRepository", ReplaceWith("UserRepository.updateUserPremium()"))
    suspend fun actualizarVersionApp(version: Int) {
        // No-op en V10
    }

    @Deprecated("Campo eliminado en V10 - usar UserRepository", ReplaceWith("UserRepository.updateUserEmail()"))
    suspend fun actualizarUsuarioEmail(email: String) {
        // No-op en V10
    }
}
