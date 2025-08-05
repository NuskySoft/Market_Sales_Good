// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/ConfiguracionDao.kt
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity

@Dao
interface ConfiguracionDao {

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

    @Query("UPDATE configuracion SET usuarioPassword = :password WHERE id = 1")
    suspend fun updateUsuarioPassword(password: String?)

    @Query("UPDATE configuracion SET temaOscuro = :esTemaOscuro WHERE id = 1")
    suspend fun updateTemaOscuro(esTemaOscuro: Boolean)

    @Query("UPDATE configuracion SET fuente = :fuente WHERE id = 1")
    suspend fun updateFuente(fuente: String)

    @Query("UPDATE configuracion SET idioma = :idioma WHERE id = 1")
    suspend fun updateIdioma(idioma: String)

    @Query("UPDATE configuracion SET moneda = :moneda WHERE id = 1")
    suspend fun updateMoneda(moneda: String)

    @Query("UPDATE configuracion SET versionApp = :version WHERE id = 1")
    suspend fun updateVersionApp(version: Int)

    @Query("UPDATE configuracion SET usuarioEmail = :email WHERE id = 1")
    suspend fun updateUsuarioEmail(email: String?)

    @Query("UPDATE configuracion SET usuarioId = :usuarioId WHERE id = 1")
    suspend fun updateUsuarioId(usuarioId: String?)

    @Query("UPDATE configuracion SET ultimoDispositivo = :dispositivo WHERE id = 1")
    suspend fun updateUltimoDispositivo(dispositivo: String?)

    @Query("UPDATE configuracion SET fechaUltimaSync = :fecha WHERE id = 1")
    suspend fun updateFechaUltimaSync(fecha: String?)

    @Query("UPDATE configuracion SET numeroVersion = :version WHERE id = 1")
    suspend fun updateNumeroVersion(version: String)

    @Query("DELETE FROM configuracion")
    suspend fun eliminarTodasLasConfiguraciones()

    // ————— Nuevos métodos para offline-first —————

    /** Marca si hay pendiente de sincronizar en Firebase */
    @Query("UPDATE configuracion SET pendienteSync = :pendiente WHERE id = 1")
    suspend fun updatePendienteSync(pendiente: Boolean)

    /** Flujo que emite true/false según si hay pendienteSync */
    @Query("SELECT pendienteSync FROM configuracion WHERE id = 1")
    fun getPendienteSyncFlow(): Flow<Boolean>
}


