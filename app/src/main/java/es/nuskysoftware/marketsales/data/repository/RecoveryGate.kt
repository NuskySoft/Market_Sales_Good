package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Controla la autorización persistente de recuperación y el reinicio de datos de una cuenta en una instalación vacía.

import android.content.Context
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Persistent per-account permission to restore; automatic seed records never count as user data. */
class RecoveryGate(context: Context, private val db: AppDatabase = AppDatabase.getDatabase(context)) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("account_data_generations", Context.MODE_PRIVATE)
    private val fs = FirebaseFirestore.getInstance()
    init { current = this }
    companion object {
        @Volatile private var current: RecoveryGate? = null
        fun currentCanPull(uid: String) = current?.canPull(uid) == true
        data class Question(val uid: String, val firstWrite: Boolean)
        val question = MutableStateFlow<Question?>(null)
    }
    private val collections = listOf("categorias" to "userId", "articulos" to "userId", "mercadillos" to "userId", "recibos" to "idUsuario", "lineas_venta" to "idUsuario", "LineasGastos" to "idUsuario", "saldosGuardados" to "idUsuario", "empresa" to "userId", "stock_movements" to "idUsuario")
    // Detecta datos de negocio locales relevantes para la decisión de recuperación.
    private fun hasLocal(uid: String): Boolean {
        for ((table, owner) in listOf("categorias" to "userId", "articulos" to "userId", "mercadillos" to "userId", "recibos" to "idUsuario", "LineasGastos" to "idUsuario", "empresa" to "userId", "saldos_guardados" to "idUsuario")) {
            val extra = if (table == "categorias") " AND idCategoria != ?" else ""
            val args = if (table == "categorias") arrayOf(uid, CatalogDefaults.id(uid)) else arrayOf(uid)
            db.openHelper.readableDatabase.query("SELECT 1 FROM `$table` WHERE `$owner` = ?$extra LIMIT 1", args).use { if (it.moveToFirst()) return true }
        }
        return false
    }
    // Consulta si se permite descargar datos y no hay una eliminación en curso.
    fun canPull(uid: String): Boolean = prefs.getString("$uid:state", null) == "active" && prefs.getString("$uid:delete", null) == null
    // Prepara el estado de recuperación y comprueba la generación remota.
    suspend fun prepare(uid: String): String? = withContext(Dispatchers.IO) {
        if (!DemoAccess.canSync(uid)) return@withContext null
        if (prefs.getString("$uid:delete", null) != null) finishDeletion(uid)
        val ref = fs.collection("account_data_state").document(uid)
        val control = ref.get(Source.SERVER).await()
        if (control.getString("deleting") != null) return@withContext null
        val remoteGeneration = control.getString("generation") ?: "legacy"
        val known = prefs.getString("$uid:generation", null)
        if (known != null && known != remoteGeneration) {
            // A confirmed reset on another device invalidates that device's old local copies.
            clearLocal(uid)
            prefs.edit().remove("$uid:state").putString("$uid:generation", remoteGeneration).commit()
        }
        if (ConfigurationManager.getIsPremium()) {
            prefs.edit().putString("$uid:state", "active").putString("$uid:generation", remoteGeneration).commit()
            dismiss(uid)
        } else if (prefs.getString("$uid:state", null) == null) {
            val existing = hasLocal(uid)
            var remote = false
            if (!existing) for ((collection, owner) in collections) {
                val found = fs.collection(collection).whereEqualTo(owner, uid).limit(2).get(Source.SERVER).await()
                if (found.documents.any { doc -> collection != "categorias" || doc.id != CatalogDefaults.id(uid) }) { remote = true; break }
            }
            prefs.edit().putString("$uid:generation", remoteGeneration).putString("$uid:state", if (existing || !remote) "active" else "reserved").commit()
            if (remote && !existing) question.value = Question(uid, false)
        }
        if (!canPull(uid)) return@withContext null
        remoteGeneration
    }
    // Comprueba la autorización antes de escribir datos de negocio.
    fun beforeWrite(uid: String) {
        DemoAccess.requireOwner(uid)
        if (AppModePolicy.isLocalOnly(uid)) return
        val state = prefs.getString("$uid:state", null)
        // First local edit while offline is safe for an existing installation, but a new empty
        // installation must resolve recovery before it can mix or destroy remote data.
        if (state == null && !hasLocal(uid) || state == "reserved") {
            question.value = Question(uid, true)
            error(StringResourceManager.getString("recovery_decision_required", ConfigurationManager.idioma.value))
        }
    }
    // Cierra la pregunta pendiente del propietario indicado.
    fun dismiss(uid: String) { if (question.value?.uid == uid) question.value = null }
    // Registra la decisión explícita de reiniciar datos.
    fun confirmReset(uid: String) {
        DemoAccess.requireOwner(uid)
        check(prefs.getString("$uid:state", null) != "active")
        val generation = UUID.randomUUID().toString()
        prefs.edit().putString("$uid:delete", generation).putString("$uid:state", "active").commit()
        dismiss(uid)
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(app)
    }
    // Completa el reinicio pendiente y coordina la eliminación con el servidor.
    private suspend fun finishDeletion(uid: String) {
        check(DemoAccess.canSync(uid))
        val next = prefs.getString("$uid:delete", null) ?: return
        val ref = fs.collection("account_data_state").document(uid)
        val old = prefs.getString("$uid:generation", null) ?: (ref.get(Source.SERVER).await().getString("generation") ?: "legacy").also {
            prefs.edit().putString("$uid:generation", it).commit()
        }
        fs.runTransaction { tx ->
            val current = tx.get(ref)
            val generation = current.getString("generation") ?: "legacy"
            check(generation == old || generation == next) { "Another reset already completed" }
            if (generation == old) tx.set(ref, mapOf("generation" to old, "deleting" to next, "userId" to uid))
        }.await()
        for ((collection, owner) in collections) {
            val docs = fs.collection(collection).whereEqualTo(owner, uid).get(Source.SERVER).await()
            for (doc in docs.documents) {
                if ((doc.getString("_generation") ?: "legacy") != old) continue
                fs.runTransaction { tx ->
                    val control = tx.get(ref)
                    check(control.getString("deleting") == next)
                    val current = tx.get(doc.reference)
                    if (current.getString(owner) == uid && (current.getString("_generation") ?: "legacy") == old) tx.delete(doc.reference)
                }.await()
            }
        }
        fs.runTransaction { tx ->
            val current = tx.get(ref)
            if (current.getString("deleting") == next) tx.set(ref, mapOf("generation" to next, "userId" to uid))
        }.await()
        prefs.edit().putString("$uid:generation", next).remove("$uid:delete").commit()
    }
    // Limpia los datos locales de ese propietario en una transacción.
    private suspend fun clearLocal(uid: String) = db.withTransaction {
        for ((table, owner) in listOf("empresa" to "userId", "categorias" to "userId", "articulos" to "userId", "mercadillos" to "userId", "recibos" to "idUsuario", "lineas_venta" to "idUsuario", "LineasGastos" to "idUsuario", "saldos_guardados" to "idUsuario", "stock_movements" to "idUsuario"))
            db.openHelper.writableDatabase.execSQL("DELETE FROM `$table` WHERE `$owner` = ?", arrayOf(uid))
    }
}
