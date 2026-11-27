package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Acceso local a los datos fiscales y de contacto de empresa.
import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.EmpresaEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface EmpresaDao {
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM empresa WHERE userId = :uid") suspend fun getEmpresa(uid: String): EmpresaEntity?
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM empresa WHERE userId = :uid") fun getEmpresaFlow(uid: String): Flow<EmpresaEntity?>
    @Upsert suspend fun insertOrUpdate(empresa: EmpresaEntity)
}
