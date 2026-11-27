// app/src/main/java/es/nuskysoftware/marketsales/data/repository/VentasRepository.kt
package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Registra recibos, líneas de venta y abonos y conserva la obligación de stock junto con la venta confirmada.
import es.nuskysoftware.marketsales.utils.DemoAccess


import androidx.room.withTransaction
import es.nuskysoftware.marketsales.data.local.entity.StockMovementEntity
import es.nuskysoftware.marketsales.work.StockMovementScheduler
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VentasRepository(
    context: Context,
    database: AppDatabase = AppDatabase.getDatabase(context),
    private val onLocalSaleCommitted: ((String) -> Unit)? = null
) {
    private val appContext = context.applicationContext



    companion object { private const val TAG = "VentasRepository" }

    private val db = database
    private val recibosDao: RecibosDao = db.recibosDao()
    private val lineasVentaDao: LineasVentaDao = db.lineasVentaDao()
    private val configuracionDao = db.configuracionDao()



    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val connectivity = ConnectivityObserver(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }


    /**
     *  * Guarda una venta completa (recibo + líneas) en Room y sincroniza cuando es posible.
     *
     */
    // Confirma la venta local; la transacción conserva las líneas y las obligaciones de stock antes de solicitar la subida.
    suspend fun guardarVenta(
        idMercadillo: String,
        lineas: List<LineaVentaUI>,
        metodoPago: String,
        total: Double
    ): Result<String> {
        val userId = ConfigurationManager.getCurrentUserId() ?: return Result.failure(IllegalStateException("No owner"))
        val isPremiumAtConfirmation = ConfigurationManager.getIsPremium() && !DemoAccess.isDemo()
        val idRecibo = try {
            RecoveryGate(appContext, db).beforeWrite(userId)
            //val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            db.withTransaction {
                DemoAccess.requireOwner(userId)
                val market = db.mercadilloDao().getMercadilloById(idMercadillo) ?: error("Market not found")
                DemoAccess.requireOwner(market.userId)
                check(DemoAccess.isDemo() || market.estado !in 4..7) { "Market does not allow sales" }
                for (refund in lineas.filter { it.idLineaOriginalAbonada != null }) {
                    val previous = lineasVentaDao.obtenerLineasPorMercadillo(idMercadillo).first()
                    check(previous.none { it.idLineaOriginalAbonada == refund.idLineaOriginalAbonada }) { "Already refunded" }
                }
                DemoAccess.checkLimit("ventas", recibosDao.contarDemo(userId))
                if (DemoAccess.isDemo() && (total <= 0 || lineas.any { it.cantidad <= 0 || it.subtotal < 0 || it.idLineaOriginalAbonada != null })) {
                    DemoAccess.deny("demo_abonos_registrados")
                }
                val now = System.currentTimeMillis()

                val idiomaCfgPersistido = if (DemoAccess.isDemo()) null else configuracionDao.getConfiguracionSync()?.idioma
                val idioma2Letras = (idiomaCfgPersistido ?: ConfigurationManager.idioma.value)
                    .trim()
                    .split('-', '_')
                    .firstOrNull()
                    ?.take(2)
                    ?.uppercase(Locale.ROOT)
                    ?: "ES"

                    val idRecibo = "RC${java.util.UUID.randomUUID()}-$idioma2Letras"

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
                    totalTicket = es.nuskysoftware.marketsales.utils.Money.sum(lineas.map { it.subtotal }),
                    estado = "COMPLETADO",
                    lastModified = now,
                    sincronizadoFirebase = false
                )

                val lineasEntity = lineas.mapIndexed { index, l ->
                    val numeroLineaRecibo = index + 1
                    val idLineaSecuencial = nextSeq(base + numeroLineaRecibo)
                    LineaVentaEntity(
                        idLinea = java.util.UUID.randomUUID().toString(),
                        idRecibo = idRecibo,
                        idMercadillo = idMercadillo,
                        idUsuario = userId,
                        numeroLinea = numeroLineaRecibo,
                        tipoLinea = l.tipoLinea.name, // MANUAL | PRODUCTO
                        descripcion = l.descripcion,
                        idProducto = l.idProducto,
                        cantidad = l.cantidad,
                        precioUnitario = es.nuskysoftware.marketsales.utils.Money.round(l.precioUnitario),
                        subtotal = es.nuskysoftware.marketsales.utils.Money.round(l.subtotal),
                        idLineaOriginalAbonada = l.idLineaOriginalAbonada,
                        lastModified = now,
                        sincronizadoFirebase = false
                    )
                }

                // --- Persistencia local (offline-first) ---
                recibosDao.insertarRecibo(recibo)
                lineasVentaDao.insertarLineas(lineasEntity)
                // Capture the obligation in the SAME commit as the sale, not the stock update.
                if (isPremiumAtConfirmation) {
                    val movements = mutableListOf<StockMovementEntity>()
                    for (line in lineasEntity) {
                        val productId = line.idProducto
                        if (line.tipoLinea == TipoLinea.PRODUCTO.name && productId != null) {
                            val article = db.articuloDao().getArticuloById(productId)
                            // If missing, retain the obligation for diagnosis/recovery rather than lose it.
                            if (article == null || article.controlarStock) {
                                movements += StockMovementEntity(
                                    idMovimiento = "$idRecibo:${line.idLinea}",
                                    idUsuario = userId, idRecibo = idRecibo, idArticulo = productId,
                                    cantidad = line.cantidad, creadoEn = now
                                )
                            }
                        }
                    }
                    db.stockMovementDao().enqueue(movements)
                }
                idRecibo
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // Only an unsuccessful LOCAL sale commit may be presented as a failed sale.
            return Result.failure(failure)
        }

        // Past this point the sale is final. No stock/network/scheduling failure may change it.
        try {
            if (onLocalSaleCommitted != null) {
                onLocalSaleCommitted.invoke(idRecibo)
            } else {
                runCatching { StockMovementScheduler.request(appContext) }
                    .onFailure { Log.w(TAG, "Sale saved; stock scheduling will retry on startup", it) }
                scope.launch {
                    runCatching { StockMovementProcessor(db).processPending() }
                        .onFailure { Log.w(TAG, "Sale saved; stock remains pending", it) }
                    runCatching { StockMovementScheduler.requestUpload(appContext) }
                    if (DemoAccess.canSync(userId)) runCatching { sincronizarPendientes() }
                }
            }
        } catch (failure: Exception) {
            Log.w(TAG, "Sale already saved; post-commit work remains pending", failure)
        }
        return Result.success(idRecibo)
    }

    fun obtenerVentasMercadillo(idMercadillo: String): Flow<List<ReciboEntity>> =
        ConfigurationManager.usuarioLogueado.flatMapLatest { uid -> recibosDao.obtenerRecibosPorMercadillo(idMercadillo).map { rows -> rows.filter { it.idUsuario == uid } } }

    fun obtenerLineasVenta(idRecibo: String): Flow<List<LineaVentaEntity>> =
        ConfigurationManager.usuarioLogueado.flatMapLatest { uid -> lineasVentaDao.obtenerLineasPorRecibo(idRecibo).map { rows -> rows.filter { it.idUsuario == uid } } }

    /**
     * Crea un abono por la línea original indicada.
     * Aplica las mismas reglas de idRecibo e idLinea que una venta normal.
     */
    // Registra la devolución mediante el flujo de abonos y conserva su relación con la operación original.
    suspend fun crearAbono(
        lineaOriginal: LineaVentaEntity,
        cantidadAbonar: Int,
        idMercadillo: String,
        metodoDevolucion: String
    ): Result<String> = try {
        DemoAccess.requireRegistered("demo_abonos_registrados")
        DemoAccess.requireOwner(lineaOriginal.idUsuario)
        require(metodoDevolucion.uppercase(Locale.ROOT) in setOf("EFECTIVO", "BIZUM", "TARJETA"))
        require(cantidadAbonar == lineaOriginal.cantidad && cantidadAbonar > 0)
        require(lineaOriginal.idMercadillo == idMercadillo && lineaOriginal.idLineaOriginalAbonada == null)
        val refund = LineaVentaUI(tipoLinea = TipoLinea.valueOf(lineaOriginal.tipoLinea), descripcion = lineaOriginal.descripcion,
            idProducto = lineaOriginal.idProducto, cantidad = -cantidadAbonar, precioUnitario = lineaOriginal.precioUnitario,
            subtotal = -lineaOriginal.subtotal, idLineaOriginalAbonada = lineaOriginal.idLinea)
        guardarVenta(idMercadillo, listOf(refund), metodoDevolucion.uppercase(Locale.ROOT), refund.subtotal)
    } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { Result.failure(failure) }

    private suspend fun sincronizarPendientes() {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
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
