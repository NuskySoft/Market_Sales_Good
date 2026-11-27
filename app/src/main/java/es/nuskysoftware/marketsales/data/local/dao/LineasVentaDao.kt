
// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/LineasVentaDao.kt
package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Persistencia de líneas de venta y abono; consultas de importes y cantidades.

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineasVentaDao {
    /** Cuenta unidades positivas de catálogo de tickets confirmados, incluso si luego se abonan.
     * El propietario y el mercadillo delimitan el cupo; las líneas manuales no consumen unidades. */
    @Query("SELECT COALESCE(SUM(l.cantidad), 0) FROM lineas_venta l JOIN recibos r ON r.idRecibo = l.idRecibo AND r.idMercadillo = l.idMercadillo AND r.idUsuario = l.idUsuario WHERE l.idUsuario = :uid AND l.idMercadillo = :market AND l.tipoLinea = 'PRODUCTO' AND l.cantidad > 0 AND l.idLineaOriginalAbonada IS NULL AND r.estado = 'COMPLETADO'")
    suspend fun unidadesCatalogoVendidas(uid: String, market: String): Long

    // Mismo cómputo reactivo para informar del cupo antes del cobro.
    @Query("SELECT COALESCE(SUM(l.cantidad), 0) FROM lineas_venta l JOIN recibos r ON r.idRecibo = l.idRecibo AND r.idMercadillo = l.idMercadillo AND r.idUsuario = l.idUsuario WHERE l.idUsuario = :uid AND l.idMercadillo = :market AND l.tipoLinea = 'PRODUCTO' AND l.cantidad > 0 AND l.idLineaOriginalAbonada IS NULL AND r.estado = 'COMPLETADO'")
    fun observarUnidadesCatalogoVendidas(uid: String, market: String): Flow<Long>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LineaVentaEntity>)

    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM lineas_venta WHERE idUsuario = :uid")
    suspend fun contarPorUsuario(uid: String): Int


    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM lineas_venta WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // Líneas de un recibo (en orden de línea)
    //@Query("SELECT * FROM lineas_venta WHERE idRecibo = :idRecibo ORDER BY numeroLinea ASC")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM lineas_venta WHERE idRecibo = :idRecibo AND activo = 1  AND idUsuario = :sessionOwner ORDER BY numeroLinea ASC")
    fun observarLineasPorRecibo(idRecibo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Flow<List<LineaVentaEntity>>

    // Líneas de un mercadillo (si lo necesitas para vistas agregadas)
    //@Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo ORDER BY numeroLinea ASC")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo AND activo = 1  AND idUsuario = :sessionOwner ORDER BY numeroLinea ASC")
    fun observarLineasPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Flow<List<LineaVentaEntity>>

    @Upsert
    suspend fun upsert(linea: LineaVentaEntity)

    //@Query("SELECT EXISTS(SELECT 1 FROM lineas_venta WHERE idUsuario = :uid LIMIT 1)")
    // Consulta local de lectura; comprueba la existencia de registros del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT EXISTS(SELECT 1 FROM lineas_venta WHERE idUsuario = :uid AND activo = 1 LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean

    //@Query("SELECT * FROM lineas_venta WHERE idRecibo = :idRecibo ORDER BY numeroLinea")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM lineas_venta WHERE idRecibo = :idRecibo AND activo = 1  AND idUsuario = :sessionOwner ORDER BY numeroLinea")
    fun obtenerLineasPorRecibo(idRecibo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Flow<List<LineaVentaEntity>>

    //@Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo ORDER BY numeroLinea")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo AND activo = 1  AND idUsuario = :sessionOwner ORDER BY numeroLinea")
    fun obtenerLineasPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Flow<List<LineaVentaEntity>>

    // ⚠️ Con PK compuesta, una única 'idLinea' ya no identifica por sí sola; mantenemos el método por compatibilidad,
    // pero para lecturas puntuales es mejor usar ambos campos.
    //@Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo AND idLinea = :idLinea")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo AND idLinea = :idLinea AND activo = 1 AND idUsuario = :sessionOwner ")
    suspend fun obtenerLineaPorId(idMercadillo: String, idLinea: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): LineaVentaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLinea(linea: LineaVentaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLineas(lineas: List<LineaVentaEntity>)

    @Update
    suspend fun actualizarLinea(linea: LineaVentaEntity)

    @Delete
    suspend fun eliminarLinea(linea: LineaVentaEntity)

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM lineas_venta WHERE idRecibo = :idRecibo AND idUsuario = :sessionOwner ")
    suspend fun eliminarLineasPorRecibo(idRecibo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM lineas_venta WHERE idMercadillo = :idMercadillo AND idUsuario = :sessionOwner ")
    suspend fun eliminarLineasPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // ✅ Útil si alguna vez quieres contar (opcional)
    //@Query("SELECT COUNT(*) FROM lineas_venta WHERE idMercadillo = :idMercadillo")
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM lineas_venta WHERE idMercadillo = :idMercadillo AND activo = 1 AND idUsuario = :sessionOwner ")
    suspend fun contarLineasPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Int

    // ✅ Para reinicio por mercadillo: devuelve el mayor idLinea (lexicográfico funciona con 0-padding)
    //@Query("SELECT MAX(idLinea) FROM lineas_venta WHERE idMercadillo = :idMercadillo")
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT MAX(idLinea) FROM lineas_venta WHERE idMercadillo = :idMercadillo AND activo = 1 AND idUsuario = :sessionOwner ")
    suspend fun obtenerMaxIdLineaPorMercadillo(idMercadillo: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): String?

    // === Sincronización híbrida ===
    // Consulta local de lectura del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM lineas_venta WHERE idUsuario = :userId AND sincronizadoFirebase = 0")
    suspend fun getPendientesSync(userId: String): List<LineaVentaEntity>

    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE lineas_venta SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idMercadillo = :mercadillo AND idLinea = :idLinea AND idUsuario = :sessionOwner ")
    suspend fun marcarSincronizado(mercadillo: String, idLinea: String, timestamp: Long, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE lineas_venta SET sincronizadoFirebase = 0 WHERE idMercadillo = :mercadillo AND idLinea = :idLinea AND idUsuario = :sessionOwner ")
    suspend fun marcarPendiente(mercadillo: String, idLinea: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())
}
