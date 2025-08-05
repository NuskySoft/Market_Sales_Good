// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/SaldoGuardadoDao.kt
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaldoGuardadoDao {

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

    @Query("DELETE FROM saldos_guardados WHERE idRegistro = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM saldos_guardados WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // ===== Lecturas =====
    @Query("SELECT * FROM saldos_guardados WHERE idUsuario = :userId ORDER BY lastModified DESC")
    fun getAllByUser(userId: String): Flow<List<SaldoGuardadoEntity>>

    @Query("SELECT * FROM saldos_guardados WHERE idUsuario = :userId AND consumido = 0 ORDER BY lastModified DESC LIMIT 1")
    suspend fun getUltimoNoConsumido(userId: String): SaldoGuardadoEntity?

    @Query("SELECT COUNT(*) > 0 FROM saldos_guardados WHERE idUsuario = :userId")
    suspend fun existeAlgunoDeUsuario(userId: String): Boolean

    @Query("SELECT * FROM saldos_guardados WHERE idUsuario = :userId AND sincronizadoFirebase = 0")
    suspend fun getNoSincronizados(userId: String): List<SaldoGuardadoEntity>

    // ===== Estados / flags =====
    @Query("""
        UPDATE saldos_guardados
        SET consumido = 1,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idRegistro = :id
    """)
    suspend fun marcarConsumido(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE saldos_guardados
        SET sincronizadoFirebase = 1,
            lastModified = :timestamp
        WHERE idRegistro = :id
    """)
    suspend fun marcarSincronizado(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE saldos_guardados
        SET sincronizadoFirebase = 0
        WHERE idRegistro = :id
    """)
    suspend fun marcarNoSincronizado(id: String)
}


//// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/SaldoGuardadoDao.kt
//package es.nuskysoftware.marketsales.data.local.dao
//
//import androidx.room.*
//import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
//
//@Dao
//interface SaldoGuardadoDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsert(item: SaldoGuardadoEntity)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsertAll(items: List<SaldoGuardadoEntity>)
//
//    @Query("""
//        SELECT * FROM saldos_guardados
//        WHERE idUsuario = :userId AND consumido = 0
//        ORDER BY lastModified DESC
//        LIMIT 1
//    """)
//    suspend fun getUltimoNoConsumido(userId: String): SaldoGuardadoEntity?
//
//    @Query("DELETE FROM saldos_guardados WHERE idRegistro = :id")
//    suspend fun deleteById(id: String)
//
//    @Query("""
//        UPDATE saldos_guardados
//        SET consumido = 1, lastModified = :now, sincronizadoFirebase = 0
//        WHERE idRegistro = :id
//    """)
//    suspend fun marcarConsumido(id: String, now: Long = System.currentTimeMillis())
//
//    @Query("SELECT * FROM saldos_guardados WHERE idUsuario = :userId ORDER BY lastModified DESC")
//    suspend fun getAllByUser(userId: String): List<SaldoGuardadoEntity>
//
//    // ✅ FALTABA: usado por el repositorio tras sincronizar con Firebase
//    @Query("UPDATE saldos_guardados SET sincronizadoFirebase = 1 WHERE idRegistro = :id")
//    suspend fun marcarSincronizado(id: String)
//
//}
//
