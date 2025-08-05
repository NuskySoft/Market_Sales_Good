// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/RecibosDao.kt
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecibosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReciboEntity>)

    @Query("DELETE FROM recibos WHERE idUsuario = :userId")
    suspend fun borrarPorUsuario(userId: String)

    @Upsert
    suspend fun upsert(recibo: ReciboEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM recibos WHERE idUsuario = :uid LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean

    @Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo ORDER BY fechaHora DESC")
    fun obtenerRecibosPorMercadillo(idMercadillo: String): Flow<List<ReciboEntity>>

    @Query("SELECT * FROM recibos WHERE idRecibo = :idRecibo")
    suspend fun obtenerReciboPorId(idRecibo: String): ReciboEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRecibo(recibo: ReciboEntity)

    @Update
    suspend fun actualizarRecibo(recibo: ReciboEntity)

    @Delete
    suspend fun eliminarRecibo(recibo: ReciboEntity)

    @Query("SELECT * FROM recibos WHERE idMercadillo = :idMercadillo ORDER BY fechaHora ASC")
    fun observarRecibosPorMercadillo(idMercadillo: String): Flow<List<ReciboEntity>>

    @Query("SELECT * FROM recibos WHERE idUsuario = :userId ORDER BY fechaHora DESC")
    fun observarRecibosUsuario(userId: String): Flow<List<ReciboEntity>>

    @Query("DELETE FROM recibos WHERE idMercadillo = :idMercadillo")
    suspend fun eliminarRecibosPorMercadillo(idMercadillo: String)
}
