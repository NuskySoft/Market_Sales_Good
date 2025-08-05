package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.nuskysoftware.marketsales.data.local.dao.LineasVentaDao
import es.nuskysoftware.marketsales.data.local.dao.RecibosDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VentasRepository(context: Context) {

    private val database: AppDatabase = AppDatabase.getDatabase(context)
    private val recibosDao: RecibosDao = database.recibosDao()
    private val lineasVentaDao: LineasVentaDao = database.lineasVentaDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val connectivityObserver = ConnectivityObserver(context)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "VentasRepository"
    }

    init {
        // Al recuperar conexión, solo subimos pendientes (no recalculamos estados)
        repoScope.launch {
            connectivityObserver.isConnected.collect { online ->
                if (online) {
                    try {
                        val uid = ConfigurationManager.getCurrentUserId()
                        if (!uid.isNullOrBlank()) {
                            pushPendientes(uid)
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    // ========= OBSERVACIÓN PARA UI =========

    fun observarRecibosPorMercadillo(idMercadillo: String): Flow<List<ReciboEntity>> =
        recibosDao.observarRecibosPorMercadillo(idMercadillo)

    fun observarLineasPorRecibo(idRecibo: String): Flow<List<LineaVentaEntity>> =
        lineasVentaDao.observarLineasPorRecibo(idRecibo)

    fun observarLineasPorMercadillo(idMercadillo: String): Flow<List<LineaVentaEntity>> =
        lineasVentaDao.observarLineasPorMercadillo(idMercadillo)

    // ========= ALTA DE VENTA (TICKET) =========
    /**
     * Room-first. Si hay red, sube (best effort). Si no, queda pendiente.
     * idRecibo = "RC" + HHmmss + "-" + <IDIOMA_APP_2L>
     * idLinea  = contador "0001", "0002", … reinicia por mercadillo.
     */
    suspend fun guardarVenta(
        idMercadillo: String,
        lineas: List<es.nuskysoftware.marketsales.data.repository.LineaVentaUI>,
        metodoPago: String,
        total: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: ConfigurationManager.getCurrentUserId()
            ?: throw IllegalStateException("Usuario no autenticado")
            val now = System.currentTimeMillis()

            val idioma = (ConfigurationManager.idioma.value)
                .trim().split('-', '_').firstOrNull()?.take(2)?.uppercase(Locale.ROOT) ?: "ES"
            val hora = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(now))
            val idRecibo = "RC$hora-$idioma"

            val maxId = lineasVentaDao.obtenerMaxIdLineaPorMercadillo(idMercadillo)
            val base = maxId?.toIntOrNull() ?: 0
            fun nextSeq(n: Int) = String.format("%04d", n)

            val recibo = ReciboEntity(
                idRecibo = idRecibo,
                idMercadillo = idMercadillo,
                idUsuario = userId,
                fechaHora = now,
                metodoPago = metodoPago,
                totalTicket = total,
                estado = "COMPLETADO",
                sincronizadoFirebase = false,
                version = 1,
                lastModified = now,
                syncError = null
            )

            val lineasEntity = lineas.mapIndexed { idx, l ->
                LineaVentaEntity(
                    idLinea = nextSeq(base + idx + 1),
                    idRecibo = idRecibo,
                    idMercadillo = idMercadillo,
                    idUsuario = userId,
                    numeroLinea = idx + 1,
                    tipoLinea = l.tipoLinea.name,
                    descripcion = l.descripcion,
                    idProducto = l.idProducto,
                    cantidad = l.cantidad,
                    precioUnitario = l.precioUnitario,
                    subtotal = l.subtotal,
                    // En alta normal de ticket NO es un abono → null
                    idLineaOriginalAbonada = null,
                    sincronizadoFirebase = false,
                    version = 1,
                    lastModified = now,
                    syncError = null
                )
            }

            // Persistencia local (TX)
            database.runInTransaction {
                runBlocking {
                    recibosDao.insertarRecibo(recibo)
                    lineasVentaDao.insertarLineas(lineasEntity)
                }
            }

            // Best-effort subida
            try {
                upsertReciboYLineasEnFirebase(recibo, lineasEntity)
                // Si todo OK, marcar sincronizado
                recibosDao.marcarSincronizados(listOf(idRecibo))
                lineasVentaDao.marcarSincronizadasPorRecibo(idRecibo)
            } catch (e: Exception) {
                // Silencioso: queda pendiente
                Log.w(TAG, "push inmediato falló: ${e.message}")
            }

            Result.success(idRecibo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========= ABONO DE LÍNEA =========
    suspend fun abonarLinea(
        idMercadillo: String,
        lineaOriginal: LineaVentaEntity,
        cantidadAbonar: Int = 1
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: ConfigurationManager.getCurrentUserId()
            ?: throw IllegalStateException("Usuario no autenticado")
            val yaAbonada = lineasVentaDao.existeAbonoParaLinea(idMercadillo, lineaOriginal.idLinea)
            if (yaAbonada) return@withContext Result.failure(IllegalStateException("La línea ya tiene abono"))

            val now = System.currentTimeMillis()
            val idioma = (ConfigurationManager.idioma.value)
                .trim().split('-', '_').firstOrNull()?.take(2)?.uppercase(Locale.ROOT) ?: "ES"
            val hora = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(now))
            val idRecibo = "RC$hora-$idioma"

            val maxId = lineasVentaDao.obtenerMaxIdLineaPorMercadillo(idMercadillo)
            val base = maxId?.toIntOrNull() ?: 0
            fun nextSeq(n: Int) = String.format("%04d", n)

            val reciboAbono = ReciboEntity(
                idRecibo = idRecibo,
                idMercadillo = idMercadillo,
                idUsuario = userId,
                fechaHora = now,
                metodoPago = "ABONO",
                totalTicket = -(lineaOriginal.precioUnitario * cantidadAbonar),
                estado = "COMPLETADO",
                sincronizadoFirebase = false,
                version = 1,
                lastModified = now,
                syncError = null
            )

            val lineaAbono = LineaVentaEntity(
                idLinea = nextSeq(base + 1),
                idRecibo = idRecibo,
                idMercadillo = idMercadillo,
                idUsuario = userId,
                numeroLinea = 1,
                tipoLinea = lineaOriginal.tipoLinea,
                descripcion = "ABONO: ${lineaOriginal.descripcion}",
                idProducto = lineaOriginal.idProducto,
                cantidad = -cantidadAbonar,
                precioUnitario = lineaOriginal.precioUnitario,
                subtotal = -(lineaOriginal.precioUnitario * cantidadAbonar),
                idLineaOriginalAbonada = lineaOriginal.idLinea,
                sincronizadoFirebase = false,
                version = 1,
                lastModified = now,
                syncError = null
            )

            database.runInTransaction {
                runBlocking {
                    recibosDao.insertarRecibo(reciboAbono)
                    lineasVentaDao.insertarLinea(lineaAbono)
                }
            }

            try {
                upsertReciboYLineasEnFirebase(reciboAbono, listOf(lineaAbono))
                recibosDao.marcarSincronizados(listOf(idRecibo))
                lineasVentaDao.marcarSincronizadasPorRecibo(idRecibo)
            } catch (e: Exception) {
                Log.w(TAG, "push abono inmediato falló: ${e.message}")
            }

            Result.success(idRecibo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========= SYNC DIFERIDO =========
    suspend fun pushPendientes(userId: String) = withContext(Dispatchers.IO) {
        val pendientes = recibosDao.obtenerPendientes(userId)
        for (rec in pendientes) {
            try {
                val lineas = lineasVentaDao.obtenerPendientesPorRecibo(rec.idRecibo)
                upsertReciboYLineasEnFirebase(rec, lineas)
                recibosDao.marcarSincronizados(listOf(rec.idRecibo))
                lineasVentaDao.marcarSincronizadasPorRecibo(rec.idRecibo)
                Log.i(TAG, "[SYNC] Upsert OK recibo ${rec.idRecibo} + ${lineas.size} líneas")
            } catch (e: Exception) {
                val msg = e.message ?: "Error desconocido"
                recibosDao.marcarError(rec.idRecibo, msg)
                lineasVentaDao.marcarErrorPorRecibo(rec.idRecibo, msg)
                Log.e(TAG, "[SYNC] Error subiendo ${rec.idRecibo}: $msg")
            }
        }
    }

    // ========= PULL SELECTIVO (solo si no hay sucios) =========
    suspend fun pullSelective(userId: String) = withContext(Dispatchers.IO) {
        val haySucios = recibosDao.existenPendientes(userId)
        if (haySucios) return@withContext
        // Aquí podrías descargar últimos X días y hacer upsert a Room.
    }

    // ========= HELPERS FIREBASE =========
    private suspend fun upsertReciboYLineasEnFirebase(
        recibo: ReciboEntity,
        lineas: List<LineaVentaEntity>
    ) {
        // Idempotencia: IDs locales como IDs de documento
        val recRef = firestore.collection("recibos").document(recibo.idRecibo)
        recRef.set(recibo.toMap(), SetOptions.merge()).await()
        for (l in lineas) {
            val lineaDocId = "${l.idMercadillo}_${l.idLinea}" // único dentro del mercadillo
            val linRef = firestore.collection("lineas_venta").document(lineaDocId)
            linRef.set(l.toMap(), SetOptions.merge()).await()
        }
    }

    // ========= DTO UI (solo para firma pública si lo usas desde fuera) =========
    data class LineaVentaUI(
        val tipoLinea: TipoLinea,
        val descripcion: String,
        val idProducto: String? = null,
        val cantidad: Int,
        val precioUnitario: Double,
        val subtotal: Double,
        val idLineaOriginalAbonada: String? = null
    )

    enum class TipoLinea { MANUAL, PRODUCTO }

    // ========= MAPEO SIMPLE =========
    private fun ReciboEntity.toMap() = mapOf(
        "idRecibo" to idRecibo,
        "idMercadillo" to idMercadillo,
        "idUsuario" to idUsuario,
        "fechaHora" to fechaHora,
        "metodoPago" to metodoPago,
        "totalTicket" to totalTicket,
        "estado" to estado,
        "version" to version,
        "lastModified" to lastModified
    )

    private fun LineaVentaEntity.toMap() = mapOf(
        "idLinea" to idLinea,
        "idRecibo" to idRecibo,
        "idMercadillo" to idMercadillo,
        "idUsuario" to idUsuario,
        "numeroLinea" to numeroLinea,
        "tipoLinea" to tipoLinea,
        "descripcion" to descripcion,
        "idProducto" to idProducto,
        "cantidad" to cantidad,
        "precioUnitario" to precioUnitario,
        "subtotal" to subtotal,
        "idLineaOriginalAbonada" to idLineaOriginalAbonada,
        "version" to version,
        "lastModified" to lastModified
    )
}


//// app/src/main/java/es/nuskysoftware/marketsales/data/repository/VentasRepository.kt
//package es.nuskysoftware.marketsales.data.repository
//
//import android.content.Context
//import android.util.Log
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.SetOptions
//import es.nuskysoftware.marketsales.data.local.dao.LineasVentaDao
//import es.nuskysoftware.marketsales.data.local.dao.RecibosDao
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
//import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.ConnectivityObserver
//import kotlinx.coroutines.*
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.tasks.await
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//
//
//class VentasRepository(context: Context) {
//
//    private val database: AppDatabase = AppDatabase.getDatabase(context)
//    private val recibosDao: RecibosDao = database.recibosDao()
//    private val lineasVentaDao: LineasVentaDao = database.lineasVentaDao()
//    private val firestore = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//    private val connectivityObserver = ConnectivityObserver(context)
//    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//    companion object {
//        private const val TAG = "VentasRepository"
//    }
//
//    init {
//        // Al recuperar conexión, solo subimos pendientes (no recalculamos estados)
//        repoScope.launch {
//            connectivityObserver.isConnected.collect { online ->
//                if (online) {
//                    try {
//                        val uid = ConfigurationManager.getCurrentUserId()
//                        if (!uid.isNullOrBlank()) {
//                            pushPendientes(uid)
//                        }
//                    } catch (_: Exception) {}
//                }
//            }
//        }
//    }
//
//    // ========= OBSERVACIÓN PARA UI =========
//
//    fun observarRecibosPorMercadillo(idMercadillo: String): Flow<List<ReciboEntity>> =
//        recibosDao.observarRecibosPorMercadillo(idMercadillo)
//
//    fun observarLineasPorRecibo(idRecibo: String): Flow<List<LineaVentaEntity>> =
//        lineasVentaDao.observarLineasPorRecibo(idRecibo)
//
//    fun observarLineasPorMercadillo(idMercadillo: String): Flow<List<LineaVentaEntity>> =
//        lineasVentaDao.observarLineasPorMercadillo(idMercadillo)
//
//    // ========= ALTA DE VENTA (TICKET) =========
//    /**
//     * Room-first. Si hay red, sube (best effort). Si no, queda pendiente.
//     * idRecibo = "RC" + HHmmss + "-" + <IDIOMA_APP_2L>
//     * idLinea  = contador "0001", "0002", … reinicia por mercadillo.
//     */
//    suspend fun guardarVenta(
//        idMercadillo: String,
//        lineas: List<es.nuskysoftware.marketsales.data.repository.LineaVentaUI>,
//        metodoPago: String,
//        total: Double
//    ): Result<String> = withContext(Dispatchers.IO) {
//        try {
//            val userId = auth.currentUser?.uid ?: ConfigurationManager.getCurrentUserId()
//            ?: throw IllegalStateException("Usuario no autenticado")
//            val now = System.currentTimeMillis()
//
//            val idioma = (ConfigurationManager.idioma.value)
//                .trim().split('-', '_').firstOrNull()?.take(2)?.uppercase(Locale.ROOT) ?: "ES"
//            val hora = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(now))
//            val idRecibo = "RC$hora-$idioma"
//
//            val maxId = lineasVentaDao.obtenerMaxIdLineaPorMercadillo(idMercadillo)
//            val base = maxId?.toIntOrNull() ?: 0
//            fun nextSeq(n: Int) = String.format("%04d", n)
//
//            val recibo = ReciboEntity(
//                idRecibo = idRecibo,
//                idMercadillo = idMercadillo,
//                idUsuario = userId,
//                fechaHora = now,
//                metodoPago = metodoPago,
//                totalTicket = total,
//                estado = "COMPLETADO",
//                sincronizadoFirebase = false,
//                version = 1,
//                lastModified = now,
//                syncError = null
//            )
//
//            val lineasEntity = lineas.mapIndexed { idx, l ->
//                LineaVentaEntity(
//                    idLinea = nextSeq(base + idx + 1),
//                    idRecibo = idRecibo,
//                    idMercadillo = idMercadillo,
//                    idUsuario = userId,
//                    numeroLinea = idx + 1,
//                    tipoLinea = l.tipoLinea.name,
//                    descripcion = l.descripcion,
//                    idProducto = l.idProducto,
//                    cantidad = l.cantidad,
//                    precioUnitario = l.precioUnitario,
//                    subtotal = l.subtotal,
//                    idLineaOriginalAbonada = l.idLineaOriginalAbonada,
//                    sincronizadoFirebase = false,
//                    version = 1,
//                    lastModified = now,
//                    syncError = null
//                )
//            }
//
//            // Persistencia local (TX)
//            database.runInTransaction {
//                runBlocking {
//                    recibosDao.insertarRecibo(recibo)
//                    lineasVentaDao.insertarLineas(lineasEntity)
//                }
//            }
//
//            // Best-effort subida
//            try {
//                upsertReciboYLineasEnFirebase(recibo, lineasEntity)
//                // Si todo OK, marcar sincronizado
//                recibosDao.marcarSincronizados(listOf(idRecibo))
//                lineasVentaDao.marcarSincronizadasPorRecibo(idRecibo)
//            } catch (e: Exception) {
//                // Silencioso: queda pendiente
//                Log.w(TAG, "push inmediato falló: ${e.message}")
//            }
//
//            Result.success(idRecibo)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    // ========= ABONO DE LÍNEA =========
//    suspend fun abonarLinea(
//        idMercadillo: String,
//        lineaOriginal: LineaVentaEntity,
//        cantidadAbonar: Int = 1
//    ): Result<String> = withContext(Dispatchers.IO) {
//        try {
//            val userId = auth.currentUser?.uid ?: ConfigurationManager.getCurrentUserId()
//            ?: throw IllegalStateException("Usuario no autenticado")
//            val yaAbonada = lineasVentaDao.existeAbonoParaLinea(idMercadillo, lineaOriginal.idLinea)
//            if (yaAbonada) return@withContext Result.failure(IllegalStateException("La línea ya tiene abono"))
//
//            val now = System.currentTimeMillis()
//            val idioma = (ConfigurationManager.idioma.value)
//                .trim().split('-', '_').firstOrNull()?.take(2)?.uppercase(Locale.ROOT) ?: "ES"
//            val hora = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(now))
//            val idRecibo = "RC$hora-$idioma"
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
//                estado = "COMPLETADO",
//                sincronizadoFirebase = false,
//                version = 1,
//                lastModified = now,
//                syncError = null
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
//                idLineaOriginalAbonada = lineaOriginal.idLinea,
//                sincronizadoFirebase = false,
//                version = 1,
//                lastModified = now,
//                syncError = null
//            )
//
//            database.runInTransaction {
//                runBlocking {
//                    recibosDao.insertarRecibo(reciboAbono)
//                    lineasVentaDao.insertarLinea(lineaAbono)
//                }
//            }
//
//            try {
//                upsertReciboYLineasEnFirebase(reciboAbono, listOf(lineaAbono))
//                recibosDao.marcarSincronizados(listOf(idRecibo))
//                lineasVentaDao.marcarSincronizadasPorRecibo(idRecibo)
//            } catch (e: Exception) {
//                Log.w(TAG, "push abono inmediato falló: ${e.message}")
//            }
//
//            Result.success(idRecibo)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    // ========= SYNC DIFERIDO =========
//    suspend fun pushPendientes(userId: String) = withContext(Dispatchers.IO) {
//        val pendientes = recibosDao.obtenerPendientes(userId)
//        for (rec in pendientes) {
//            try {
//                val lineas = lineasVentaDao.obtenerPendientesPorRecibo(rec.idRecibo)
//                upsertReciboYLineasEnFirebase(rec, lineas)
//                recibosDao.marcarSincronizados(listOf(rec.idRecibo))
//                lineasVentaDao.marcarSincronizadasPorRecibo(rec.idRecibo)
//                Log.i(TAG, "[SYNC] Upsert OK recibo ${rec.idRecibo} + ${lineas.size} líneas")
//            } catch (e: Exception) {
//                val msg = e.message ?: "Error desconocido"
//                recibosDao.marcarError(rec.idRecibo, msg)
//                lineasVentaDao.marcarErrorPorRecibo(rec.idRecibo, msg)
//                Log.e(TAG, "[SYNC] Error subiendo ${rec.idRecibo}: $msg")
//            }
//        }
//    }
//
//    // ========= PULL SELECTIVO (solo si no hay sucios) =========
//    suspend fun pullSelective(userId: String) = withContext(Dispatchers.IO) {
//        val haySucios = recibosDao.existenPendientes(userId)
//        if (haySucios) return@withContext
//        // Si quieres, aquí puedes descargar últimos X días y hacer upsert a Room.
//        // Lo dejo como stub por ahora para no mezclar con tu lógica de pull global.
//    }
//
//    // ========= HELPERS FIREBASE =========
//    private suspend fun upsertReciboYLineasEnFirebase(recibo: ReciboEntity, lineas: List<LineaVentaEntity>) {
//        // Idempotencia: IDs locales como IDs de documento
//        val recRef = firestore.collection("recibos").document(recibo.idRecibo)
//        recRef.set(recibo.toMap(), SetOptions.merge()).await()
//        for (l in lineas) {
//            val lineaDocId = "${l.idMercadillo}_${l.idLinea}" // único dentro del mercadillo
//            val linRef = firestore.collection("lineas_venta").document(lineaDocId)
//            linRef.set(l.toMap(), SetOptions.merge()).await()
//        }
//    }
//
//    // ========= DTO UI =========
//    data class LineaVentaUI(
//        val tipoLinea: TipoLinea,
//        val descripcion: String,
//        val idProducto: String? = null,
//        val cantidad: Int,
//        val precioUnitario: Double,
//        val subtotal: Double,
//        val idLineaOriginalAbonada: String? = null
//    )
//
//    enum class TipoLinea { MANUAL, PRODUCTO }
//
//    // ========= MAPEO SIMPLE =========
//    private fun ReciboEntity.toMap() = mapOf(
//        "idRecibo" to idRecibo,
//        "idMercadillo" to idMercadillo,
//        "idUsuario" to idUsuario,
//        "fechaHora" to fechaHora,
//        "metodoPago" to metodoPago,
//        "totalTicket" to totalTicket,
//        "estado" to estado,
//        "version" to version,
//        "lastModified" to lastModified
//    )
//
//    private fun LineaVentaEntity.toMap() = mapOf(
//        "idLinea" to idLinea,
//        "idRecibo" to idRecibo,
//        "idMercadillo" to idMercadillo,
//        "idUsuario" to idUsuario,
//        "numeroLinea" to numeroLinea,
//        "tipoLinea" to tipoLinea,
//        "descripcion" to descripcion,
//        "idProducto" to idProducto,
//        "cantidad" to cantidad,
//        "precioUnitario" to precioUnitario,
//        "subtotal" to subtotal,
//        "idLineaOriginalAbonada" to idLineaOriginalAbonada,
//        "version" to version,
//        "lastModified" to lastModified
//    )
//}
//
//
