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


    /** Unidades confirmadas del catálogo; el borrador se suma aparte en la interfaz. */
    fun observarUnidadesCatalogo(uid: String, market: String): Flow<Long> =
        lineasVentaDao.observarUnidadesCatalogoVendidas(uid, market)

    // Consulta previa al cobro; la comprobación definitiva se repite dentro de la transacción.
    suspend fun admiteCatalogo(uid: String, market: String, lineas: List<LineaVentaUI>): Boolean {
        DemoAccess.requireOwner(uid)
        val requested = unidadesCatalogo(lineas)
        if (requested == 0L) return true
        val mode = DemoAccess.mode()
        return es.nuskysoftware.marketsales.utils.AppModePolicy.canUseCatalog(mode, lineasVentaDao.unidadesCatalogoVendidas(uid, market), requested)
    }

    private fun unidadesCatalogo(lineas: List<LineaVentaUI>): Long =
        lineas.filter { it.tipoLinea == TipoLinea.PRODUCTO && it.cantidad > 0 && it.idLineaOriginalAbonada == null }.sumOf { it.cantidad.toLong() }

    /**
     * Confirma una venta local y conserva las obligaciones de stock en la misma transacción.
     * El envío remoto es posterior y no puede invalidar una venta ya guardada.
     * @param idMercadillo Identificador del mercadillo propietario de la venta.
     * @param lineas Conceptos manuales o artículos, con cantidad, precio e importe.
     * @param metodoPago Método empleado en esta operación, también cuando es una devolución.
     * @param total Importe recibido desde la interfaz, utilizado por la validación Demo.
     * @return Identificador del recibo confirmado, o fallo de la operación local.
     */
    suspend fun guardarVenta(
        idMercadillo: String,
        lineas: List<LineaVentaUI>,
        metodoPago: String,
        total: Double
    ): Result<String> {
        val userId = ConfigurationManager.getCurrentUserId() ?: return Result.failure(IllegalStateException("No owner"))
        val idRecibo = try {
            // Una instalación gratuita con historial remoto reservado debe resolver la recuperación antes de escribir.
            RecoveryGate(appContext, db).beforeWrite(userId)
            //val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            db.withTransaction {
                DemoAccess.requireOwner(userId)
                val market = db.mercadilloDao().getMercadilloById(idMercadillo) ?: error("Market not found")
                DemoAccess.requireOwner(market.userId)
                // El plan se captura dentro de la confirmación local, junto a la verificación de cupo.
                val mode = DemoAccess.mode()
                val isPremiumAtConfirmation = mode == es.nuskysoftware.marketsales.utils.AppMode.PREMIUM
                // En cuentas registradas no se admiten ventas durante el arqueo, cierre o cancelación; Demo es la excepción.
                check(DemoAccess.isDemo() || market.estado !in 4..7) { "Market does not allow sales" }
                // Comprueba las referencias de abono para que una línea original no se devuelva dos veces.
                for (refund in lineas.filter { it.idLineaOriginalAbonada != null }) {
                    val previous = lineasVentaDao.obtenerLineasPorMercadillo(idMercadillo).first()
                    check(previous.none { it.idLineaOriginalAbonada == refund.idLineaOriginalAbonada }) { "Already refunded" }
                }
                // El cupo se comprueba bajo la misma transacción que guarda el ticket para evitar
                // que dos confirmaciones locales simultáneas consuman las mismas unidades.
                if (!es.nuskysoftware.marketsales.utils.AppModePolicy.canUseCatalog(mode, lineasVentaDao.unidadesCatalogoVendidas(userId, idMercadillo), unidadesCatalogo(lineas))) {
                    DemoAccess.deny("free_catalog_limit")
                }
                check(market.activo) { "Market unavailable" }
                // El límite Demo cuenta recibos existentes y se verifica dentro de la transacción.
                DemoAccess.checkLimit("ventas", recibosDao.contarDemo(userId))
                if (DemoAccess.isDemo() && (total <= 0 || lineas.any { it.cantidad <= 0 || it.subtotal < 0 || it.idLineaOriginalAbonada != null })) {
                    DemoAccess.deny("demo_abonos_registrados")
                }
                val now = System.currentTimeMillis()

                // El idioma se utiliza únicamente como sufijo de presentación del identificador del recibo.
                val idiomaCfgPersistido = if (DemoAccess.isDemo()) null else configuracionDao.getConfiguracionSync()?.idioma
                val idioma2Letras = (idiomaCfgPersistido ?: ConfigurationManager.idioma.value)
                    .trim()
                    .split('-', '_')
                    .firstOrNull()
                    ?.take(2)
                    ?.uppercase(Locale.ROOT)
                    ?: "ES"

                    // UUID evita colisiones de recibo entre ventas cercanas y entre dispositivos.
                    val idRecibo = "RC${java.util.UUID.randomUUID()}-$idioma2Letras"

                // Cálculo secuencial conservado en esta versión. La identidad persistida de cada línea es un UUID;
                // numeroLinea representa su posición dentro del recibo, no una clave remota global.
                val maxId = lineasVentaDao.obtenerMaxIdLineaPorMercadillo(idMercadillo) // p.ej. "0007" o null
                val base = maxId?.toIntOrNull() ?: 0
                fun nextSeq(n: Int) = String.format("%04d", n)

                // --- Construir entidades ---
                // La cabecera calcula el total desde las líneas con aritmética monetaria decimal.
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

                // Cada línea recibe un identificador propio y conserva el vínculo con un eventual abono original.
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
                // Persiste la obligación de stock junto al recibo, sin exigir que el descuento ya se haya aplicado.
                if (isPremiumAtConfirmation) {
                    val movements = mutableListOf<StockMovementEntity>()
                    for (line in lineasEntity) {
                        val productId = line.idProducto
                        if (line.tipoLinea == TipoLinea.PRODUCTO.name && productId != null) {
                            val article = db.articuloDao().getArticuloById(productId)
                            // Si falta el artículo, conserva la obligación para diagnóstico y recuperación posterior.
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
                    // La instantánea del recibo se guarda en la misma transacción; el PDF se genera después.
                    val original = lineasEntity.firstNotNullOfOrNull { it.idLineaOriginalAbonada }?.let { originalId ->
                        lineasVentaDao.obtenerLineasPorMercadillo(idMercadillo, userId).first().firstOrNull { it.idLinea == originalId }?.idRecibo
                    }.orEmpty()
                    val logo = runCatching { es.nuskysoftware.marketsales.receipts.ReceiptFiles.logo(appContext,userId).takeIf { it.isFile && it.length() <= 524288 }?.readBytes()?.let { android.util.Base64.encodeToString(it,android.util.Base64.NO_WRAP) }.orEmpty() }.getOrDefault("")
                    val snapshot = es.nuskysoftware.marketsales.receipts.ReceiptFiles.snapshot(recibo,lineasEntity,db.empresaDao().getEmpresa(userId),market.lugar,ConfigurationManager.getMoneda(),ConfigurationManager.getIdioma(),logo,original)
                    db.receiptDocumentDao().insert(es.nuskysoftware.marketsales.data.local.entity.ReceiptDocumentEntity(userId,idRecibo,snapshot))
                }
                idRecibo
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // Solo se comunica fallo de venta cuando no se ha completado la transacción local.
            return Result.failure(failure)
        }

        // A partir de aquí la venta es definitiva: un fallo de stock, red o planificación no puede invalidarla.
        try {
            if (onLocalSaleCommitted != null) {
                onLocalSaleCommitted.invoke(idRecibo)
            } else {
                // El trabajo PDF se programa después de la confirmación; un fallo no invalida el ticket.
                runCatching { es.nuskysoftware.marketsales.work.ReceiptWorker.request(appContext) }
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

    // El flujo vuelve a filtrar los recibos cuando cambia la cuenta activa para evitar mostrar datos ajenos.
    fun obtenerVentasMercadillo(idMercadillo: String): Flow<List<ReciboEntity>> =
        ConfigurationManager.usuarioLogueado.flatMapLatest { uid -> recibosDao.obtenerRecibosPorMercadillo(idMercadillo).map { rows -> rows.filter { it.idUsuario == uid } } }

    // Las líneas del recibo solo se muestran si pertenecen al propietario de la sesión.
    fun obtenerLineasVenta(idRecibo: String): Flow<List<LineaVentaEntity>> =
        ConfigurationManager.usuarioLogueado.flatMapLatest { uid -> lineasVentaDao.obtenerLineasPorRecibo(idRecibo).map { rows -> rows.filter { it.idUsuario == uid } } }

    /**
     * Crea un abono por la línea original indicada.
     * Utiliza un recibo nuevo con el método real de devolución y una línea de cantidad e importe negativos.
     * @param lineaOriginal Línea de venta que se devuelve en su totalidad.
     * @param cantidadAbonar Unidades de la línea original; no se admiten abonos parciales.
     * @param idMercadillo Mercadillo al que pertenece la operación original.
     * @param metodoDevolucion Efectivo, Bizum o tarjeta, según cómo se devuelve el dinero.
     * @return Identificador del recibo de abono o fallo de validación/guardado local.
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
        // Una devolución conserva el precio unitario y referencia original; invierte cantidad e importe.
        val refund = LineaVentaUI(tipoLinea = TipoLinea.valueOf(lineaOriginal.tipoLinea), descripcion = lineaOriginal.descripcion,
            idProducto = lineaOriginal.idProducto, cantidad = -cantidadAbonar, precioUnitario = lineaOriginal.precioUnitario,
            subtotal = -lineaOriginal.subtotal, idLineaOriginalAbonada = lineaOriginal.idLinea)
        guardarVenta(idMercadillo, listOf(refund), metodoDevolucion.uppercase(Locale.ROOT), refund.subtotal)
    } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { Result.failure(failure) }

    // La subida se delega al trabajo persistente de conciliación; no bloquea la confirmación de venta.
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
