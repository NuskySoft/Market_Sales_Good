


// app/src/main/java/es/nuskysoftware/marketsales/data/repository/MercadilloRepository.kt
package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Gestiona mercadillos, estados, saldos, arqueos y consultas locales por propietario.
import es.nuskysoftware.marketsales.utils.DemoAccess


import androidx.room.withTransaction
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.MercadilloDao
import es.nuskysoftware.marketsales.data.local.dao.EstadisticaEstado
import es.nuskysoftware.marketsales.data.local.dao.LineasVentaDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MercadilloRepository(
    context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    observeNetwork: Boolean = true
) : java.io.Closeable {
    private val appContext = context.applicationContext
    private val mercadilloDao: MercadilloDao = database.mercadilloDao()
    private val lineasVentaDao: LineasVentaDao = database.lineasVentaDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val connectivityObserver = ConnectivityObserver(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val tz: TimeZone = TimeZone.getTimeZone("Europe/Madrid")
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply { timeZone = tz }

    private val localDb = database

    companion object { private const val TAG = "MercadilloRepository" }

    init {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    // ===== Flows =====
    fun getMercadillosUsuarioActual(): Flow<List<MercadilloEntity>> =
        ConfigurationManager.usuarioLogueado.flatMapLatest { userId ->
            val effective = if (userId.isNullOrBlank()) es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID else userId
            Log.d(TAG, "🔄 Usuario cambió a: $effective - Recargando mercadillos")
            mercadilloDao.getMercadillosByUser(effective)
        }

    fun getMercadillosPorMes(ano: Int, mes: Int): Flow<List<MercadilloEntity>> =
        ConfigurationManager.usuarioLogueado.flatMapLatest { userId ->
            val effective = if (userId.isNullOrBlank()) es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID else userId
            val mesPattern = "%${String.format("%02d", mes)}-$ano"
            mercadilloDao.getMercadillosByUserAndMes(effective, mesPattern)
        }

    // ===== CRUD =====
    suspend fun crearMercadillo(
        fecha: String,
        lugar: String,
        organizador: String,
        esGratis: Boolean = true,
        importeSuscripcion: Double = 0.0,
        requiereMesa: Boolean = true,
        requiereCarpa: Boolean = true,
        hayPuntoLuz: Boolean = false,
        horaInicio: String = "09:00",
        horaFin: String = "14:00",
        saldoInicial: Double? = null,
        saldoGuardadoId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val userId = ConfigurationManager.getCurrentUserId()
            ?: throw IllegalStateException(es.nuskysoftware.marketsales.utils.StringResourceManager.getString("sin_usuario_mercadillo", ConfigurationManager.idioma.value))

        RecoveryGate(appContext).beforeWrite(userId)

        // Si el alta es con FECHA=HOY, crear directamente en EN_CURSO (3)
        val hoyStr = dateFormat.format(Date())
        val estadoInicial = when {
            fecha == hoyStr -> EstadosMercadillo.Estado.EN_CURSO.codigo
            saldoInicial != null -> EstadosMercadillo.Estado.PROGRAMADO_TOTAL.codigo
            else -> EstadosMercadillo.Estado.PROGRAMADO_PARCIAL.codigo
        }

        val nuevo = MercadilloEntity(
            userId = userId,
            fecha = fecha,
            lugar = lugar,
            organizador = organizador,
            esGratis = esGratis,
            importeSuscripcion = if (!esGratis) importeSuscripcion else 0.0,
            requiereMesa = requiereMesa,
            requiereCarpa = requiereCarpa,
            hayPuntoLuz = hayPuntoLuz,
            horaInicio = horaInicio,
            horaFin = horaFin,
            saldoInicial = saldoInicial,
            estado = estadoInicial,
            creadoEn = System.currentTimeMillis(),
            sincronizadoFirebase = false
        )

        localDb.withTransaction {
                DemoAccess.requireOwner(userId)
                DemoAccess.checkLimit("mercadillos", mercadilloDao.contarDemo(userId))
                // Comprobar fecha y cupo dentro de la transacción de alta, también sin conexión.
                validarCupoFree(userId, fecha)
                if (saldoGuardadoId != null) DemoAccess.requirePremium()
                mercadilloDao.insertMercadillo(nuevo)
                if (saldoGuardadoId != null) {
                    val saved = database.saldoGuardadoDao().byId(saldoGuardadoId, userId) ?: error("Balance not available")
                    check(!saved.consumido && !saved.eliminado)
                    database.saldoGuardadoDao().upsert(saved.copy(consumido = true, idMercadilloDestino = nuevo.idMercadillo, version = saved.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
                }
            }
        repositoryScope.launch { sincronizarMercadilloConFirebase(nuevo) }
        nuevo.idMercadillo
    }

    /** Comprueba los límites de planificación sin contar el propio mercadillo durante una edición.
     * Las ediciones en la misma fecha no consumen otra plaza ni impiden completar altas Premium anteriores. */
    private suspend fun validarCupoFree(uid: String, fecha: String, anterior: MercadilloEntity? = null) {
        if (DemoAccess.mode() != es.nuskysoftware.marketsales.utils.AppMode.FREE) return
        val day = es.nuskysoftware.marketsales.utils.CalendarDates.epochDay(fecha) ?: error("Invalid date")
        if (anterior?.fechaEpochDay == day) return
        val date = java.time.LocalDate.ofEpochDay(day)
        val except = anterior?.idMercadillo.orEmpty()
        if (mercadilloDao.ocupaFecha(uid, day, except)) DemoAccess.deny("limite_free_mercadillos")
        val previousYear = anterior?.fechaEpochDay?.let { java.time.LocalDate.ofEpochDay(it).year }
        if (previousYear != date.year) {
            val start = date.withDayOfYear(1)
            if (mercadilloDao.contarAnio(uid, start.toEpochDay(), start.plusYears(1).toEpochDay(), except) >= es.nuskysoftware.marketsales.utils.AppModePolicy.FREE_MARKETS_YEAR) {
                DemoAccess.deny("free_market_year_limit")
            }
        }
    }

    suspend fun actualizarMercadillo(mercadillo: MercadilloEntity): Boolean = withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(mercadillo.userId)
        val anterior = mercadilloDao.getMercadilloById(mercadillo.idMercadillo) ?: error("Market not found")
        check(anterior.estado !in 6..7 || DemoAccess.isDemo()) { "Final market" }
        val actualizado = mercadillo.copy(
            creadoEn = anterior.creadoEn,
            fechaEpochDay = es.nuskysoftware.marketsales.utils.CalendarDates.epochDay(mercadillo.fecha),
            version = mercadillo.version + 1,
            lastModified = System.currentTimeMillis(),
            sincronizadoFirebase = false
        )
        localDb.withTransaction {
            DemoAccess.requireOwner(anterior.userId)
            validarCupoFree(anterior.userId, actualizado.fecha, anterior)
            mercadilloDao.updateMercadillo(actualizado)
        }
        repositoryScope.launch { sincronizarMercadilloConFirebase(actualizado) }
        true
    }

    suspend fun eliminarMercadillo(mercadillo: MercadilloEntity): Boolean = withContext(Dispatchers.IO) {
        DemoAccess.requireRegistered("demo_no_borrar_mercadillo")
        DemoAccess.requireOwner(mercadillo.userId)
        mercadilloDao.updateMercadillo(mercadillo.copy(activo = false, version = mercadillo.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
        eliminarMercadilloDeFirebase(mercadillo.idMercadillo)
        true
    }

    suspend fun desactivarMercadillo(mercadilloId: String): Boolean = withContext(Dispatchers.IO) {
        DemoAccess.requireRegistered("demo_no_borrar_mercadillo")
        mercadilloDao.desactivarMercadillo(mercadilloId)
        mercadilloDao.getMercadilloById(mercadilloId)?.let { sincronizarMercadilloConFirebase(it) }
        true
    }

    suspend fun getMercadilloById(id: String): MercadilloEntity? = withContext(Dispatchers.IO) {
        mercadilloDao.getMercadilloById(id)?.takeIf { it.userId == ConfigurationManager.getCurrentUserId() }
    }

    // ===== SALDOS / ARQUEO =====
    // Aplica el saldo inicial al mercadillo indicado.
    suspend fun asignarSaldoInicial(mercadilloId: String, saldoInicial: Double, esAsignacionAutomatica: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            val ownedMarket = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            DemoAccess.requireOwner(ownedMarket.userId)
            check(ownedMarket.estado !in 6..7 || DemoAccess.isDemo()) { "Final market" }

            // La introducción manual es común; la asignación automática desde otro mercadillo es Premium.
            if (esAsignacionAutomatica) DemoAccess.requirePremium()
            mercadilloDao.asignarSaldoInicial(mercadilloId, saldoInicial)
            mercadilloDao.getMercadilloById(mercadilloId)?.let { sincronizarMercadilloConFirebase(it) }
            true
        }

    suspend fun asignarSaldoInicialAutomatico(mercadilloId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = ConfigurationManager.getCurrentUserId() ?: return@withContext false
        val ultimo = mercadilloDao.getUltimoMercadilloConSaldoFinal(userId)
        if (ultimo?.saldoFinal != null) {
            val ok = asignarSaldoInicial(mercadilloId, ultimo.saldoFinal, esAsignacionAutomatica = true)
            if (ok) mercadilloDao.marcarSaldoAsignado(ultimo.idMercadillo)
            ok
        } else false
    }

    suspend fun realizarArqueoCaja(mercadilloId: String, saldoFinal: Double, ventasEfectivo: Double, gastosEfectivo: Double): Boolean =
        withContext(Dispatchers.IO) {
            val ownedMarket = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            DemoAccess.requireOwner(ownedMarket.userId)
            check(ownedMarket.estado !in 6..7 || DemoAccess.isDemo()) { "Final market" }

            val m = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            val si = m.saldoInicial ?: return@withContext false
            val arqueoCaja = si + ventasEfectivo - gastosEfectivo
            mercadilloDao.realizarArqueoCaja(mercadilloId, arqueoCaja, saldoFinal)
            mercadilloDao.getMercadilloById(mercadilloId)?.let { sincronizarMercadilloConFirebase(it) }
            true
        }

    /** Cierra con saldo final y marca estado=6 (pendienteAsignarSaldo=0). */
    suspend fun cerrarConSaldoFinal(mercadilloId: String, saldoFinal: Double): Boolean = withContext(Dispatchers.IO) {
            val ownedMarket = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            DemoAccess.requireOwner(ownedMarket.userId)
            check(ownedMarket.estado !in 6..7 || DemoAccess.isDemo()) { "Final market" }

        mercadilloDao.marcarSaldoAsignado(mercadilloId)
        val m = mercadilloDao.getMercadilloById(mercadilloId)
        if (m != null) sincronizarMercadilloConFirebase(m)
        true
    }

    /** Finaliza un arqueo pendiente de asignación cuando la cuenta ha vuelto a Free.
     * Conserva el saldo final y no crea ni consume ningún saldo guardado.
     */
    suspend fun cerrarPendienteFree(mercadilloId: String): Boolean = withContext(Dispatchers.IO) {
        val uid = ConfigurationManager.getCurrentUserId() ?: return@withContext false
        localDb.withTransaction {
            DemoAccess.requireOwner(uid)
            check(DemoAccess.mode() == es.nuskysoftware.marketsales.utils.AppMode.FREE)
            val market = mercadilloDao.getMercadilloById(mercadilloId, uid) ?: return@withTransaction false
            if (!market.activo || market.estado != 5 || market.saldoFinal == null) return@withTransaction false
            mercadilloDao.marcarSaldoAsignado(mercadilloId, sessionOwner = uid)
            true
        }.also { closed ->
            if (closed) es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
        }
    }

    // ===== AUTOESTADOS =====

    // Delega las transiciones temporales en el procesador local compartido.
    suspend fun actualizarEstadosAutomaticos(userId: String) = withContext(Dispatchers.IO) {
        val cambios = AutoEstadoProcessor(database).apply(userId)
        if (cambios > 0 && DemoAccess.canSync(userId)) {
            es.nuskysoftware.marketsales.work.AutoEstadoScheduler.requestUpload(appContext)
        }
        Unit
    }

    suspend fun sincronizarPendientesAutoestado(userId: String) =
        SyncEngine(appContext).sync(userId)

    override fun close() {
        repositoryScope.cancel()
    }

    // Registra el importe de arqueo y el nuevo estado según el flujo de cierre.
    suspend fun confirmarArqueoCaja(
        mercadilloId: String,
        arqueoFinal: Double,
        nuevoEstado: Int
    ): Boolean = withContext(Dispatchers.IO) {
            val ownedMarket = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            DemoAccess.requireOwner(ownedMarket.userId)
            check(ownedMarket.estado !in 6..7 || DemoAccess.isDemo()) { "Final market" }

        // 1) Room primero: fija arqueo + saldoFinal y deja el mercadillo en estado=5 (pendiente de asignar)
        mercadilloDao.realizarArqueoCaja(
            id = mercadilloId,
            arqueoCaja = arqueoFinal,
            saldoFinal = arqueoFinal
        )

        // 2) Si el flujo es FREE -> cerrar directamente (estado=6 y pendienteAsignarSaldo=0)
        if (nuevoEstado == 6) {
            mercadilloDao.marcarSaldoAsignado(mercadilloId)
        }

        // 3) Intento de sync; si no hay red, queda marcado para sincronizar
        mercadilloDao.getMercadilloById(mercadilloId)?.let { sincronizarMercadilloConFirebase(it) }
        true
    }

    // ===== Consultas específicas / híbrido =====
    suspend fun getProximoMercadillo(): MercadilloEntity? = withContext(Dispatchers.IO) {
        val userId = ConfigurationManager.getCurrentUserId() ?: es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID
        val fechaActual = dateFormat.format(Date())
        mercadilloDao.getProximoMercadillo(userId, fechaActual)
    }

    fun getMercadillosRequierenAtencion(): Flow<List<MercadilloEntity>> =
        ConfigurationManager.usuarioLogueado.flatMapLatest { userId ->
            if (!userId.isNullOrBlank() && userId != es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID) {
                mercadilloDao.getMercadillosPendientesArqueo(userId)
            } else flowOf(emptyList())
        }

    /**
     * 📅 Próximos mercadillos (punto 2): desde hoy en adelante, por fecha real.
     */
    suspend fun getProximosMercadillos(userId: String): List<MercadilloEntity> = withContext(Dispatchers.IO) {
        try {
            val hoyStr = dateFormat.format(Date())
            val hoy = dateFormat.parse(hoyStr) ?: return@withContext emptyList()
            val todos = mercadilloDao.getMercadillosByUser(userId).first()

            todos.filter { m ->
                val fechaM = try { dateFormat.parse(m.fecha) } catch (_: Exception) { null }
                fechaM != null && !fechaM.before(hoy)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en getProximosMercadillos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * ✅ NUEVO (punto 3): Finalizar manualmente un mercadillo en curso.
     * - Solo actúa si el estado actual es EN_CURSO (3).
     * - Cambia a estado 4 (pendiente de arqueo) usando marcarPendienteArqueo del DAO.
     * - Lanza sync con Firebase.
     */
    suspend fun finalizarMercadilloEnCurso(mercadilloId: String): Boolean = withContext(Dispatchers.IO) {
            val ownedMarket = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            DemoAccess.requireOwner(ownedMarket.userId)
            check(ownedMarket.estado !in 6..7 || DemoAccess.isDemo()) { "Final market" }

        try {
            val m = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            val estado = EstadosMercadillo.Estado.fromCodigo(m.estado)
            if (estado != EstadosMercadillo.Estado.EN_CURSO) return@withContext false

            mercadilloDao.marcarPendienteArqueo(mercadilloId)
            mercadilloDao.getMercadilloById(mercadilloId)?.let { sincronizarMercadilloConFirebase(it) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finalizando mercadillo en curso: ${e.message}", e)
            false
        }
    }

    suspend fun getEstadisticasPorEstado(): Map<EstadosMercadillo.Estado, Int> = withContext(Dispatchers.IO) {
        val userId = ConfigurationManager.getCurrentUserId() ?: return@withContext emptyMap()
        mercadilloDao.getEstadisticasPorEstado(userId).associate { (estado, cantidad) ->
            (EstadosMercadillo.Estado.fromCodigo(estado)
                ?: EstadosMercadillo.Estado.PROGRAMADO_PARCIAL) to cantidad
        }
    }

    // ===== Sync sin autoestados / consultas =====
    suspend fun sincronizarSinEstadosAutomaticos(): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = ConfigurationManager.getCurrentUserId() ?: return@withContext false
            // El gesto solicita el motor común; nunca promete restauración a una cuenta Free.
            if (!DemoAccess.canSync(userId)) return@withContext false
            es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
            if (!connectivityObserver.isConnected.first()) return@withContext false
            SyncEngine(appContext).sync(userId)
            RecoveryGate(appContext).canPull(userId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) { Log.e(TAG, "Sync sin autoestados pendiente", e); false }
    }

    suspend fun getMercadillosPorFecha(userId: String, fecha: String): List<MercadilloEntity> =
        withContext(Dispatchers.IO) { mercadilloDao.getMercadillosByUserAndFecha(userId, fecha) }

    // ===== Híbrido Room/Firebase =====
    suspend fun getHybridMercadillos(userId: String): List<MercadilloEntity> = withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(userId)
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
        mercadilloDao.getMercadillosByUser(userId).first()
    }

    // ===== Sync Firebase =====
    private suspend fun sincronizarMercadilloConFirebase(mercadillo: MercadilloEntity) {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    private suspend fun eliminarMercadilloDeFirebase(mercadilloId: String) {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    private suspend fun sincronizarMercadillosNoSincronizados(userId: String) {
        try {
            val pendientes = mercadilloDao.getMercadillosNoSincronizadosByUser(userId)
            pendientes.forEach { sincronizarMercadilloConFirebase(it); delay(100) }
        } catch (_: Exception) { }
    }

    private suspend fun descargarMercadillosDesdeFirebase(userId: String): List<MercadilloEntity> = if (!DemoAccess.canSync(userId)) emptyList() else try {
        val snapshot = FirebaseFirestore.getInstance().collection("mercadillos")
            .whereEqualTo("userId", userId)
            .whereEqualTo("activo", true)
            .get().await()
        snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            MercadilloEntity(
                idMercadillo = data["idMercadillo"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                fecha = data["fecha"] as? String ?: "",
                lugar = data["lugar"] as? String ?: "",
                organizador = data["organizador"] as? String ?: "",
                esGratis = data["esGratis"] as? Boolean ?: true,
                importeSuscripcion = (data["importeSuscripcion"] as? Number)?.toDouble() ?: 0.0,
                requiereMesa = data["requiereMesa"] as? Boolean ?: true,
                requiereCarpa = data["requiereCarpa"] as? Boolean ?: true,
                hayPuntoLuz = data["hayPuntoLuz"] as? Boolean ?: false,
                horaInicio = data["horaInicio"] as? String ?: "09:00",
                horaFin = data["horaFin"] as? String ?: "14:00",
                estado = (data["estado"] as? Number)?.toInt() ?: 1,
                pendienteArqueo = data["pendienteArqueo"] as? Boolean ?: false,
                pendienteAsignarSaldo = data["pendienteAsignarSaldo"] as? Boolean ?: false,
                saldoInicial = (data["saldoInicial"] as? Number)?.toDouble(),
                saldoFinal = (data["saldoFinal"] as? Number)?.toDouble(),
                arqueoCaja = (data["arqueoCaja"] as? Number)?.toDouble(),
                totalVentas = (data["totalVentas"] as? Number)?.toDouble() ?: 0.0,
                totalGastos = (data["totalGastos"] as? Number)?.toDouble() ?: 0.0,
                arqueoMercadillo = (data["arqueoMercadillo"] as? Number)?.toDouble(),
                activo = data["activo"] as? Boolean ?: true,
                version = (data["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (data["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }
    } catch (_: Exception) { emptyList() }

    /** ✅ NUEVO: saber si Room está vacío para un usuario (sin tocar DAOs). */
    suspend fun isRoomEmptyForUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            mercadilloDao.getMercadillosByUser(userId).first().isEmpty()
        } catch (_: Exception) { false }
    }

    // ===== Utilidades de fecha (TZ Europe/Madrid) =====
    private fun parseFechaStartOfDay(fecha: String): Long? = try {
        val d = dateFormat.parse(fecha) ?: return null
        Calendar.getInstance(tz, Locale.getDefault()).apply {
            time = d
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (_: Exception) { null }

    private fun endOfWindowMillis(fechaStartMillis: Long): Long =
        Calendar.getInstance(tz, Locale.getDefault()).apply {
            timeInMillis = fechaStartMillis
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 5); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ===== Helpers para Arqueo / Ventas =====

    /**
     * Si hay recibos COMPLETADOS > 0 y totalVentas==0 → recalcula desde recibos y actualiza la fila.
     * Devuelve true si ha corregido algo.
     */

    suspend fun corregirTotalVentasSiIncongruente(mercadilloId: String): Boolean = withContext(Dispatchers.IO) {
            val ownedMarket = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            DemoAccess.requireOwner(ownedMarket.userId)
            check(ownedMarket.estado !in 6..7 || DemoAccess.isDemo()) { "Final market" }

        try {
            val m = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false

            val tieneLineas = lineasVentaDao.contarLineasPorMercadillo(mercadilloId) > 0
            if (!tieneLineas) return@withContext false

            val nuevoTotal = mercadilloDao.calcularTotalVentasDesdeRecibos(mercadilloId)

            if (kotlin.math.abs(m.totalVentas - nuevoTotal) > 0.009) {
                val now = System.currentTimeMillis()
                mercadilloDao.actualizarTotalVentas(mercadilloId, nuevoTotal, now)
                mercadilloDao.getMercadilloById(mercadilloId)?.let { sincronizarMercadilloConFirebase(it) }
                true
            } else {
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error corrigiendo totalVentas: ${e.message}", e)
            false
        }
    }
//                true
//            } else false
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error corrigiendo totalVentas: ${e.message}", e)
//            false

    /** Total de ventas por método de pago (EFECTIVO/BIZUM/TARJETA). */
    suspend fun getTotalVentasPorMetodo(mercadilloId: String, metodo: String): Double = withContext(Dispatchers.IO) {
        when (metodo.uppercase(Locale.ROOT)) {
            "EFECTIVO" -> mercadilloDao.getTotalVentasPorMetodoEF(mercadilloId)
            "BIZUM"    -> mercadilloDao.getTotalVentasPorMetodoBIZ(mercadilloId)
            "TARJETA"  -> mercadilloDao.getTotalVentasPorMetodoTAR(mercadilloId)
            else -> 0.0
        }
    }

    /** Snapshot (no Flow) de mercadillos por estado para el usuario (activos, orden fecha asc). */
    suspend fun getMercadillosDeUsuarioPorEstadoSnapshot(userId: String, estado: Int): List<MercadilloEntity> =
        withContext(Dispatchers.IO) { mercadilloDao.getMercadillosByUserAndEstado(userId, estado).first() }
}
