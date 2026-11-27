package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Concilia los datos de negocio de Room y Firebase de cada cuenta. Las filas pendientes de Room constituyen la cola persistente de subida.

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.*
import es.nuskysoftware.marketsales.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.reflect.Modifier

/** Only business-data network writer. Dirty Room rows are the durable outbox. */
class SyncEngine(context: Context) {
    private val app = context.applicationContext
    private val db = AppDatabase.getDatabase(app)
    private val fs = FirebaseFirestore.getInstance()
    companion object { private val mutex = Mutex() }

    internal data class Table(val name: String, val remote: String, val owner: String, val keys: List<String>, val type: Class<*>)
    private val tables = listOf(
        Table("categorias", "categorias", "userId", listOf("idCategoria"), CategoriaEntity::class.java),
        Table("articulos", "articulos", "userId", listOf("idArticulo"), ArticuloEntity::class.java),
        Table("mercadillos", "mercadillos", "userId", listOf("idMercadillo"), MercadilloEntity::class.java),
        Table("recibos", "recibos", "idUsuario", listOf("idRecibo"), ReciboEntity::class.java),
        Table("lineas_venta", "lineas_venta", "idUsuario", listOf("idMercadillo", "idLinea"), LineaVentaEntity::class.java),
        Table("LineasGastos", "LineasGastos", "idUsuario", listOf("idMercadillo", "numeroLinea"), LineaGastoEntity::class.java),
        Table("saldos_guardados", "saldosGuardados", "idUsuario", listOf("idRegistro"), SaldoGuardadoEntity::class.java),
        Table("empresa", "empresa", "userId", listOf("userId"), EmpresaEntity::class.java)
    )
    // Selecciona los campos de entidad que se pueden persistir.
    private fun fields(t: Table) = t.type.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
    // Obtiene valores de referencia de la entidad para completar campos compatibles.
    private fun defaults(t: Table): Map<String, Any?> {
        val obj = t.type.getDeclaredConstructor().newInstance()
        return fields(t).associate { it.isAccessible = true; it.name to it.get(obj) }
    }
    // Lee las filas del propietario; dirtyOnly limita la consulta a los cambios pendientes.
    private fun rows(t: Table, uid: String, dirtyOnly: Boolean = false): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        db.openHelper.readableDatabase.query("SELECT * FROM `${t.name}` WHERE `${t.owner}` = ?" + if (dirtyOnly) " AND sincronizadoFirebase = 0" else "", arrayOf(uid)).use { c ->
            while (c.moveToNext()) result += fields(t).associate { f ->
                val i = c.getColumnIndexOrThrow(f.name)
                f.name to when {
                    c.isNull(i) -> null
                    f.type == Boolean::class.javaPrimitiveType -> (c.getInt(i) != 0)
                    c.getType(i) == Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                    c.getType(i) == Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                    else -> c.getString(i)
                }
            }
        }
        return result
    }
    // Compone el identificador de documento a partir de las claves de la tabla.
    private fun id(t: Table, row: Map<String, Any?>) = t.keys.joinToString(if (t.name == "lineas_venta") "_" else "-") { row[it].toString() }
    // Extrae la marca de modificación de origen, independiente de la hora de subida.
    private fun modified(row: Map<String, Any?>) = (row["lastModified"] as? Number)?.toLong() ?: 0L
    // Representación estable para resolver empates de fecha; omite metadatos de transporte y stock.
    private fun fingerprint(row: Map<String, Any?>) = row.filterKeys { it !in setOf("sincronizadoFirebase", "fechaSync", "stock", "_generation") }.toSortedMap().toString()
    // Selecciona el contenido que prevalece por fecha y, en empate, por representación estable.
    private fun newer(a: Map<String, Any?>, b: Map<String, Any?>): Boolean = modified(a) > modified(b) || (modified(a) == modified(b) && fingerprint(a) > fingerprint(b))
    // Persiste la representación de una fila en su tabla local.
    private fun write(t: Table, row: Map<String, Any?>) {
        val cv = ContentValues()
        for (f in fields(t)) when (val v = row[f.name]) {
            null -> cv.putNull(f.name)
            is Boolean -> cv.put(f.name, if (v) 1 else 0)
            is Number -> if (f.type == Double::class.javaPrimitiveType || f.type == java.lang.Double::class.java) cv.put(f.name, v.toDouble()) else cv.put(f.name, v.toLong())
            else -> cv.put(f.name, v.toString())
        }
        db.openHelper.writableDatabase.insert(t.name, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
    }
    // Aplica una fila remota en una transacción; la instantánea esperada protege frente a cambios locales posteriores.
    private suspend fun apply(t: Table, uid: String, remote: Map<String, Any?>, expected: Map<String, Any?>? = null) = db.withTransaction {
        val local = rows(t, uid).firstOrNull { id(t, it) == id(t, remote) }
        if (local != null && expected != null && fingerprint(local) != fingerprint(expected)) return@withTransaction
        if (local != null && newer(local, remote)) return@withTransaction
        if (t.name == "mercadillos" && local != null && (local["estado"] as? Number)?.toInt() in 6..7 && local["sincronizadoFirebase"] == true) return@withTransaction
        val keys = t.keys.joinToString(" AND ") { "`$it` = ?" }
        val args = t.keys.map { remote[it].toString() }.toTypedArray()
        db.openHelper.readableDatabase.query("SELECT `${t.owner}` FROM `${t.name}` WHERE $keys", args).use { c ->
            check(!c.moveToFirst() || c.getString(0) == uid) { "Local owner mismatch" }
        }
        val adjusted = if (t.name == "articulos" && remote["stockBase"] is Number) {
            val articleId = remote["idArticulo"].toString()
            val quantity = db.openHelper.readableDatabase.query("SELECT COALESCE(SUM(cantidad),0) FROM stock_movements WHERE idArticulo = ? AND idUsuario = ? AND aplicado = 1 AND incorporadoEnBase = 0", arrayOf(articleId, uid)).use { c -> c.moveToFirst(); c.getInt(0) }
            remote + ("stock" to ((remote["stockBase"] as Number).toInt() - quantity))
        } else remote
        write(t, adjusted + ("sincronizadoFirebase" to true))
    }
    // Concilia un propietario en IO bajo exclusión mutua, incluidas bajas y cambios pendientes.
    suspend fun sync(uid: String) = withContext(Dispatchers.IO) { mutex.withLock {
        if (!DemoAccess.canSync(uid)) return@withLock
        syncProfile(uid)
        val generation = RecoveryGate(app).prepare(uid) ?: return@withLock
        for (t in tables) {
            if (!DemoAccess.canSync(uid)) return@withLock
            for (row in rows(t, uid, true)) {
                if (!DemoAccess.canSync(uid)) return@withLock
                val ref = fs.collection(t.remote).document(id(t, row))
                val winner = fs.runTransaction { tx ->
                    check(DemoAccess.canSync(uid))
                    val control = tx.get(fs.collection("account_data_state").document(uid))
                    check((control.getString("generation") ?: "legacy") == generation) { "Account generation changed" }
                    check(control.getString("deleting") == null) { "Deletion pending" }
                    val snap = tx.get(ref)
                    val remote = snap.data
                    check(remote == null || remote[t.owner] == uid) { "Owner mismatch" }
                    val current = remote?.takeIf { (it["_generation"] ?: "legacy") == generation }
                    val winner = if (current != null && newer(current, row)) defaults(t) + current else row
                    tx.set(ref, winner + mapOf("_generation" to generation, "sincronizadoFirebase" to true))
                    if (t.name == "articulos" && winner["stockBase"] == null) winner + ("stockBase" to winner["stock"]) else winner
                }.await()
                apply(t, uid, winner, row)
            }
            // Fetch every record's revision, including tombstones. Counts cannot detect edits.
            if (RecoveryGate(app).canPull(uid)) {
                val docs = fs.collection(t.remote).whereEqualTo(t.owner, uid).get(Source.SERVER).await()
                for (doc in docs.documents) {
                    if (!DemoAccess.canSync(uid)) return@withLock
                    val raw = doc.data ?: continue
                    if ((raw["_generation"] ?: "legacy") != generation) continue
                    val row = defaults(t).toMutableMap().apply { putAll(raw) }
                    row["lastModified"] = (raw["lastModified"] as? Number)?.toLong() ?: 0L
                    if (t.name == "articulos" && raw["stockBase"] == null) row["stockBase"] = raw["stock"]
                    // Support earlier expense IDs without fabricating missing data.
                    if (t.name == "LineasGastos" && row["numeroLinea"].toString().isBlank()) row["numeroLinea"] = raw["idLineaGasto"] ?: continue
                    if (t.keys.any { row[it].toString().isBlank() }) continue
                    if (t.name == "mercadillos") {
                        val day = CalendarDates.epochDay(row["fecha"].toString())
                        row["fechaEpochDay"] = day
                        if (day != null) row["fecha"] = CalendarDates.display(day)
                    }
                    if (t.name == "saldos_guardados") {
                        val day = CalendarDates.epochDay(row["fechaMercadillo"].toString())
                        row["fechaEpochDay"] = day
                        if (day != null) row["fechaMercadillo"] = CalendarDates.display(day)
                    }
                    apply(t, uid, row)
                }
            }
        }
        syncStock(uid, generation)
        // Recompute from the same baseline + union of movements on every device.
        db.withTransaction {
            val sql = db.openHelper.writableDatabase
            sql.execSQL("UPDATE articulos SET stock = stockBase - COALESCE((SELECT SUM(cantidad) FROM stock_movements m WHERE m.idUsuario = articulos.userId AND m.idArticulo = articulos.idArticulo AND m.aplicado = 1 AND m.incorporadoEnBase = 0),0) WHERE userId = ? AND stockBase IS NOT NULL", arrayOf(uid))
        }
        db.invalidationTracker.refreshVersionsAsync()
    } }
    // Concilia el perfil y publica el estado de la fila realmente guardada.
    private suspend fun syncProfile(uid: String) {
        val t = Table("usuarios", "usuarios", "uid", listOf("uid"), UserEntity::class.java)
        val local = rows(t, uid).firstOrNull()
        val ref = fs.collection("usuarios").document(uid)
        val winner = fs.runTransaction { tx ->
            check(DemoAccess.canSync(uid))
            val snap = tx.get(ref)
            val raw = snap.data
            val remote = raw?.let { defaults(t) + it + mapOf("uid" to uid, "lastModified" to ((it["lastModified"] as? Number)?.toLong() ?: 0L)) }
            val selected = if (local != null && (remote == null || newer(local, remote))) local else remote ?: return@runTransaction null
            val result = selected + mapOf("sincronizadoFirebase" to true)
            tx.set(ref, result)
            result
        }.await() ?: return
        if (!DemoAccess.canSync(uid)) return
        apply(t, uid, winner, local)
        // apply() may reject an outdated snapshot; publish the actual committed row, not winner.
        db.userDao().getUserById(uid)?.let { account ->
            ConfigurationManager.applyAccountFromRoom(uid, account.esPremium, account.email, account.displayName)
        }
    }
    // Concilia movimientos de stock de la generación activa y reconstruye existencias.
    private suspend fun syncStock(uid: String, generation: String) {
        val dao = db.stockMovementDao()
        for (m in dao.allForOwner(uid)) {
            if (!DemoAccess.canSync(uid)) return
            if (m.sincronizadoFirebase) continue
            val ref = fs.collection("stock_movements").document(m.idMovimiento)
            fs.runTransaction { tx ->
                val control = tx.get(fs.collection("account_data_state").document(uid))
                check((control.getString("generation") ?: "legacy") == generation && control.getString("deleting") == null)
                val old = tx.get(ref)
                check(!old.exists() || old.getString("idUsuario") == uid)
                tx.set(ref, mapOf("idMovimiento" to m.idMovimiento, "idUsuario" to uid, "idRecibo" to m.idRecibo, "idArticulo" to m.idArticulo, "cantidad" to m.cantidad, "creadoEn" to m.creadoEn, "_generation" to generation))
            }.await()
            dao.markUploaded(m.idMovimiento, uid)
        }
        if (!RecoveryGate(app).canPull(uid)) return
        val docs = fs.collection("stock_movements").whereEqualTo("idUsuario", uid).get(Source.SERVER).await()
        for (doc in docs.documents) {
            if ((doc.getString("_generation") ?: "legacy") != generation || !DemoAccess.canSync(uid)) continue
            val m = StockMovementEntity(idMovimiento = doc.id, idUsuario = uid, idRecibo = doc.getString("idRecibo") ?: "", idArticulo = doc.getString("idArticulo") ?: continue, cantidad = (doc.getLong("cantidad") ?: continue).toInt(), creadoEn = doc.getLong("creadoEn") ?: 0, sincronizadoFirebase = true)
            dao.enqueue(listOf(m))
        }
        check(StockMovementProcessor(db).processPending(uid)) { "Stock still pending" }
    }
}
