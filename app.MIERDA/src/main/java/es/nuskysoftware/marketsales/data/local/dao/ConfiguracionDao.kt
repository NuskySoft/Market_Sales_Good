// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/ConfiguracionDao.kt
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity

/**
 * ConfiguracionDao V10 - ACTUALIZADO PARA SISTEMA MONOUSUARIO
 *
 * CAMBIOS V10:
 * - Agregados métodos para usuarioLogueado
 * - Agregados métodos para sincronización (version, lastModified)
 * - Eliminados métodos obsoletos (usuarioPassword, usuarioEmail, usuarioId, versionApp)
 * - Mantenidos métodos legacy marcados como @Deprecated
 */
@Dao
interface ConfiguracionDao {

    // ========== MÉTODOS BÁSICOS (sin cambios) ==========
    @Query("SELECT EXISTS(SELECT 1 FROM configuracion WHERE id = 1)")
    suspend fun existeConfiguracion(): Boolean

    @Query("SELECT * FROM configuracion WHERE id = 1")
    suspend fun getConfiguracion(): ConfiguracionEntity?

    @Query("SELECT * FROM configuracion WHERE id = 1")
    suspend fun getConfiguracionSync(): ConfiguracionEntity?

    @Query("SELECT * FROM configuracion WHERE id = 1")
    fun getConfiguracionFlow(): Flow<ConfiguracionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarConfiguracion(configuracion: ConfiguracionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(configuracion: ConfiguracionEntity)

    @Update
    suspend fun actualizarConfiguracion(configuracion: ConfiguracionEntity)

    @Query("DELETE FROM configuracion")
    suspend fun eliminarTodasLasConfiguraciones()

    // ========== CONFIGURACIÓN GLOBAL V10 ==========

    /** Actualiza la moneda (común a todos los usuarios) */
    @Query("UPDATE configuracion SET moneda = :moneda, version = version + 1, lastModified = :timestamp, pendienteSync = 1 WHERE id = 1")
    suspend fun updateMoneda(moneda: String, timestamp: Long = System.currentTimeMillis())

    /** Actualiza el idioma (común a todos los usuarios) */
    @Query("UPDATE configuracion SET idioma = :idioma, version = version + 1, lastModified = :timestamp, pendienteSync = 1 WHERE id = 1")
    suspend fun updateIdioma(idioma: String, timestamp: Long = System.currentTimeMillis())

    /** Actualiza la fuente (común a todos los usuarios) */
    @Query("UPDATE configuracion SET fuente = :fuente, version = version + 1, lastModified = :timestamp, pendienteSync = 1 WHERE id = 1")
    suspend fun updateFuente(fuente: String, timestamp: Long = System.currentTimeMillis())

    /** Actualiza el tema oscuro (común a todos los usuarios) */
    @Query("UPDATE configuracion SET temaOscuro = :esTemaOscuro, version = version + 1, lastModified = :timestamp, pendienteSync = 1 WHERE id = 1")
    suspend fun updateTemaOscuro(esTemaOscuro: Boolean, timestamp: Long = System.currentTimeMillis())

    // ========== USUARIO LOGUEADO V10 ==========

    /** Establece el usuario actualmente logueado */
    @Query("UPDATE configuracion SET usuarioLogueado = :usuarioUid, version = version + 1, lastModified = :timestamp WHERE id = 1")
    suspend fun setUsuarioLogueado(usuarioUid: String, timestamp: Long = System.currentTimeMillis())

    /** Obtiene el UID del usuario actualmente logueado */
    @Query("SELECT usuarioLogueado FROM configuracion WHERE id = 1")
    suspend fun getUsuarioLogueado(): String?

    /** Flow que emite el usuario logueado */
    @Query("SELECT usuarioLogueado FROM configuracion WHERE id = 1")
    fun getUsuarioLogueadoFlow(): Flow<String?>

    /** Verifica si es usuario por defecto */
    @Query("SELECT usuarioLogueado = 'usuario_default' FROM configuracion WHERE id = 1")
    suspend fun isUsuarioDefault(): Boolean

    // ========== SINCRONIZACIÓN V10 ==========

    /** Marca si hay pendiente de sincronizar en Firebase */
    @Query("UPDATE configuracion SET pendienteSync = :pendiente WHERE id = 1")
    suspend fun updatePendienteSync(pendiente: Boolean)

    /** Flujo que emite true/false según si hay pendienteSync */
    @Query("SELECT pendienteSync FROM configuracion WHERE id = 1")
    fun getPendienteSyncFlow(): Flow<Boolean>

    /** Actualiza campos de sincronización después de sync exitoso */
    @Query("UPDATE configuracion SET version = :version, lastModified = :timestamp, pendienteSync = 0 WHERE id = 1")
    suspend fun markSyncSuccessful(version: Long, timestamp: Long = System.currentTimeMillis())

    /** Obtiene versión actual para resolución de conflictos */
    @Query("SELECT version FROM configuracion WHERE id = 1")
    suspend fun getCurrentVersion(): Long?

    /** Obtiene timestamp de última modificación */
    @Query("SELECT lastModified FROM configuracion WHERE id = 1")
    suspend fun getLastModified(): Long?

    // ========== MÉTODOS LEGACY (mantener para compatibilidad) ==========

    /** Actualiza la fecha de última sincronización */
    @Query("UPDATE configuracion SET fechaUltimaSync = :fecha WHERE id = 1")
    suspend fun updateFechaUltimaSync(fecha: String?)

    /** Actualiza el último dispositivo */
    @Query("UPDATE configuracion SET ultimoDispositivo = :dispositivo WHERE id = 1")
    suspend fun updateUltimoDispositivo(dispositivo: String?)

    /** Actualiza el número de versión de la app */
    @Query("UPDATE configuracion SET numeroVersion = :version WHERE id = 1")
    suspend fun updateNumeroVersion(version: String)

    // ========== MÉTODOS OBSOLETOS V10 (mantener por compatibilidad) ==========

    @Deprecated("Campo eliminado en V10 - usar UserRepository para gestionar usuarios", ReplaceWith("UserRepository.updateUserPassword()"))
    @Query("SELECT 1") // Query dummy que no hace nada
    suspend fun updateUsuarioPassword(password: String?) { /* No-op */ }

    @Deprecated("Campo eliminado en V10 - usar UserRepository para gestionar usuarios", ReplaceWith("UserRepository.updateUserEmail()"))
    @Query("SELECT 1") // Query dummy que no hace nada
    suspend fun updateUsuarioEmail(email: String?) { /* No-op */ }

    @Deprecated("Campo eliminado en V10 - usar setUsuarioLogueado()", ReplaceWith("setUsuarioLogueado(usuarioId)"))
    @Query("SELECT 1") // Query dummy que no hace nada
    suspend fun updateUsuarioId(usuarioId: String?) { /* No-op */ }

    @Deprecated("Campo eliminado en V10 - usar UserRepository.updateUserPremium()", ReplaceWith("UserRepository.updateUserPremium()"))
    @Query("SELECT 1") // Query dummy que no hace nada
    suspend fun updateVersionApp(version: Int) { /* No-op */ }
}
