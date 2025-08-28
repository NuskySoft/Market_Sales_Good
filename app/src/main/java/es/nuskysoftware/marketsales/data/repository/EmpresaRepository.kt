package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.EmpresaDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.EmpresaEntity
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Repository para gestionar datos de Empresa con política offline-first
 */
class EmpresaRepository(context: Context) {

    private val empresaDao: EmpresaDao = AppDatabase.getDatabase(context).empresaDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val connectivityObserver = ConnectivityObserver(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "EmpresaRepository"
    }

    init {
        scope.launch {
            connectivityObserver.isConnected.collect { online ->
                if (online) {
                    sincronizarPendiente()
                }
            }
        }
    }

    fun getEmpresaFlow(): Flow<EmpresaEntity?> = empresaDao.getEmpresaFlow()

    suspend fun guardarOActualizar(
        nif: String,
        nombre: String,
        razon: String,
        direccion: String,
        poblacion: String,
        codigoPostal: String,
        provincia: String,
        pais: String
    ) = withContext(Dispatchers.IO) {
        val existente = empresaDao.getEmpresa()
        val entidad = if (existente == null) {
            EmpresaEntity(
                nif = nif,
                nombre = nombre,
                razonSocial = razon,
                direccion = direccion,
                poblacion = poblacion,
                codigoPostal = codigoPostal,
                provincia = provincia,
                pais = pais,
                sincronizadoFirebase = false
            )
        } else {
            existente.copy(
                nif = nif,
                nombre = nombre,
                razonSocial = razon,
                direccion = direccion,
                poblacion = poblacion,
                codigoPostal = codigoPostal,
                provincia = provincia,
                pais = pais,
                version = existente.version + 1,
                lastModified = System.currentTimeMillis(),
                sincronizadoFirebase = false
            )
        }

        empresaDao.insertOrUpdate(entidad)
        sincronizarConFirebase(entidad)
    }

    private suspend fun sincronizarConFirebase(empresa: EmpresaEntity) {
        try {
            if (!connectivityObserver.isConnected.first()) {
                Log.d(TAG, "⚠️ Sin conexión, empresa pendiente de sincronizar")
                return
            }

            val datos = mapOf(
                "nif" to empresa.nif,
                "nombre" to empresa.nombre,
                "razonSocial" to empresa.razonSocial,
                "direccion" to empresa.direccion,
                "poblacion" to empresa.poblacion,
                "codigoPostal" to empresa.codigoPostal,
                "provincia" to empresa.provincia,
                "pais" to empresa.pais,
                "version" to empresa.version,
                "lastModified" to empresa.lastModified,
                "fechaSync" to System.currentTimeMillis()
            )

            firestore.collection("empresa")
                .document("datos")
                .set(datos)
                .await()

            empresaDao.updateSincronizado(true)
            Log.d(TAG, "☁️ Empresa sincronizada con Firebase")
        } catch (e: Exception) {
            Log.w(TAG, "❌ Error sincronizando empresa", e)
        }
    }

    private suspend fun sincronizarPendiente() {
        val pendiente = empresaDao.getEmpresaPendiente()
        if (pendiente != null) {
            sincronizarConFirebase(pendiente)
        }
    }
}