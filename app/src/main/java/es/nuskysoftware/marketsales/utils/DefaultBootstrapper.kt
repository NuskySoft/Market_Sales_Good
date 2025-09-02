package es.nuskysoftware.marketsales.utils

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.*
import java.lang.Exception

/**
 * Bootstrap ligero para el usuario "usuario_default".
 * Se ejecuta solo si Room está vacío PARA ese usuario.
 * No toca AuthRepository.
 */
object DefaultBootstrapper {

    private const val TAG = "DefaultBootstrapper"

    suspend fun runIfNeeded(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val uid = "usuario_default"

            val categoriaDao = db.categoriaDao()
            val articuloDao = db.articuloDao()
            val mercadilloDao = db.mercadilloDao()
            val recibosDao = db.recibosDao()
            val lineasDao = db.lineasVentaDao()

            val empty = !(categoriaDao.existeAlgunoDeUsuario(uid)
                    || articuloDao.existeAlgunoDeUsuario(uid)
                    || mercadilloDao.existeAlgunoDeUsuario(uid)
                    || recibosDao.existeAlgunoDeUsuario(uid)
                    || lineasDao.existeAlgunoDeUsuario(uid))

            if (!empty) {
                Log.d(TAG, "Room NO está vacío para $uid — no se hace bootstrap")
                return
            }

            Log.i(TAG, "Room vacío para $uid — intentando bootstrap desde Firebase…")

            val fs = FirebaseFirestore.getInstance()

//            val catsSnap = fs.collection("categorias").whereEqualTo("userId", uid).get().await()
//            val artsSnap = fs.collection("articulos").whereEqualTo("userId", uid).get().await()
//            val mercSnap = fs.collection("mercadillos").whereEqualTo("userId", uid).get().await()
//            val recsSnap = fs.collection("recibos").whereEqualTo("idUsuario", uid).get().await()
//            val linsSnap = fs.collection("lineas_venta").whereEqualTo("idUsuario", uid).get().await()
            val catsSnap = fs.collection("categorias")
                .whereEqualTo("userId", uid)
                .whereEqualTo("activa", true)
                .get().await()
            val artsSnap = fs.collection("articulos")
                .whereEqualTo("userId", uid)
                .whereEqualTo("activo", true)
                .get().await()
            val mercSnap = fs.collection("mercadillos")
                .whereEqualTo("userId", uid)
                .whereEqualTo("activo", true)
                .get().await()
            val recsSnap = fs.collection("recibos")
                .whereEqualTo("idUsuario", uid)
                .whereEqualTo("activo", true)
                .get().await()
            val linsSnap = fs.collection("lineas_venta")
                .whereEqualTo("idUsuario", uid)
                .whereEqualTo("activo", true)
                .get().await()

            val categorias = catsSnap.documents.mapNotNull { it.toCategoria(uid) }
            val articulos  = artsSnap.documents.mapNotNull { it.toArticulo(uid) }
            val mercas     = mercSnap.documents.mapNotNull { it.toMercadillo(uid) }
            val recibos    = recsSnap.documents.mapNotNull { it.toRecibo(uid) }
            val lineas     = linsSnap.documents.mapNotNull { snap ->
                val idRecibo = (snap.data?.get("idRecibo") as? String).orEmpty()
                snap.toLinea(uid, idRecibo)
            }

            db.withTransaction {
                categoriaDao.upsertAll(categorias)
                articuloDao.upsertAll(articulos)
                mercadilloDao.upsertAll(mercas)
                recibosDao.upsertAll(recibos)
                lineasDao.upsertAll(lineas)

                categorias.forEach { categoriaDao.marcarComoSincronizada(it.idCategoria) }
                articulos.forEach  { articuloDao.marcarComoSincronizado(it.idArticulo) }
                mercas.forEach     { mercadilloDao.marcarComoSincronizado(it.idMercadillo) }
            }

            Log.i(TAG, "Bootstrap DEFAULT completado: cats=${categorias.size}, arts=${articulos.size}, merc=${mercas.size}, rec=${recibos.size}, lin=${lineas.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error en bootstrap default", e)
        }
    }
}

/* ====== Mappers locales (idénticos a los usados en AuthRepository) ====== */

private fun Map<String, Any?>.getString(key: String, def: String = ""): String =
    (this[key] as? String) ?: def
private fun Map<String, Any?>.getDouble(key: String, def: Double = 0.0): Double =
    when (val v = this[key]) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: def; else -> def }
private fun Map<String, Any?>.getInt(key: String, def: Int = 0): Int =
    when (val v = this[key]) { is Number -> v.toInt(); is String -> v.toIntOrNull() ?: def; else -> def }
private fun Map<String, Any?>.getBool(key: String, def: Boolean = false): Boolean =
    when (val v = this[key]) { is Boolean -> v; is Number -> v.toInt()!=0; is String -> v=="true"; else -> def }
private fun Map<String, Any?>.getLong(key: String, def: Long = System.currentTimeMillis()): Long =
    when (val v = this[key]) { is Number -> v.toLong(); is String -> v.toLongOrNull() ?: def; else -> def }

private fun com.google.firebase.firestore.DocumentSnapshot.toCategoria(uid: String): CategoriaEntity? {
    val m = this.data ?: return null
    return CategoriaEntity(
        idCategoria = m.getString("idCategoria", this.id),
        userId = uid,
        nombre = m.getString("nombre"),
        colorHex = m.getString("colorHex", "#FFFFFF"),
        orden = m.getInt("orden", 0),
        activa = m.getBool("activa", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}
private fun com.google.firebase.firestore.DocumentSnapshot.toArticulo(uid: String): ArticuloEntity? {
    val m = this.data ?: return null
    return ArticuloEntity(
        idArticulo = m.getString("idArticulo", this.id),
        userId = uid,
        nombre = m.getString("nombre"),
        idCategoria = m.getString("idCategoria"),
        precioVenta = m.getDouble("precioVenta", 0.0),
        precioCoste = (m["precioCoste"] as? Number)?.toDouble(),
        stock = (m["stock"] as? Number)?.toInt(),
        controlarStock = m.getBool("controlarStock", false),
        controlarCoste = m.getBool("controlarCoste", false),
        favorito = m.getBool("favorito", false),
        fotoUri = m["fotoUri"] as? String,
        activo = m.getBool("activo", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}
private fun com.google.firebase.firestore.DocumentSnapshot.toMercadillo(uid: String): MercadilloEntity? {
    val m = this.data ?: return null
    return MercadilloEntity(
        idMercadillo = m.getString("idMercadillo", this.id),
        userId = uid,
        fecha = m.getString("fecha"),
        lugar = m.getString("lugar"),
        organizador = m.getString("organizador"),
        esGratis = m.getBool("esGratis", true),
        importeSuscripcion = m.getDouble("importeSuscripcion", 0.0),
        requiereMesa = m.getBool("requiereMesa", true),
        requiereCarpa = m.getBool("requiereCarpa", true),
        hayPuntoLuz = m.getBool("hayPuntoLuz", false),
        horaInicio = m.getString("horaInicio", "09:00"),
        horaFin = m.getString("horaFin", "14:00"),
        estado = m.getInt("estado", 1),
        pendienteArqueo = m.getBool("pendienteArqueo", false),
        pendienteAsignarSaldo = m.getBool("pendienteAsignarSaldo", false),
        saldoInicial = (m["saldoInicial"] as? Number)?.toDouble(),
        saldoFinal = (m["saldoFinal"] as? Number)?.toDouble(),
        arqueoCaja = (m["arqueoCaja"] as? Number)?.toDouble(),
        totalVentas = m.getDouble("totalVentas", 0.0),
        totalGastos = m.getDouble("totalGastos", 0.0),
        arqueoMercadillo = (m["arqueoMercadillo"] as? Number)?.toDouble(),
        activo = m.getBool("activo", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}
private fun com.google.firebase.firestore.DocumentSnapshot.toRecibo(uid: String): ReciboEntity? {
    val m = this.data ?: return null
    return ReciboEntity(
        idRecibo = m.getString("idRecibo", this.id),
        idMercadillo = m.getString("idMercadillo"),
        idUsuario = uid,
        fechaHora = m.getLong("fechaHora", System.currentTimeMillis()),
        metodoPago = m.getString("metodoPago", "efectivo"),
        totalTicket = m.getDouble("totalTicket", 0.0),
        estado = m.getString("estado", "COMPLETADO"),
        activo = m.getBool("activo", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}
private fun com.google.firebase.firestore.DocumentSnapshot.toLinea(uid: String, idRecibo: String): LineaVentaEntity? {
    val m = this.data ?: return null
    return LineaVentaEntity(
        idLinea = m.getString("idLinea", this.id),
        idRecibo = idRecibo,
        idMercadillo = m.getString("idMercadillo"),
        idUsuario = uid,
        numeroLinea = m.getInt("numeroLinea", 1),
        tipoLinea = m.getString("tipoLinea", "producto"),
        descripcion = m.getString("descripcion"),
        idProducto = m["idProducto"] as? String,
        cantidad = m.getInt("cantidad", 1),
        precioUnitario = m.getDouble("precioUnitario", 0.0),
        subtotal = m.getDouble("subtotal", 0.0),
        idLineaOriginalAbonada = m["idLineaOriginalAbonada"] as? String,
        activo = m.getBool("activo", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}
