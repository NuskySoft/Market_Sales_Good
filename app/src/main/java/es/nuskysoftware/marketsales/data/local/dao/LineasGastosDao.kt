// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/LineasGastosDao.kt
package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Persistencia de gastos con clave compuesta mercadillo y número de línea.

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
    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM LineasGastos WHERE idMercadillo = :idMercadillo AND idUsuario = :sessionOwner ")
    suspend fun borrarPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM LineasGastos WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // === QUERIES ===
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1  AND idUsuario = :sessionOwner ORDER BY numeroLinea ASC")
    fun observarGastosPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Flow<List<LineaGastoEntity>>

    // Consulta local de lectura; agrega los importes o cantidades seleccionados del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COALESCE((SUM(CAST(ROUND(importe * 100, 0) AS INTEGER)) / 100.0), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1 AND idUsuario = :sessionOwner ")
    suspend fun getTotalGastosMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Double

    // Consulta local de lectura; agrega los importes o cantidades seleccionados del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COALESCE((SUM(CAST(ROUND(importe * 100, 0) AS INTEGER)) / 100.0), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1 AND formaPago = :forma AND idUsuario = :sessionOwner ")
    suspend fun getTotalGastosPorMetodo(idMercadillo: String, forma: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Double

    // ✅ NUEVO: solo gastos en efectivo que SÍ se descuentan de caja (para arqueo)
    // Consulta local de lectura; agrega los importes o cantidades seleccionados. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COALESCE((SUM(CAST(ROUND(importe * 100, 0) AS INTEGER)) / 100.0), 0.0)
        FROM LineasGastos
        WHERE idMercadillo = :idMercadillo
          AND activo = 1
          AND formaPago = 'efectivo'
          AND descontarDeCaja = 1
     AND idUsuario = :sessionOwner """)
    suspend fun getTotalGastosEfectivoDeCaja(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Double

    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT MAX(numeroLinea) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND activo = 1 AND idUsuario = :sessionOwner ")
    suspend fun obtenerMaxNumeroLineaPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): String?

    // === Sincronización híbrida ===
    // Consulta local de lectura del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM LineasGastos WHERE idUsuario = :userId AND sincronizadoFirebase = 0")
    suspend fun getPendientesSync(userId: String): List<LineaGastoEntity>

    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE LineasGastos SET sincronizadoFirebase = 1 WHERE idUsuario = :uid AND idMercadillo = :mercadillo AND numeroLinea = :numeroLinea AND lastModified = :timestamp AND version = :version")
    suspend fun marcarSincronizado(uid: String, mercadillo: String, numeroLinea: String, timestamp: Long, version: Long): Int

    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM LineasGastos WHERE idUsuario = :uid")
    suspend fun contarPorUsuario(uid: String): Int

    // Consulta local de lectura; comprueba la existencia de registros del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT EXISTS(SELECT 1 FROM LineasGastos WHERE idUsuario = :uid AND activo = 1 LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean

    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE LineasGastos SET sincronizadoFirebase = 0 WHERE idMercadillo = :mercadillo AND numeroLinea = :numeroLinea AND idUsuario = :sessionOwner ")
    suspend fun marcarPendiente(mercadillo: String, numeroLinea: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())
}


//// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/LineasGastosDao.kt
//    // === INSERT/UPSERT ===
//    // === DELETE POR MERCADILLO ===
//    // === QUERIES ===
//    //  @Query("SELECT * FROM LineasGastos WHERE idMercadillo = :idMercadillo ORDER BY numeroLinea ASC")
//    // @Query("SELECT COALESCE((SUM(CAST(ROUND(importe * 100, 0) AS INTEGER)) / 100.0), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo")
//    // @Query("SELECT COALESCE((SUM(CAST(ROUND(importe * 100, 0) AS INTEGER)) / 100.0), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND formaPago = :forma")
//    // @Query("SELECT MAX(numeroLinea) FROM LineasGastos WHERE idMercadillo = :idMercadillo")
//    // === Sincronización híbrida ===
