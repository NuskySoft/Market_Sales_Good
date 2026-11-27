// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/RecibosDao.kt
package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Persistencia de cabeceras de recibos y sus estados de sincronización.

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecibosDao {
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM recibos WHERE idUsuario = :uid")
    suspend fun contarDemo(uid: String): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReciboEntity>)

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM recibos WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM recibos WHERE idUsuario = :uid")
    suspend fun contarPorUsuario(uid: String): Int

    @Upsert
    suspend fun upsert(recibo: ReciboEntity)

    //@Query("SELECT EXISTS(SELECT 1 FROM recibos WHERE idUsuario = :uid LIMIT 1)")
    // Consulta local de lectura; comprueba la existencia de registros del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT EXISTS(SELECT 1 FROM recibos WHERE idUsuario = :uid AND activo = 1 LIMIT 1)")

    suspend fun existeAlgunoDeUsuario(uid: String): Boolean

    //@Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo ORDER BY fechaHora DESC")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo AND activo = 1  AND idUsuario = :sessionOwner ORDER BY fechaHora DESC")
    fun obtenerRecibosPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Flow<List<ReciboEntity>>

    //@Query("SELECT * FROM recibos WHERE idRecibo = :idRecibo")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM recibos WHERE idRecibo = :idRecibo AND activo = 1 AND idUsuario = :sessionOwner ")

    suspend fun obtenerReciboPorId(idRecibo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): ReciboEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRecibo(recibo: ReciboEntity)

    @Update
    suspend fun actualizarRecibo(recibo: ReciboEntity)

    @Delete
    suspend fun eliminarRecibo(recibo: ReciboEntity)

    //@Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo ORDER BY fechaHora ASC")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo AND activo = 1  AND idUsuario = :sessionOwner ORDER BY fechaHora ASC")
    fun observarRecibosPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Flow<List<ReciboEntity>>

    //@Query("SELECT * FROM recibos WHERE idUsuario = :userId ORDER BY fechaHora DESC")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM recibos WHERE idUsuario = :userId AND activo = 1 ORDER BY fechaHora DESC")
    fun observarRecibosUsuario(userId: String): Flow<List<ReciboEntity>>

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM recibos WHERE idMercadillo = :idMercadillo AND idUsuario = :sessionOwner ")
    suspend fun eliminarRecibosPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // === Sincronización híbrida ===
    // Consulta local de lectura del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM recibos WHERE idUsuario = :userId AND sincronizadoFirebase = 0")
    suspend fun getPendientesSync(userId: String): List<ReciboEntity>

    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE recibos SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idRecibo = :id AND idUsuario = :sessionOwner ")
    suspend fun marcarSincronizado(id: String, timestamp: Long, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE recibos SET sincronizadoFirebase = 0 WHERE idRecibo = :id AND idUsuario = :sessionOwner ")
    suspend fun marcarPendiente(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())
}
