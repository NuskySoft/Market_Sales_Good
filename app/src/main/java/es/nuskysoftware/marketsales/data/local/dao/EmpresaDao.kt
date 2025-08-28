

package  es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.EmpresaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la tabla empresa
 */
@Dao
interface EmpresaDao {

    @Query("SELECT * FROM empresa WHERE id = 1")
    suspend fun getEmpresa(): EmpresaEntity?

    @Query("SELECT * FROM empresa WHERE id = 1")
    fun getEmpresaFlow(): Flow<EmpresaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(empresa: EmpresaEntity)

    @Query("UPDATE empresa SET sincronizadoFirebase = :sincronizado WHERE id = 1")
    suspend fun updateSincronizado(sincronizado: Boolean)

    @Query("SELECT * FROM empresa WHERE sincronizadoFirebase = 0 LIMIT 1")
    suspend fun getEmpresaPendiente(): EmpresaEntity?
}