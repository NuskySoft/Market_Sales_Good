package es.nuskysoftware.marketsales.data.repository
import android.content.Context
import androidx.room.withTransaction
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.EmpresaEntity
import es.nuskysoftware.marketsales.utils.*
import es.nuskysoftware.marketsales.work.BusinessSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
class EmpresaRepository(context: Context) {
    private val app = context.applicationContext
    private val db = AppDatabase.getDatabase(app)
    private val dao = db.empresaDao()
    fun getEmpresaFlow(): Flow<EmpresaEntity?> = ConfigurationManager.usuarioLogueado.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(null) else dao.getEmpresaFlow(uid)
    }
    suspend fun guardarOActualizar(nif: String, nombre: String, razon: String, direccion: String, poblacion: String, codigoPostal: String, provincia: String, pais: String) = withContext(Dispatchers.IO) {
        val uid = ConfigurationManager.getCurrentUserId() ?: error("No owner")
        RecoveryGate(app).beforeWrite(uid)
        db.withTransaction {
            DemoAccess.requireOwner(uid)
            val old = dao.getEmpresa(uid)
            dao.insertOrUpdate(EmpresaEntity(userId = uid, nif = nif, nombre = nombre, razonSocial = razon, direccion = direccion, poblacion = poblacion, codigoPostal = codigoPostal, provincia = provincia, pais = pais, version = (old?.version ?: 0) + 1, lastModified = System.currentTimeMillis()))
        }
        BusinessSyncScheduler.request(app)
    }
}
