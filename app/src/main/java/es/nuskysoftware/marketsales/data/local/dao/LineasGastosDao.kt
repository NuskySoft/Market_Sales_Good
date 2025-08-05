// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/LineasGastosDao.kt
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineasGastosDao {

    // === INSERT/UPSERT ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLinea(linea: LineaGastoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLineas(lineas: List<LineaGastoEntity>)

    // === DELETE POR MERCADILLO ===
    @Query("DELETE FROM LineasGastos WHERE idMercadillo = :idMercadillo")
    suspend fun borrarPorMercadillo(idMercadillo: String)

    // === QUERIES ===
    @Query("SELECT * FROM LineasGastos WHERE idMercadillo = :idMercadillo ORDER BY numeroLinea ASC")
    fun observarGastosPorMercadillo(idMercadillo: String): Flow<List<LineaGastoEntity>>

    @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo")
    suspend fun getTotalGastosMercadillo(idMercadillo: String): Double

    @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo AND formaPago = :forma")
    suspend fun getTotalGastosPorMetodo(idMercadillo: String, forma: String): Double

    @Query("SELECT MAX(numeroLinea) FROM LineasGastos WHERE idMercadillo = :idMercadillo")
    suspend fun obtenerMaxNumeroLineaPorMercadillo(idMercadillo: String): String?
}



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
//    @Update
//    suspend fun actualizarLinea(linea: LineaGastoEntity)
//
//    // === DELETE ===
//    @Query("DELETE FROM LineasGastos WHERE idMercadillo = :idMercadillo AND numeroLinea = :numeroLinea")
//    suspend fun borrarLinea(idMercadillo: String, numeroLinea: String)
//
//    @Query("DELETE FROM LineasGastos WHERE idMercadillo = :idMercadillo")
//    suspend fun borrarPorMercadillo(idMercadillo: String)
//
//    // === QUERIES ===
//    @Query("SELECT * FROM LineasGastos WHERE idMercadillo = :idMercadillo ORDER BY fechaHora ASC, numeroLinea ASC")
//    fun observarGastosPorMercadillo(idMercadillo: String): Flow<List<LineaGastoEntity>>
//
//    @Query("SELECT COALESCE(SUM(importe), 0.0) FROM LineasGastos WHERE idMercadillo = :idMercadillo")
//    suspend fun getTotalGastosMercadillo(idMercadillo: String): Double
//
//    @Query("SELECT MAX(numeroLinea) FROM LineasGastos WHERE idMercadillo = :idMercadillo")
//    suspend fun obtenerMaxNumeroLineaPorMercadillo(idMercadillo: String): String?
//}
