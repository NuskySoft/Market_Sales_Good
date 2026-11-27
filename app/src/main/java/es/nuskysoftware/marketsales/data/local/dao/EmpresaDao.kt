package es.nuskysoftware.marketsales.data.local.dao
import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.EmpresaEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface EmpresaDao {
    @Query("SELECT * FROM empresa WHERE userId = :uid") suspend fun getEmpresa(uid: String): EmpresaEntity?
    @Query("SELECT * FROM empresa WHERE userId = :uid") fun getEmpresaFlow(uid: String): Flow<EmpresaEntity?>
    @Upsert suspend fun insertOrUpdate(empresa: EmpresaEntity)
}
