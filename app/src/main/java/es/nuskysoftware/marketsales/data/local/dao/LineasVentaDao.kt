// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/LineasVentaDao.kt
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineasVentaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LineaVentaEntity>)

    @Query("DELETE FROM lineas_venta WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // Líneas de un recibo (en orden de línea)
    @Query("SELECT * FROM lineas_venta WHERE idRecibo = :idRecibo ORDER BY numeroLinea ASC")
    fun observarLineasPorRecibo(idRecibo: String): Flow<List<LineaVentaEntity>>

    // Líneas de un mercadillo (si lo necesitas para vistas agregadas)
    @Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo ORDER BY numeroLinea ASC")
    fun observarLineasPorMercadillo(idMercadillo: String): Flow<List<LineaVentaEntity>>

    @Upsert
    suspend fun upsert(linea: LineaVentaEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM lineas_venta WHERE idUsuario = :uid LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean

    @Query("SELECT * FROM lineas_venta WHERE idRecibo = :idRecibo ORDER BY numeroLinea")
    fun obtenerLineasPorRecibo(idRecibo: String): Flow<List<LineaVentaEntity>>

    @Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo ORDER BY numeroLinea")
    fun obtenerLineasPorMercadillo(idMercadillo: String): Flow<List<LineaVentaEntity>>

    // ⚠️ Con PK compuesta, una única 'idLinea' ya no identifica por sí sola; mantenemos el método por compatibilidad,
    // pero para lecturas puntuales es mejor usar ambos campos.
    @Query("SELECT * FROM lineas_venta WHERE idMercadillo = :idMercadillo AND idLinea = :idLinea")
    suspend fun obtenerLineaPorId(idMercadillo: String, idLinea: String): LineaVentaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLinea(linea: LineaVentaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLineas(lineas: List<LineaVentaEntity>)

    @Update
    suspend fun actualizarLinea(linea: LineaVentaEntity)

    @Delete
    suspend fun eliminarLinea(linea: LineaVentaEntity)

    @Query("DELETE FROM lineas_venta WHERE idRecibo = :idRecibo")
    suspend fun eliminarLineasPorRecibo(idRecibo: String)

    @Query("DELETE FROM lineas_venta WHERE idMercadillo = :idMercadillo")
    suspend fun eliminarLineasPorMercadillo(idMercadillo: String)

    // ✅ Útil si alguna vez quieres contar (opcional)
    @Query("SELECT COUNT(*) FROM lineas_venta WHERE idMercadillo = :idMercadillo")
    suspend fun contarLineasPorMercadillo(idMercadillo: String): Int

    // ✅ Para reinicio por mercadillo: devuelve el mayor idLinea (lexicográfico funciona con 0-padding)
    @Query("SELECT MAX(idLinea) FROM lineas_venta WHERE idMercadillo = :idMercadillo")
    suspend fun obtenerMaxIdLineaPorMercadillo(idMercadillo: String): String?
}
