package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecibosDao {

    // ===== CRUD / lote =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReciboEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRecibo(recibo: ReciboEntity)

    @Update
    suspend fun actualizarRecibo(recibo: ReciboEntity)

    @Delete
    suspend fun eliminarRecibo(recibo: ReciboEntity)

    @Query("DELETE FROM recibos WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    @Query("DELETE FROM recibos WHERE idMercadillo = :idMercadillo")
    suspend fun eliminarRecibosPorMercadillo(idMercadillo: String)

    // ===== Listado / Observación =====
    @Query("SELECT * FROM recibos WHERE idUsuario = :userId ORDER BY fechaHora DESC")
    fun observarRecibosUsuario(userId: String): Flow<List<ReciboEntity>>

    @Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo ORDER BY fechaHora DESC")
    fun observarRecibosPorMercadillo(idMercadillo: String): Flow<List<ReciboEntity>>

    // Alias que tu pantalla usa por nombre exacto
    @Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo ORDER BY fechaHora DESC")
    fun obtenerRecibosPorMercadillo(idMercadillo: String): Flow<List<ReciboEntity>>

    @Query("SELECT * FROM recibos WHERE idRecibo = :idRecibo LIMIT 1")
    suspend fun obtenerReciboPorId(idRecibo: String): ReciboEntity?

    // ===== Pendientes de sync =====
    @Query("SELECT * FROM recibos WHERE idUsuario = :userId AND sincronizadoFirebase = 0 ORDER BY lastModified ASC")
    suspend fun obtenerPendientes(userId: String): List<ReciboEntity>

    @Query("UPDATE recibos SET sincronizadoFirebase = 1, syncError = NULL WHERE idRecibo IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)

    @Query("UPDATE recibos SET syncError = :error WHERE idRecibo = :idRecibo")
    suspend fun marcarError(idRecibo: String, error: String)

    // ===== Aux =====
    @Query("SELECT EXISTS(SELECT 1 FROM recibos WHERE idUsuario = :userId AND sincronizadoFirebase = 0)")
    suspend fun existenPendientes(userId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM recibos WHERE idUsuario = :uid LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean
}


//// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/RecibosDao.kt
//package es.nuskysoftware.marketsales.data.local.dao
//
//import androidx.room.*
//import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface RecibosDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsertAll(items: List<ReciboEntity>)
//
//    @Query("DELETE FROM recibos WHERE idUsuario = :userId")
//    suspend fun borrarPorUsuario(userId: String)
//
//    @Upsert
//    suspend fun upsert(recibo: ReciboEntity)
//
//    @Query("SELECT EXISTS(SELECT 1 FROM recibos WHERE idUsuario = :uid LIMIT 1)")
//    suspend fun existeAlgunoDeUsuario(uid: String): Boolean
//
//    @Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo ORDER BY fechaHora DESC")
//    fun obtenerRecibosPorMercadillo(idMercadillo: String): Flow<List<ReciboEntity>>
//
//    @Query("SELECT * FROM recibos WHERE idRecibo = :idRecibo")
//    suspend fun obtenerReciboPorId(idRecibo: String): ReciboEntity?
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertarRecibo(recibo: ReciboEntity)
//
//    @Update
//    suspend fun actualizarRecibo(recibo: ReciboEntity)
//
//    @Delete
//    suspend fun eliminarRecibo(recibo: ReciboEntity)
//
//    @Query("DELETE FROM recibos WHERE idMercadillo = :idMercadillo")
//    suspend fun eliminarRecibosPorMercadillo(idMercadillo: String)
//}
