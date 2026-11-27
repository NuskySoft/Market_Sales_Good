// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/SaldoGuardadoDao.kt
package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Persistencia de saldos guardados y control de disponibilidad.

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaldoGuardadoDao {
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM saldos_guardados WHERE idRegistro = :id AND idUsuario = :uid")
    suspend fun byId(id: String, uid: String): SaldoGuardadoEntity?
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM saldos_guardados WHERE idUsuario = :uid AND consumido = 0 AND eliminado = 0 ORDER BY fechaEpochDay DESC, lastModified DESC")
    fun pendientes(uid: String): Flow<List<SaldoGuardadoEntity>>


    @Upsert
    suspend fun upsert(item: SaldoGuardadoEntity)

    @Upsert
    suspend fun upsertAll(items: List<SaldoGuardadoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SaldoGuardadoEntity)

    @Update
    suspend fun update(item: SaldoGuardadoEntity)

    @Delete
    suspend fun delete(item: SaldoGuardadoEntity)

    // Eliminación local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM saldos_guardados WHERE idRegistro = :id")
    suspend fun deleteById(id: String)

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM saldos_guardados WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // ===== Lecturas =====
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM saldos_guardados WHERE idUsuario = :userId ORDER BY lastModified DESC")
    fun getAllByUser(userId: String): Flow<List<SaldoGuardadoEntity>>

    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM saldos_guardados WHERE idUsuario = :userId AND consumido = 0 AND eliminado = 0 ORDER BY lastModified DESC LIMIT 1")
    suspend fun getUltimoNoConsumido(userId: String): SaldoGuardadoEntity?

    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) > 0 FROM saldos_guardados WHERE idUsuario = :userId")
    suspend fun existeAlgunoDeUsuario(userId: String): Boolean

    // Consulta local de lectura del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM saldos_guardados WHERE idUsuario = :userId AND sincronizadoFirebase = 0")
    suspend fun getNoSincronizados(userId: String): List<SaldoGuardadoEntity>

    // ===== Estados / flags =====
    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE saldos_guardados
        SET consumido = 1,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idRegistro = :id
    """)
    suspend fun marcarConsumido(id: String, timestamp: Long = System.currentTimeMillis())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE saldos_guardados
        SET sincronizadoFirebase = 1,
            lastModified = :timestamp
        WHERE idRegistro = :id
    """)
    suspend fun marcarSincronizado(id: String, timestamp: Long = System.currentTimeMillis())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE saldos_guardados
        SET sincronizadoFirebase = 0
        WHERE idRegistro = :id
    """)
    suspend fun marcarNoSincronizado(id: String)
}
