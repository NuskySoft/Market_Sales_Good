// app/src/main/java/es/nuskysoftware/marketsales/utils/FullBootstrapper.kt
package es.nuskysoftware.marketsales.utils

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.*

/**
 * Bootstrap para poblar **TODAS** las tablas de Room desde Firebase
 * cuando la base local está vacía para el usuario indicado.
 *
 * Tablas: usuarios, configuracion, categorias, articulos,
 * mercadillos, recibos, lineas_venta, lineas_gasto, saldos_guardados.
 *
 * Política: lo descargado entra con sincronizadoFirebase=true (si el entity lo tiene).
 */
object FullBootstrapper {

    private const val TAG = "FullBootstrapper"

    suspend fun runIfNeeded(context: Context, rawUserId: String?): Boolean {
        val uid = (rawUserId ?: "").ifBlank { "usuario_default" }

        val db = AppDatabase.getDatabase(context)
        val categoriaDao   = db.categoriaDao()
        val articuloDao    = db.articuloDao()
        val mercadilloDao  = db.mercadilloDao()
        val recibosDao     = db.recibosDao()
        val lineasVentaDao = db.lineasVentaDao()
        val lineasGastosDao= db.lineasGastosDao()
        val configDao      = db.configuracionDao()
        val userDao        = db.userDao()
        val saldosDao      = db.saldoGuardadoDao()

        // ¿Room vacío para este usuario?
        val vacio = !(
                categoriaDao.existeAlgunoDeUsuario(uid) ||
                        articuloDao.existeAlgunoDeUsuario(uid)  ||
                        mercadilloDao.existeAlgunoDeUsuario(uid)||
                        recibosDao.existeAlgunoDeUsuario(uid)   ||
                        lineasVentaDao.existeAlgunoDeUsuario(uid) ||
                        (saldosDao.getUltimoNoConsumido(uid) != null) ||
                        userDao.existeUsuario(uid) ||
                        configDao.existeConfiguracion()
                )

        if (!vacio) {
            Log.d(TAG, "Room NO está vacío para $uid → no bootstrap")
            return false
        }

        Log.i(TAG, "Room vacío para $uid → bootstrap desde Firebase…")
        val fs = FirebaseFirestore.getInstance()

        // ===== Descargas por colección =====
        val userSnap  = fs.collection("usuarios").document(uid).get().await()
        val confDoc   = fs.collection("configuracion").document(uid).get().await()
        val confQuery = fs.collection("configuracion").whereEqualTo("userId", uid).get().await()
        val catsSnap  = fs.collection("categorias").whereEqualTo("userId", uid).get().await()
        val artsSnap  = fs.collection("articulos").whereEqualTo("userId", uid).get().await()
        val mercSnap  = fs.collection("mercadillos").whereEqualTo("userId", uid).get().await()
        val recsSnap  = fs.collection("recibos").whereEqualTo("idUsuario", uid).get().await()
        val linsSnap  = fs.collection("lineas_venta").whereEqualTo("idUsuario", uid).get().await()
        val gastoSnap = fs.collection("lineas_gasto").whereEqualTo("idUsuario", uid).get().await()
        val saldoSnap = fs.collection("saldosGuardados").whereEqualTo("idUsuario", uid).get().await()

        // ===== Map a Entities (coincidiendo con tus data classes actuales) =====

        // usuarios
        val usuario: UserEntity? = userSnap.data?.let { m ->
            UserEntity(
                uid = uid,
                email = (m["email"] as? String) ?: "",
                displayName = (m["displayName"] as? String) ?: (m["nombreUsuario"] as? String ?: ""),
                photoUrl = (m["photoUrl"] as? String) ?: "",
                esPremium = (m["esPremium"] as? Boolean) ?: false,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                fechaCreacion = (m["fechaCreacion"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                fechaUltimaSync = (m["fechaUltimaSync"] as? String),
                sincronizadoFirebase = true,
                activo = (m["activo"] as? Boolean) ?: true
            )
        }

        // configuracion (monoregistro con PK fija)
        val configuracion: ConfiguracionEntity = when {
            confDoc.exists() && confDoc.data != null -> {
                val m = confDoc.data!!
                ConfiguracionEntity(
                    moneda = (m["moneda"] as? String) ?: "€ Euro",
                    idioma = (m["idioma"] as? String) ?: "es",
                    fuente = (m["fuente"] as? String) ?: "Montserrat",
                    temaOscuro = (m["temaOscuro"] as? Boolean) ?: false,
                    usuarioLogueado = uid,
                    numeroVersion = (m["numeroVersion"] as? String) ?: "V10.0",
                    ultimoDispositivo = m["ultimoDispositivo"] as? String,
                    fechaUltimaSync = m["fechaUltimaSync"] as? String,
                    version = (m["version"] as? Number)?.toLong() ?: 1L,
                    lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    pendienteSync = (m["pendienteSync"] as? Boolean) ?: false,
                    sincronizadoFirebase = true
                )
            }
            confQuery.documents.isNotEmpty() -> {
                val m = confQuery.documents.first().data!!
                ConfiguracionEntity(
                    moneda = (m["moneda"] as? String) ?: "€ Euro",
                    idioma = (m["idioma"] as? String) ?: "es",
                    fuente = (m["fuente"] as? String) ?: "Montserrat",
                    temaOscuro = (m["temaOscuro"] as? Boolean) ?: false,
                    usuarioLogueado = uid,
                    numeroVersion = (m["numeroVersion"] as? String) ?: "V10.0",
                    ultimoDispositivo = m["ultimoDispositivo"] as? String,
                    fechaUltimaSync = (m["fechaUltimaSync"] as? String),
                    version = (m["version"] as? Number)?.toLong() ?: 1L,
                    lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    pendienteSync = (m["pendienteSync"] as? Boolean) ?: false,
                    sincronizadoFirebase = true
                )
            }
            else -> {
                ConfiguracionEntity(
                    usuarioLogueado = uid,
                    sincronizadoFirebase = true
                )
            }
        }

        // categorias
        val categorias: List<CategoriaEntity> = catsSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            CategoriaEntity(
                idCategoria = (m["idCategoria"] as? String) ?: d.id,
                userId = (m["userId"] as? String) ?: uid,
                nombre = (m["nombre"] as? String) ?: "",
                colorHex = (m["colorHex"] as? String) ?: "#9E9E9E",
                orden = (m["orden"] as? Number)?.toInt() ?: 0,
                activa = (m["activa"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // articulos
        val articulos: List<ArticuloEntity> = artsSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            ArticuloEntity(
                idArticulo = (m["idArticulo"] as? String) ?: d.id,
                userId = (m["userId"] as? String) ?: uid,
                nombre = (m["nombre"] as? String) ?: "",
                idCategoria = (m["idCategoria"] as? String) ?: "",
                precioVenta = (m["precioVenta"] as? Number)?.toDouble()
                    ?: (m["precio"] as? Number)?.toDouble() ?: 0.0,
                precioCoste = (m["precioCoste"] as? Number)?.toDouble(),
                stock = (m["stock"] as? Number)?.toInt(),
                controlarStock = (m["controlarStock"] as? Boolean) ?: false,
                controlarCoste = (m["controlarCoste"] as? Boolean) ?: false,
                favorito = (m["favorito"] as? Boolean) ?: false,
                fotoUri = (m["fotoUri"] as? String),
                activo = (m["activo"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // mercadillos
        val mercadillos: List<MercadilloEntity> = mercSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            MercadilloEntity(
                idMercadillo = (m["idMercadillo"] as? String) ?: d.id,
                userId = (m["userId"] as? String) ?: uid,
                fecha = (m["fecha"] as? String) ?: "",
                lugar = (m["lugar"] as? String) ?: "",
                organizador = (m["organizador"] as? String) ?: "",
                esGratis = (m["esGratis"] as? Boolean) ?: true,
                importeSuscripcion = (m["importeSuscripcion"] as? Number)?.toDouble() ?: 0.0,
                requiereMesa = (m["requiereMesa"] as? Boolean) ?: true,
                requiereCarpa = (m["requiereCarpa"] as? Boolean) ?: true,
                hayPuntoLuz = (m["hayPuntoLuz"] as? Boolean) ?: false,
                horaInicio = (m["horaInicio"] as? String) ?: "09:00",
                horaFin = (m["horaFin"] as? String) ?: "14:00",
                estado = (m["estado"] as? Number)?.toInt() ?: 1,
                pendienteArqueo = (m["pendienteArqueo"] as? Boolean) ?: false,
                pendienteAsignarSaldo = (m["pendienteAsignarSaldo"] as? Boolean) ?: false,
                saldoInicial = (m["saldoInicial"] as? Number)?.toDouble(),
                saldoFinal = (m["saldoFinal"] as? Number)?.toDouble(),
                arqueoCaja = (m["arqueoCaja"] as? Number)?.toDouble(),
                totalVentas = (m["totalVentas"] as? Number)?.toDouble() ?: 0.0,
                totalGastos = (m["totalGastos"] as? Number)?.toDouble() ?: 0.0,
                arqueoMercadillo = (m["arqueoMercadillo"] as? Number)?.toDouble() ?: 0.0,
                activo = (m["activo"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // recibos
        val recibos: List<ReciboEntity> = recsSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            ReciboEntity(
                idRecibo = (m["idRecibo"] as? String) ?: d.id,
                idUsuario = (m["idUsuario"] as? String) ?: uid,
                idMercadillo = (m["idMercadillo"] as? String) ?: "",
                fechaHora = (m["fechaHora"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                metodoPago = (m["metodoPago"] as? String) ?: "EFECTIVO",
                totalTicket = (m["totalTicket"] as? Number)?.toDouble() ?: 0.0,
                estado = (m["estado"] as? String) ?: "COMPLETADO"
            )
        }

        // lineas_venta
        val lineasVenta: List<LineaVentaEntity> = linsSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            LineaVentaEntity(
                idLinea = (m["idLinea"] as? String) ?: d.id,
                idUsuario = (m["idUsuario"] as? String) ?: uid,
                idMercadillo = (m["idMercadillo"] as? String) ?: "",
                idRecibo = (m["idRecibo"] as? String) ?: "",
                numeroLinea = (m["numeroLinea"] as? Number)?.toInt() ?: 1,
                tipoLinea = (m["tipoLinea"] as? String) ?: "VENTA",
                descripcion = (m["descripcion"] as? String) ?: "",
                idProducto = (m["idProducto"] as? String) ?: (m["idArticulo"] as? String ?: ""),
                cantidad = (m["cantidad"] as? Number)?.toInt() ?: 1,
                precioUnitario = (m["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (m["subtotal"] as? Number)?.toDouble()
                    ?: (m["totalLinea"] as? Number)?.toDouble() ?: 0.0
            )
        }

        // lineas_gasto
        // lineas_gasto  (tu entidad exige fechaHora → lo mapeo; si no existe en Firebase, uso now)
        val lineasGasto = gastoSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            LineaGastoEntity(
                numeroLinea = (m["idLinea"] as? String) ?: d.id,
                idUsuario = (m["idUsuario"] as? String) ?: uid,
                idMercadillo = (m["idMercadillo"] as? String) ?: "",
                descripcion = (m["concepto"] as? String) ?: "",
                importe = (m["importe"] as? Number)?.toDouble() ?: 0.0,
                fechaHora = (m["fechaHora"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                formaPago = (m["formaPago"] as? String) ?: "EFECTIVO"
            )
        }

        // saldos_guardados
        val saldosGuardados: List<SaldoGuardadoEntity> = saldoSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            SaldoGuardadoEntity(
                idRegistro = (m["idRegistro"] as? String) ?: d.id,
                idUsuario = (m["idUsuario"] as? String) ?: uid,
                idMercadilloOrigen = (m["idMercadilloOrigen"] as? String) ?: "",
                fechaMercadillo = (m["fechaMercadillo"] as? String) ?: "",
                lugarMercadillo = (m["lugarMercadillo"] as? String) ?: "",
                organizadorMercadillo = (m["organizadorMercadillo"] as? String) ?: "",
                horaInicioMercadillo = (m["horaInicioMercadillo"] as? String) ?: "",
                saldoInicialGuardado = (m["saldoInicialGuardado"] as? Number)?.toDouble() ?: 0.0,
                consumido = (m["consumido"] as? Boolean) ?: false,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true,
                notas = (m["notas"] as? String)
            )
        }

        // ===== Escribir TODO en transacción =====
        db.withTransaction {
            // Usuario → SQL directo para evitar depender del nombre del método en UserDao
            usuario?.let { u: UserEntity ->
                val sql = """
                    INSERT OR REPLACE INTO usuarios
                    (uid, email, displayName, photoUrl, esPremium, version, lastModified, fechaCreacion, fechaUltimaSync, sincronizadoFirebase, activo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                val args = arrayOf(
                    u.uid,
                    u.email,
                    u.displayName,
                    u.photoUrl,
                    if (u.esPremium) 1 else 0,
                    u.version,
                    u.lastModified,
                    u.fechaCreacion,
                    u.fechaUltimaSync,
                    if (u.sincronizadoFirebase) 1 else 0,
                    if (u.activo) 1 else 0
                )
                db.openHelper.writableDatabase.execSQL(sql, args)
            }

            // Configuración (PK fija ⇒ insertar/replace)
            configDao.insertarConfiguracion(configuracion)

            // Resto
            categoriaDao.upsertAll(categorias)
            articuloDao.upsertAll(articulos)
            mercadilloDao.upsertAll(mercadillos)
            recibosDao.upsertAll(recibos)
            lineasVentaDao.upsertAll(lineasVenta)
            lineasGastosDao.insertarLineas(lineasGasto)
            saldosGuardados.forEach { s -> saldosDao.upsert(s) }
        }

        Log.i(
            TAG,
            "Bootstrap OK uid=$uid → cats=${categorias.size}, arts=${articulos.size}, merc=${mercadillos.size}, rec=${recibos.size}, linV=${lineasVenta.size}, linG=${lineasGasto.size}, saldos=${saldosGuardados.size}, conf=1, user=${usuario != null}"
        )
        return true
    }
}




//
//// app/src/main/java/es/nuskysoftware/marketsales/utils/FullBootstrapper.kt
//package es.nuskysoftware.marketsales.utils
//
//import android.content.Context
//import android.util.Log
//import androidx.room.withTransaction
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.tasks.await
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.local.entity.*
//
///**
// * Bootstrap general “offline-first”:
// * - Si Room está vacío para el usuario actual (incluye usuario_default), descarga TODO lo esencial
// *   desde Firebase y lo persiste en Room, marcándolo como sincronizado.
// * - Además, lee el perfil del usuario para ajustar plan/moneda en ConfigurationManager.
// *
// * Tablas cubiertas:
// *   - categorias, articulos, mercadillos, recibos, lineas_venta  (igual que tu DefaultBootstrapper)
// *   - saldosGuardados (añadido ahora, ya que lo usas en Asignar Saldo)
// *   - perfil de usuario (solo para poblar ConfigurationManager.esPremium/moneda, etc.)
// */
//object FullBootstrapper {
//
//    private const val TAG = "FullBootstrapper"
//
//    suspend fun runIfNeeded(context: Context) {
//        val db = AppDatabase.getDatabase(context)
//        val uid = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
//
//        // Comprobamos “vacío” para el uid mirando varias tablas (criterio ligero pero efectivo)
//        val empty = try {
//            val categoriaDao = db.categoriaDao()
//            val articuloDao = db.articuloDao()
//            val mercadilloDao = db.mercadilloDao()
//            val recibosDao = db.recibosDao()
//            val lineasDao = db.lineasVentaDao()
//
//            !(categoriaDao.existeAlgunoDeUsuario(uid)
//                    || articuloDao.existeAlgunoDeUsuario(uid)
//                    || mercadilloDao.existeAlgunoDeUsuario(uid)
//                    || recibosDao.existeAlgunoDeUsuario(uid)
//                    || lineasDao.existeAlgunoDeUsuario(uid))
//        } catch (e: Exception) {
//            Log.w(TAG, "No pude comprobar si Room está vacío; asumo que SÍ lo está", e)
//            true
//        }
//
//        if (!empty) {
//            Log.d(TAG, "Room NO está vacío para $uid — no hago bootstrap.")
//            // Aun así, actualizamos perfil para esPremium si hace falta:
//            try { actualizarPerfilEnConfigurationManager(uid) } catch (_: Exception) {}
//            return
//        }
//
//        Log.i(TAG, "Room vacío para $uid — descargando datos de Firebase…")
//        runFullDownload(context, uid)
//    }
//
//    suspend fun runFullDownload(context: Context, uid: String) {
//        val db = AppDatabase.getDatabase(context)
//        val fs = FirebaseFirestore.getInstance()
//
//        try {
//            // 1) Snapshots remotos
//            val catsSnap = fs.collection("categorias").whereEqualTo("userId", uid).get().await()
//            val artsSnap = fs.collection("articulos").whereEqualTo("userId", uid).get().await()
//            val mercSnap = fs.collection("mercadillos").whereEqualTo("userId", uid).get().await()
//            val recsSnap = fs.collection("recibos").whereEqualTo("idUsuario", uid).get().await()
//            val linsSnap = fs.collection("lineas_venta").whereEqualTo("idUsuario", uid).get().await()
//            val saldoSnap = fs.collection("saldosGuardados").whereEqualTo("idUsuario", uid).get().await()
//
//            // 2) Mapear a entities (reusamos el mismo estilo que ya usas en DefaultBootstrapper)
//            val categorias = catsSnap.documents.mapNotNull { it.toCategoria(uid) }
//            val articulos  = artsSnap.documents.mapNotNull { it.toArticulo(uid) }
//            val mercas     = mercSnap.documents.mapNotNull { it.toMercadillo(uid) }
//            val recibos    = recsSnap.documents.mapNotNull { it.toRecibo(uid) }
//            val lineas     = linsSnap.documents.mapNotNull { d ->
//                val idRecibo = (d.data?.get("idRecibo") as? String).orEmpty()
//                d.toLinea(uid, idRecibo)
//            }
//            val saldos     = saldoSnap.documents.mapNotNull { it.toSaldoGuardado(uid) }
//
//            // 3) Persistir en Room (sincronizado = true)
//            val categoriaDao = db.categoriaDao()
//            val articuloDao = db.articuloDao()
//            val mercadilloDao = db.mercadilloDao()
//            val recibosDao = db.recibosDao()
//            val lineasDao = db.lineasVentaDao()
//            val saldoDao = db.saldoGuardadoDao()
//
//            db.withTransaction {
//                categoriaDao.upsertAll(categorias)
//                articuloDao.upsertAll(articulos)
//                mercadilloDao.upsertAll(mercas)
//                recibosDao.upsertAll(recibos)
//                lineasDao.upsertAll(lineas)
//                if (saldos.isNotEmpty()) saldoDao.upsertAll(saldos)
//
//                // Marcar como sincronizado (como ya haces tú en el default)
//                categorias.forEach { categoriaDao.marcarComoSincronizada(it.idCategoria) }
//                articulos.forEach  { articuloDao.marcarComoSincronizado(it.idArticulo) }
//                mercas.forEach     { mercadilloDao.marcarComoSincronizado(it.idMercadillo) }
//                saldos.forEach     { saldoDao.marcarSincronizado(it.idRegistro) }
//            }
//
//            Log.i(TAG, "Bootstrap COMPLETO: cats=${categorias.size}, arts=${articulos.size}, merc=${mercas.size}, rec=${recibos.size}, lin=${lineas.size}, saldos=${saldos.size}")
//
//            // 4) Perfil → ConfigurationManager (plan/moneda/esPremium)
//            actualizarPerfilEnConfigurationManager(uid)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error en bootstrap", e)
//            throw e
//        }
//    }
//
//    // -------- Perfil / plan → ConfigurationManager --------
//
//    private suspend fun actualizarPerfilEnConfigurationManager(uid: String) {
//        try {
//            val fs = FirebaseFirestore.getInstance()
//            val userDoc = fs.collection("usuarios").document(uid).get().await()
//            if (!userDoc.exists()) {
//                // Usuario default o perfil sin doc → conserva valores actuales
//                return
//            }
//            val data = userDoc.data.orEmpty()
//            val plan = (data["planUsuario"] as? String)?.uppercase() ?: "FREE"
//            val moneda = (data["moneda"] as? String) ?: ConfigurationManager.moneda.value
//            val displayName = data["displayName"] as? String
//            val email = data["email"] as? String
//            val isAuth = uid != "usuario_default"
//
//            // Aplica a ConfigurationManager (tu método ya calcula esPremium a partir del plan)
//            ConfigurationManager.updateUserConfiguration(
//                usuarioId = uid,
//                usuarioEmail = email,
//                displayName = displayName,
//                planUsuario = plan,
//                isAuthenticated = isAuth,
//                moneda = moneda
//            )
//        } catch (e: Exception) {
//            Log.w(TAG, "No se pudo actualizar perfil/plan en ConfigurationManager", e)
//        }
//    }
//}
//
///* ====== MAPPERS (idénticos a los del DefaultBootstrapper + saldo guardado) ====== */
//
//private fun Map<String, Any?>.getString(key: String, def: String = ""): String =
//    (this[key] as? String) ?: def
//private fun Map<String, Any?>.getDouble(key: String, def: Double = 0.0): Double =
//    when (val v = this[key]) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: def; else -> def }
//private fun Map<String, Any?>.getInt(key: String, def: Int = 0): Int =
//    when (val v = this[key]) { is Number -> v.toInt(); is String -> v.toIntOrNull() ?: def; else -> def }
//private fun Map<String, Any?>.getBool(key: String, def: Boolean = false): Boolean =
//    when (val v = this[key]) { is Boolean -> v; is Number -> v.toInt()!=0; is String -> v=="true"; else -> def }
//private fun Map<String, Any?>.getLong(key: String, def: Long = System.currentTimeMillis()): Long =
//    when (val v = this[key]) { is Number -> v.toLong(); is String -> v.toLongOrNull() ?: def; else -> def }
//
//// Estos 4 mappers son los mismos que ya usas en DefaultBootstrapper (copiados 1:1)
//private fun com.google.firebase.firestore.DocumentSnapshot.toCategoria(uid: String): CategoriaEntity? {
//    val m = this.data ?: return null
//    return CategoriaEntity(
//        idCategoria = m.getString("idCategoria", this.id),
//        userId = uid,
//        nombre = m.getString("nombre"),
//        colorHex = m.getString("colorHex", "#FFFFFF"),
//        orden = m.getInt("orden", 0),
//        activa = m.getBool("activa", true),
//        version = m.getLong("version", 1),
//        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
//        sincronizadoFirebase = true
//    )
//}
//
//private fun com.google.firebase.firestore.DocumentSnapshot.toArticulo(uid: String): ArticuloEntity? {
//    val m = this.data ?: return null
//    return ArticuloEntity(
//        idArticulo = m.getString("idArticulo", this.id),
//        userId = uid,
//        nombre = m.getString("nombre"),
//        idCategoria = m.getString("idCategoria"),
//        precioVenta = m.getDouble("precioVenta", 0.0),
//        precioCoste = (m["precioCoste"] as? Number)?.toDouble(),
//        stock = (m["stock"] as? Number)?.toInt(),
//        controlarStock = m.getBool("controlarStock", false),
//        controlarCoste = m.getBool("controlarCoste", false),
//        favorito = m.getBool("favorito", false),
//        fotoUri = m["fotoUri"] as? String,
//        activo = m.getBool("activo", true),
//        version = m.getLong("version", 1),
//        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
//        sincronizadoFirebase = true
//    )
//}
//
//private fun com.google.firebase.firestore.DocumentSnapshot.toMercadillo(uid: String): MercadilloEntity? {
//    val m = this.data ?: return null
//    return MercadilloEntity(
//        idMercadillo = m.getString("idMercadillo", this.id),
//        userId = uid,
//        fecha = m.getString("fecha"),
//        lugar = m.getString("lugar"),
//        organizador = m.getString("organizador"),
//        esGratis = m.getBool("esGratis", true),
//        importeSuscripcion = m.getDouble("importeSuscripcion", 0.0),
//        requiereMesa = m.getBool("requiereMesa", true),
//        requiereCarpa = m.getBool("requiereCarpa", true),
//        hayPuntoLuz = m.getBool("hayPuntoLuz", false),
//        horaInicio = m.getString("horaInicio", "09:00"),
//        horaFin = m.getString("horaFin", "14:00"),
//        estado = m.getInt("estado", 1),
//        pendienteArqueo = m.getBool("pendienteArqueo", false),
//        pendienteAsignarSaldo = m.getBool("pendienteAsignarSaldo", false),
//        saldoInicial = (m["saldoInicial"] as? Number)?.toDouble(),
//        saldoFinal = (m["saldoFinal"] as? Number)?.toDouble(),
//        arqueoCaja = (m["arqueoCaja"] as? Number)?.toDouble(),
//        totalVentas = m.getDouble("totalVentas", 0.0),
//        totalGastos = m.getDouble("totalGastos", 0.0),
//        arqueoMercadillo = (m["arqueoMercadillo"] as? Number)?.toDouble(),
//        activo = m.getBool("activo", true),
//        version = m.getLong("version", 1),
//        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
//        sincronizadoFirebase = true
//    )
//}
//
//private fun com.google.firebase.firestore.DocumentSnapshot.toRecibo(uid: String): ReciboEntity? {
//    val m = this.data ?: return null
//    return ReciboEntity(
//        idRecibo = m.getString("idRecibo", this.id),
//        idMercadillo = m.getString("idMercadillo"),
//        idUsuario = uid,
//        fechaHora = m.getLong("fechaHora", System.currentTimeMillis()),
//        metodoPago = m.getString("metodoPago", "efectivo"),
//        totalTicket = m.getDouble("totalTicket", 0.0),
//        estado = m.getString("estado", "COMPLETADO")
//    )
//}
//
//private fun com.google.firebase.firestore.DocumentSnapshot.toLinea(uid: String, idRecibo: String): LineaVentaEntity? {
//    val m = this.data ?: return null
//    return LineaVentaEntity(
//        idLinea = m.getString("idLinea", this.id),
//        idRecibo = idRecibo,
//        idMercadillo = m.getString("idMercadillo"),
//        idUsuario = uid,
//        numeroLinea = m.getInt("numeroLinea", 1),
//        tipoLinea = m.getString("tipoLinea", "producto"),
//        descripcion = m.getString("descripcion"),
//        idProducto = m["idProducto"] as? String,
//        cantidad = m.getInt("cantidad", 1),
//        precioUnitario = m.getDouble("precioUnitario", 0.0),
//        subtotal = m.getDouble("subtotal", 0.0),
//        idLineaOriginalAbonada = m["idLineaOriginalAbonada"] as? String
//    )
//}
//
//// Nuevo mapper para SaldoGuardadoEntity (tabla control de saldos)
//private fun com.google.firebase.firestore.DocumentSnapshot.toSaldoGuardado(uid: String): SaldoGuardadoEntity? {
//    val m = this.data ?: return null
//    return SaldoGuardadoEntity(
//        idRegistro = m.getString("idRegistro", this.id),
//        idUsuario = uid,
//        idMercadilloOrigen = m.getString("idMercadilloOrigen"),
//        fechaMercadillo = m.getString("fechaMercadillo"),
//        lugarMercadillo = m.getString("lugarMercadillo"),
//        organizadorMercadillo = m.getString("organizadorMercadillo"),
//        horaInicioMercadillo = m.getString("horaInicioMercadillo"),
//        saldoInicialGuardado = m.getDouble("saldoInicialGuardado", 0.0),
//        consumido = m.getBool("consumido", false),
//        version = m.getLong("version", 1),
//        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
//        sincronizadoFirebase = true,
//        notas = m["notas"] as? String
//    )
//}
