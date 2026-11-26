// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/LineasGastosDao.kt
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineasGastosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LineaGastoEntity>)

    // === INSERT/UPSERT ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLinea(linea: LineaGastoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLineas(lineas: List<LineaGastoEntity>)

    // === DELETE POR MERCADILLO ===
    @Query("DELETE FROM LineasGastos WHERE idMercadillo = :idMercadillo")
    suspend fun borrarPorMercadillo(idMercadillo: String)

    @Query("DELETE FROM LineasGastos WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // === QUERIES ===
    @Query("SELECT * FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1 ORDER BY numeroLinea ASC")
    fun observarGastosPorMercadillo(idMercadillo: String): Flow<List<LineaGastoEntity>>

    @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1")
    suspend fun getTotalGastosMercadillo(idMercadillo: String): Double

    @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1 AND formaPago = :forma")
    suspend fun getTotalGastosPorMetodo(idMercadillo: String, forma: String): Double

    // ✅ NUEVO: solo gastos en efectivo que SÍ se descuentan de caja (para arqueo)
    @Query("""
        SELECT COALESCE(SUM(importe), 0.0) 
        FROM LineasGastos 
        WHERE idMercadillo = :idMercadillo 
          AND activo = 1 
          AND formaPago = 'efectivo'
          AND descontarDeCaja = 1
    """)
    suspend fun getTotalGastosEfectivoDeCaja(idMercadillo: String): Double

    @Query("SELECT MAX(numeroLinea) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1")
    suspend fun obtenerMaxNumeroLineaPorMercadillo(idMercadillo: String): String?

    // === Sincronización híbrida ===
    @Query("SELECT * FROM LineasGastos WHERE idUsuario = :userId AND sincronizadoFirebase = 0")
    suspend fun getPendientesSync(userId: String): List<LineaGastoEntity>

    @Query("UPDATE LineasGastos SET sincronizadoFirebase = 1 WHERE idUsuario = :uid AND idMercadillo = :mercadillo AND numeroLinea = :numeroLinea AND lastModified = :timestamp AND version = :version")
    suspend fun marcarSincronizado(uid: String, mercadillo: String, numeroLinea: String, timestamp: Long, version: Long): Int

    @Query("SELECT COUNT(*) FROM LineasGastos WHERE idUsuario = :uid")
    suspend fun contarPorUsuario(uid: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM LineasGastos WHERE idUsuario = :uid AND activo = 1 LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean

    @Query("UPDATE LineasGastos SET sincronizadoFirebase = 0 WHERE idMercadillo = :mercadillo AND numeroLinea = :numeroLinea")
    suspend fun marcarPendiente(mercadillo: String, numeroLinea: String)
}


//// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/LineasGastosDao.kt
//package es.nuskysoftware.marketsales.data.local.dao
//
//import androidx.room.*
//import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface LineasGastosDao {
//
//    // === INSERT/UPSERT ===
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertarLinea(linea: LineaGastoEntity)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertarLineas(lineas: List<LineaGastoEntity>)
//
//    // === DELETE POR MERCADILLO ===
//    @Query("DELETE FROM LineasGastos WHERE idMercadillo = :idMercadillo")
//    suspend fun borrarPorMercadillo(idMercadillo: String)
//
//    // === QUERIES ===
//    //  @Query("SELECT * FROM LineasGastos WHERE idMercadillo = :idMercadillo ORDER BY numeroLinea ASC")
//    @Query("SELECT * FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1 ORDER BY numeroLinea ASC")
//    fun observarGastosPorMercadillo(idMercadillo: String): Flow<List<LineaGastoEntity>>
//
//    // @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo")
//    @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1")
//    suspend fun getTotalGastosMercadillo(idMercadillo: String): Double
//
//    // @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND formaPago = :forma")
//    @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1 AND formaPago = :forma")
//    suspend fun getTotalGastosPorMetodo(idMercadillo: String, forma: String): Double
//
//    // @Query("SELECT MAX(numeroLinea) FROM LineasGastos WHERE idMercadillo = :idMercadillo")
//    @Query("SELECT MAX(numeroLinea) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1")
//    suspend fun obtenerMaxNumeroLineaPorMercadillo(idMercadillo: String): String?
//
//    // === Sincronización híbrida ===
//    @Query("SELECT * FROM LineasGastos WHERE idUsuario = :userId AND sincronizadoFirebase = 0")
//    suspend fun getPendientesSync(userId: String): List<LineaGastoEntity>
//
//    @Query("UPDATE LineasGastos SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idMercadillo = :mercadillo AND numeroLinea = :numeroLinea")
//    suspend fun marcarSincronizado(mercadillo: String, numeroLinea: String, timestamp: Long)
//
//    @Query("UPDATE LineasGastos SET sincronizadoFirebase = 0 WHERE idMercadillo = :mercadillo AND numeroLinea = :numeroLinea")
//    suspend fun marcarPendiente(mercadillo: String, numeroLinea: String)
//}
//
