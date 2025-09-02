// app/src/main/java/es/nuskysoftware/marketsales/data/repository/VentasRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.LineasVentaDao
import es.nuskysoftware.marketsales.data.local.dao.RecibosDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VentasRepository(context: Context) {

//    private val database: AppDatabase = AppDatabase.getDatabase(context)
//    private val recibosDao: RecibosDao = database.recibosDao()
//    private val lineasVentaDao: LineasVentaDao = database.lineasVentaDao()
//    private val configuracionDao = database.configuracionDao()

    companion object { private const val TAG = "VentasRepository" }

    private val db = AppDatabase.getDatabase(context)
    private val recibosDao: RecibosDao = db.recibosDao()
    private val lineasVentaDao: LineasVentaDao = db.lineasVentaDao()
    private val configuracionDao = db.configuracionDao()



    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val connectivity = ConnectivityObserver(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            connectivity.isConnected.collect { online ->
                if (online) {
                    runCatching { sincronizarPendientes() }
                        .onFailure { Log.w(TAG, "Error sincronizando pendientes", it) }
                }
            }
        }
    }


    /**
     *  * Guarda una venta completa (recibo + líneas) en Room y sincroniza cuando es posible.
     *
     */
    suspend fun guardarVenta(
        idMercadillo: String,
        lineas: List<LineaVentaUI>,
        metodoPago: String,
        total: Double
    ): Result<String> {
        return try {
            //val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            val userId = auth.currentUser?.uid
                ?: ConfigurationManager.usuarioLogueado.value
                ?: "usuario_default"
            val now = System.currentTimeMillis()

            val idiomaCfgPersistido = configuracionDao.getConfiguracionSync()?.idioma
            val idioma2Letras = (idiomaCfgPersistido ?: ConfigurationManager.idioma.value)
                .trim()
                .split('-', '_')
                .firstOrNull()
                ?.take(2)
                ?.uppercase(Locale.ROOT)
                ?: "ES"

            val hora = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(now))
            val idRecibo = "RC$hora-$idioma2Letras"

            // === Siguiente idLinea por mercadillo: MAX(idLinea) + 1 ===
            // Nota: funciona porque idLinea está zero-padded a 4 dígitos.
            val maxId = lineasVentaDao.obtenerMaxIdLineaPorMercadillo(idMercadillo) // p.ej. "0007" o null
            val base = maxId?.toIntOrNull() ?: 0
            fun nextSeq(n: Int) = String.format("%04d", n)

            // --- Construir entidades ---
            val recibo = ReciboEntity(
                idRecibo = idRecibo,
                idMercadillo = idMercadillo,
                idUsuario = userId,
                fechaHora = now,
                metodoPago = metodoPago,
                totalTicket = total,
                estado = "COMPLETADO",
                lastModified = now,
                sincronizadoFirebase = false
            )

            val lineasEntity = lineas.mapIndexed { index, l ->
//                val numeroLineaRecibo = index + 1                         // orden dentro de este recibo
//                val idLineaSecuencial = nextSeq(base + numeroLineaRecibo)  // 0001, 0002, ...
                val numeroLineaRecibo = index + 1
                val idLineaSecuencial = nextSeq(base + numeroLineaRecibo)
                LineaVentaEntity(
                    idLinea = idLineaSecuencial,
                    idRecibo = idRecibo,
                    idMercadillo = idMercadillo,
                    idUsuario = userId,
                    numeroLinea = numeroLineaRecibo,
                    tipoLinea = l.tipoLinea.name, // MANUAL | PRODUCTO
                    descripcion = l.descripcion,
                    idProducto = l.idProducto,
                    cantidad = l.cantidad,
                    precioUnitario = l.precioUnitario,
                    subtotal = l.subtotal,
                    idLineaOriginalAbonada = l.idLineaOriginalAbonada,
                    lastModified = now,
                    sincronizadoFirebase = false
                )
            }

            // --- Persistencia local (offline-first) ---
            recibosDao.insertarRecibo(recibo)
            lineasVentaDao.insertarLineas(lineasEntity)

            // --- Best-effort Firestore (sin romper si falla) ---
            try {
                firestore.collection("recibos")
                    .document(idRecibo)
                    .set(recibo)
                    .await()
                recibosDao.marcarSincronizado(idRecibo, System.currentTimeMillis())

                lineasEntity.forEach { linea ->
                    val docId = "${linea.idMercadillo}_${linea.idLinea}" // único por mercadillo+línea (tu formato anterior)
                    firestore.collection("lineas_venta")
                        .document(docId)
                        .set(linea)
                        .await()
                }
            } catch (e: Exception) {
                // Silencioso: ya quedó persistido en Room
//                println("Error subiendo a Firestore: ${e.message}")
                Log.w(TAG, "Error subiendo a Firebase", e)
            }

            Result.success(idRecibo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun obtenerVentasMercadillo(idMercadillo: String): Flow<List<ReciboEntity>> =
        recibosDao.obtenerRecibosPorMercadillo(idMercadillo)

    fun obtenerLineasVenta(idRecibo: String): Flow<List<LineaVentaEntity>> =
        lineasVentaDao.obtenerLineasPorRecibo(idRecibo)

    /**
     * Crea un abono por la línea original indicada.
     * Aplica las mismas reglas de idRecibo e idLinea que una venta normal.
     */
    suspend fun crearAbono(
        lineaOriginal: LineaVentaEntity,
        cantidadAbonar: Int,
        idMercadillo: String
    ): Result<String> {
        return try {
            //val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            val userId = auth.currentUser?.uid
                ?: ConfigurationManager.usuarioLogueado.value
                ?: "usuario_default"
            val now = System.currentTimeMillis()

            val idiomaCfgPersistido = configuracionDao.getConfiguracionSync()?.idioma
            val idioma2Letras = (idiomaCfgPersistido ?: ConfigurationManager.idioma.value)
                .trim()
                .split('-', '_')
                .firstOrNull()
                ?.take(2)
                ?.uppercase(Locale.ROOT)
                ?: "ES"

            val hora = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(now))
            val idRecibo = "RC$hora-$idioma2Letras"

            val maxId = lineasVentaDao.obtenerMaxIdLineaPorMercadillo(idMercadillo)
            val base = maxId?.toIntOrNull() ?: 0
            fun nextSeq(n: Int) = String.format("%04d", n)

            val reciboAbono = ReciboEntity(
                idRecibo = idRecibo,
                idMercadillo = idMercadillo,
                idUsuario = userId,
                fechaHora = now,
                metodoPago = "ABONO", // Recibo de abono
                totalTicket = -(lineaOriginal.precioUnitario * cantidadAbonar),
                estado = "COMPLETADO",
                lastModified = now,
                sincronizadoFirebase = false
            )

            val lineaAbono = LineaVentaEntity(
                idLinea = nextSeq(base + 1),
                idRecibo = idRecibo,
                idMercadillo = idMercadillo,
                idUsuario = userId,
                numeroLinea = 1,
                tipoLinea = lineaOriginal.tipoLinea, // respeta el tipo original (MANUAL/PRODUCTO)
                descripcion = "ABONO: ${lineaOriginal.descripcion}",
                idProducto = lineaOriginal.idProducto,
                cantidad = -cantidadAbonar,
                precioUnitario = lineaOriginal.precioUnitario,
                subtotal = -(lineaOriginal.precioUnitario * cantidadAbonar),
                idLineaOriginalAbonada = lineaOriginal.idLinea,
                lastModified = now,
                sincronizadoFirebase = false
            )

            recibosDao.insertarRecibo(reciboAbono)
            lineasVentaDao.insertarLinea(lineaAbono)

            try {
                firestore.collection("recibos")
                    .document(idRecibo)
                    .set(reciboAbono)
                    .await()
                recibosDao.marcarSincronizado(idRecibo, System.currentTimeMillis())

                val docId = "${lineaAbono.idMercadillo}_${lineaAbono.idLinea}"
                firestore.collection("lineas_venta")
                    .document(docId)
                    .set(lineaAbono)
                    .await()
            } catch (e: Exception) {
                // Silencioso: ya quedó persistido en Room
                println("Error subiendo abono a Firestore: ${e.message}")
                Log.w(TAG, "Error subiendo abono a Firebase", e)
            }

            Result.success(idRecibo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    private suspend fun sincronizarPendientes() {
        val uid = auth.currentUser?.uid ?: return
        val recibosPend = recibosDao.getPendientesSync(uid)
        recibosPend.forEach { r ->
            runCatching {
                firestore.collection("recibos").document(r.idRecibo).set(r).await()
                recibosDao.marcarSincronizado(r.idRecibo, System.currentTimeMillis())
            }
        }
        val lineasPend = lineasVentaDao.getPendientesSync(uid)
        lineasPend.forEach { l ->
            runCatching {
                val docId = "${l.idMercadillo}_${l.idLinea}"
                firestore.collection("lineas_venta").document(docId).set(l).await()
                lineasVentaDao.marcarSincronizado(l.idMercadillo, l.idLinea, System.currentTimeMillis())
            }
        }
    }
}
data class LineaVentaUI(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tipoLinea: TipoLinea,
    val descripcion: String,
    val idProducto: String? = null,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val idLineaOriginalAbonada: String? = null
)

enum class TipoLinea { MANUAL, PRODUCTO }
enum class MetodoPago { EFECTIVO, BIZUM, TARJETA }
enum class PestanaVenta { MANUAL, PRODUCTOS }

//// app/src/main/java/es/nuskysoftware/marketsales/data/repository/VentasRepository.kt
//package es.nuskysoftware.marketsales.data.repository
//
//import android.content.Context
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import es.nuskysoftware.marketsales.data.local.dao.LineasVentaDao
//import es.nuskysoftware.marketsales.data.local.dao.RecibosDao
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
//import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.tasks.await
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.UUID
//
//class VentasRepository(context: Context) {
//
//    private val database: AppDatabase = AppDatabase.getDatabase(context)
//    private val recibosDao: RecibosDao = database.recibosDao()
//    private val lineasVentaDao: LineasVentaDao = database.lineasVentaDao()
//    private val configuracionDao = database.configuracionDao()
//
//    private val firestore = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//
//    /**
//     * Guarda una venta completa (recibo + líneas) en Room (offline-first) y
//     * hace un best-effort de subida a Firestore.
//     *
//     * Reglas:
//     * - idRecibo = "RC" + HHmmss + "-" + <IDIOMA_APP_MAYUS>  (idioma tomado de ConfiguracionEntity)
//     * - idLinea  = contador "0001", "0002", ... que SE REINICIA por mercadillo.
//     *             Se calcula como max(idLinea) del mercadillo + 1 (zero-padded 4 dígitos).
//     */
//    suspend fun guardarVenta(
//        idMercadillo: String,
//        lineas: List<LineaVentaUI>,
//        metodoPago: String,
//        total: Double
//    ): Result<String> {
//        return try {
//            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
//            val now = System.currentTimeMillis()
//
//            // === IDIOMA desde la configuración de la APP (no del dispositivo) ===
//            // Intento 1: DAO (persistido). Si por cualquier motivo es null, uso el flujo en memoria del ConfigurationManager.
//            val idiomaCfgPersistido = configuracionDao.getConfiguracionSync()?.idioma
//            val idioma2Letras = (idiomaCfgPersistido ?: ConfigurationManager.idioma.value)
//                .trim()
//                .split('-', '_')
//                .firstOrNull()
//                ?.take(2)
//                ?.uppercase(Locale.ROOT)
//                ?: "ES"
//
//            // === idRecibo en formato RChhmmss-ES ===
//            val hora = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(now))
//            val idRecibo = "RC$hora-$idioma2Letras"
//
//            // === Siguiente idLinea por mercadillo: MAX(idLinea) + 1 ===
//            // Nota: funciona porque idLinea está zero-padded a 4 dígitos.
//            val maxId = lineasVentaDao.obtenerMaxIdLineaPorMercadillo(idMercadillo) // p.ej. "0007" o null
//            val base = maxId?.toIntOrNull() ?: 0
//            fun nextSeq(n: Int) = String.format("%04d", n)
//
//            // --- Construir entidades ---
//            val recibo = ReciboEntity(
//                idRecibo = idRecibo,
//                idMercadillo = idMercadillo,
//                idUsuario = userId,
//                fechaHora = now,
//                metodoPago = metodoPago,
//                totalTicket = total,
//                estado = "COMPLETADO"
//            )
//
//            val lineasEntity = lineas.mapIndexed { index, l ->
//                val numeroLineaRecibo = index + 1                      // orden dentro de este recibo
//                val idLineaSecuencial = nextSeq(base + numeroLineaRecibo) // 0001, 0002, ...
//                LineaVentaEntity(
//                    idLinea = idLineaSecuencial,
//                    idRecibo = idRecibo,
//                    idMercadillo = idMercadillo,
//                    idUsuario = userId,
//                    numeroLinea = numeroLineaRecibo,
//                    tipoLinea = l.tipoLinea.name,
//                    descripcion = l.descripcion,
//                    idProducto = l.idProducto,
//                    cantidad = l.cantidad,
//                    precioUnitario = l.precioUnitario,
//                    subtotal = l.subtotal,
//                    idLineaOriginalAbonada = l.idLineaOriginalAbonada
//                )
//            }
//
//            // --- Persistencia local (offline-first) ---
//            recibosDao.insertarRecibo(recibo)
//            lineasVentaDao.insertarLineas(lineasEntity)
//
//            // --- Best-effort Firestore (sin romper si falla) ---
//            try {
//                firestore.collection("recibos")
//                    .document(idRecibo)
//                    .set(recibo)
//                    .await()
//
//                lineasEntity.forEach { linea ->
//                    val docId = "${linea.idMercadillo}_${linea.idLinea}" // único por mercadillo+línea
//                    firestore.collection("lineas_venta")
//                        .document(docId)
//                        .set(linea)
//                        .await()
//                }
//            } catch (e: Exception) {
//                // Silencioso: ya quedó persistido en Room
//                println("Error subiendo a Firestore: ${e.message}")
//            }
//
//            Result.success(idRecibo)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    fun obtenerVentasMercadillo(idMercadillo: String): Flow<List<ReciboEntity>> =
//        recibosDao.obtenerRecibosPorMercadillo(idMercadillo)
//
//    fun obtenerLineasVenta(idRecibo: String): Flow<List<LineaVentaEntity>> =
//        lineasVentaDao.obtenerLineasPorRecibo(idRecibo)
//
//    /**
//     * Crea un abono por la línea original indicada.
//     * Aplica las mismas reglas de idRecibo e idLinea que una venta normal.
//     */
//    suspend fun crearAbono(
//        lineaOriginal: LineaVentaEntity,
//        cantidadAbonar: Int,
//        idMercadillo: String
//    ): Result<String> {
//        return try {
//            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
//            val now = System.currentTimeMillis()
//
//            val idiomaCfgPersistido = configuracionDao.getConfiguracionSync()?.idioma
//            val idioma2Letras = (idiomaCfgPersistido ?: ConfigurationManager.idioma.value)
//                .trim()
//                .split('-', '_')
//                .firstOrNull()
//                ?.take(2)
//                ?.uppercase(Locale.ROOT)
//                ?: "ES"
//
//            val hora = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(now))
//            val idRecibo = "RC$hora-$idioma2Letras"
//
//            val maxId = lineasVentaDao.obtenerMaxIdLineaPorMercadillo(idMercadillo)
//            val base = maxId?.toIntOrNull() ?: 0
//            fun nextSeq(n: Int) = String.format("%04d", n)
//
//            val reciboAbono = ReciboEntity(
//                idRecibo = idRecibo,
//                idMercadillo = idMercadillo,
//                idUsuario = userId,
//                fechaHora = now,
//                metodoPago = "ABONO",
//                totalTicket = -(lineaOriginal.precioUnitario * cantidadAbonar),
//                estado = "COMPLETADO"
//            )
//
//            val lineaAbono = LineaVentaEntity(
//                idLinea = nextSeq(base + 1),
//                idRecibo = idRecibo,
//                idMercadillo = idMercadillo,
//                idUsuario = userId,
//                numeroLinea = 1,
//                tipoLinea = lineaOriginal.tipoLinea,
//                descripcion = "ABONO: ${lineaOriginal.descripcion}",
//                idProducto = lineaOriginal.idProducto,
//                cantidad = -cantidadAbonar,
//                precioUnitario = lineaOriginal.precioUnitario,
//                subtotal = -(lineaOriginal.precioUnitario * cantidadAbonar),
//                idLineaOriginalAbonada = lineaOriginal.idLinea
//            )
//
//            recibosDao.insertarRecibo(reciboAbono)
//            lineasVentaDao.insertarLinea(lineaAbono)
//
//            try {
//                firestore.collection("recibos")
//                    .document(idRecibo)
//                    .set(reciboAbono)
//                    .await()
//
//                val docId = "${lineaAbono.idMercadillo}_${lineaAbono.idLinea}"
//                firestore.collection("lineas_venta")
//                    .document(docId)
//                    .set(lineaAbono)
//                    .await()
//            } catch (e: Exception) {
//                println("Error subiendo abono a Firestore: ${e.message}")
//            }
//
//            Result.success(idRecibo)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}
//
//// =====================
//// Modelos de UI (igual que tenías)
//// =====================
//data class LineaVentaUI(
//    val id: String = UUID.randomUUID().toString(),
//    val tipoLinea: TipoLinea,
//    val descripcion: String,
//    val idProducto: String? = null,
//    val cantidad: Int,
//    val precioUnitario: Double,
//    val subtotal: Double,
//    val idLineaOriginalAbonada: String? = null
//)
//
//enum class TipoLinea { MANUAL, PRODUCTO }
//enum class MetodoPago { EFECTIVO, BIZUM, TARJETA }
//enum class PestanaVenta { MANUAL, PRODUCTOS }
//
