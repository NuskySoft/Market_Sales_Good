


// app/src/main/java/es/nuskysoftware/marketsales/data/repository/MercadilloRepository.kt
package es.nuskysoftware.marketsales.data.repository
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
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
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
        // Sincronización básica al recuperar conexión
        repositoryScope.launch {
            connectivityObserver.isConnected.collect { online ->
                if (online) {
                    val userId = ConfigurationManager.getCurrentUserId()
                    if (userId != null) {
                        sincronizarMercadillosNoSincronizados(userId)
                        //actualizarEstadosAutomaticos(userId)
                    }
                }
            }
        }
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
        saldoInicial: Double? = null
    ): String = withContext(Dispatchers.IO) {
        val userId = ConfigurationManager.getCurrentUserId()
            ?: throw IllegalStateException(es.nuskysoftware.marketsales.utils.StringResourceManager.getString("sin_usuario_mercadillo", ConfigurationManager.idioma.value))

        val esPremium = ConfigurationManager.getIsPremium()
        if (!esPremium) {
            val yaHayEseDia = mercadilloDao.existeMercadilloEnFecha(userId, fecha)
            if (yaHayEseDia) throw IllegalArgumentException(
                es.nuskysoftware.marketsales.utils.StringResourceManager.getString("limite_free_mercadillos", ConfigurationManager.idioma.value)
            )
        }

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
            sincronizadoFirebase = false
        )

        localDb.withTransaction {
                DemoAccess.requireOwner(userId)
                DemoAccess.checkLimit("mercadillos", mercadilloDao.contarDemo(userId))
                mercadilloDao.insertMercadillo(nuevo)
            }
        repositoryScope.launch { sincronizarMercadilloConFirebase(nuevo) }
        nuevo.idMercadillo
    }

    suspend fun actualizarMercadillo(mercadillo: MercadilloEntity): Boolean = withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(mercadillo.userId)
        val actualizado = mercadillo.copy(
            version = mercadillo.version + 1,
            lastModified = System.currentTimeMillis(),
            sincronizadoFirebase = false
        )
        mercadilloDao.updateMercadillo(actualizado)
        repositoryScope.launch { sincronizarMercadilloConFirebase(actualizado) }
        true
    }

    suspend fun eliminarMercadillo(mercadillo: MercadilloEntity): Boolean = withContext(Dispatchers.IO) {
        DemoAccess.requireRegistered("demo_no_borrar_mercadillo")
        mercadilloDao.deleteMercadillo(mercadillo)
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
    suspend fun asignarSaldoInicial(mercadilloId: String, saldoInicial: Double, esAsignacionAutomatica: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
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
            val m = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
            val si = m.saldoInicial ?: return@withContext false
            val arqueoCaja = si + ventasEfectivo - gastosEfectivo
            mercadilloDao.realizarArqueoCaja(mercadilloId, arqueoCaja, saldoFinal)
            mercadilloDao.getMercadilloById(mercadilloId)?.let { sincronizarMercadilloConFirebase(it) }
            true
        }

    /** Cierra con saldo final y marca estado=6 (pendienteAsignarSaldo=0). */
    suspend fun cerrarConSaldoFinal(mercadilloId: String, saldoFinal: Double): Boolean = withContext(Dispatchers.IO) {
        mercadilloDao.marcarSaldoAsignado(mercadilloId)
        val m = mercadilloDao.getMercadilloById(mercadilloId)
        if (m != null) sincronizarMercadilloConFirebase(m)
        true
    }

    // ===== AUTOESTADOS =====

    suspend fun actualizarEstadosAutomaticos(userId: String) = withContext(Dispatchers.IO) {
        try {
            val lista = mercadilloDao.getMercadillosByUser(userId).first()
            val now = System.currentTimeMillis()
            val hoyStr = dateFormat.format(Date(now))
            var cambios = 0

            for (m in lista) {
                // ⛔️ SALVAGUARDA: nunca degradar estados ≥ 5 (5=pte asignar saldo, 6=cerrado)
                if (m.estado >= 5) continue

                val fechaStart = parseFechaStartOfDay(m.fecha) ?: continue
                val ventanaInicio = fechaStart                 // 00:00 local
                val ventanaFin = endOfWindowMillis(fechaStart) // 05:00 del día siguiente

                when {
                    // 1/2 -> 3 si hoy o dentro de ventana
                    (m.estado == 1 || m.estado == 2) &&
                            (m.fecha == hoyStr || now in ventanaInicio until ventanaFin) -> {
                        mercadilloDao.actualizarEstado(m.idMercadillo, EstadosMercadillo.Estado.EN_CURSO.codigo)
                        cambios++
                        mercadilloDao.getMercadilloById(m.idMercadillo)?.let { sincronizarMercadilloConFirebase(it) }
                    }

                    // Fuera de ventana: si hubo ventas -> 4, si no -> 7
                    now >= ventanaFin -> {
                        val ventas = lineasVentaDao.contarLineasPorMercadillo(m.idMercadillo)
                        if (ventas > 0) mercadilloDao.marcarPendienteArqueo(m.idMercadillo)
                        else mercadilloDao.cancelarMercadillo(m.idMercadillo)
                        cambios++
                        mercadilloDao.getMercadilloById(m.idMercadillo)?.let { sincronizarMercadilloConFirebase(it) }
                    }
                }
            }

            if (cambios > 0) Log.d(TAG, "✅ Autoestados aplicados: $cambios")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Autoestados", e)
        }
    }

    suspend fun confirmarArqueoCaja(
        mercadilloId: String,
        arqueoFinal: Double,
        nuevoEstado: Int
    ): Boolean = withContext(Dispatchers.IO) {
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
            sincronizarMercadillosNoSincronizados(userId)
            getHybridMercadillos(userId)
            Log.d(TAG, "✅ Sync sin autoestados")
            true
        } catch (e: Exception) { Log.e(TAG, "❌ Sync sin autoestados", e); false }
    }

    suspend fun getMercadillosPorFecha(userId: String, fecha: String): List<MercadilloEntity> =
        withContext(Dispatchers.IO) { mercadilloDao.getMercadillosByUserAndFecha(userId, fecha) }

    // ===== Híbrido Room/Firebase =====
    suspend fun getHybridMercadillos(userId: String): List<MercadilloEntity> = withContext(Dispatchers.IO) {
        if (!DemoAccess.mayRestore(localDb, userId)) return@withContext mercadilloDao.getMercadillosByUser(userId).first()

        try {
            val pendientes = mercadilloDao.getMercadillosNoSincronizadosByUser(userId)
            if (pendientes.isNotEmpty()) return@withContext mercadilloDao.getMercadillosByUser(userId).first()
            if (connectivityObserver.isConnected.first()) {
                try {
                    val remote = descargarMercadillosDesdeFirebase(userId)
                    if (remote.isNotEmpty()) {
                        remote.forEach { mercadilloDao.insertOrUpdate(it.copy(sincronizadoFirebase = true)) }
                        Log.d(TAG, "✅ Merge desde Firebase")
                    }
                } catch (_: Exception) { }
            }
            mercadilloDao.getMercadillosByUser(userId).first()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Hybrid", e)
            mercadilloDao.getMercadillosByUser(userId).first()
        }
    }

    // ===== Sync Firebase =====
    private suspend fun sincronizarMercadilloConFirebase(mercadillo: MercadilloEntity) {
        if (!DemoAccess.canSync(mercadillo.userId)) return
        try {
            if (!connectivityObserver.isConnected.first()) return
            val datos = mapOf(
                "idMercadillo" to mercadillo.idMercadillo,
                "userId" to mercadillo.userId,
                "fecha" to mercadillo.fecha,
                "lugar" to mercadillo.lugar,
                "organizador" to mercadillo.organizador,
                "esGratis" to mercadillo.esGratis,
                "importeSuscripcion" to mercadillo.importeSuscripcion,
                "requiereMesa" to mercadillo.requiereMesa,
                "requiereCarpa" to mercadillo.requiereCarpa,
                "hayPuntoLuz" to mercadillo.hayPuntoLuz,
                "horaInicio" to mercadillo.horaInicio,
                "horaFin" to mercadillo.horaFin,
                "estado" to mercadillo.estado,
                "pendienteArqueo" to mercadillo.pendienteArqueo,
                "pendienteAsignarSaldo" to mercadillo.pendienteAsignarSaldo,
                "saldoInicial" to mercadillo.saldoInicial,
                "saldoFinal" to mercadillo.saldoFinal,
                "arqueoCaja" to mercadillo.arqueoCaja,
                "totalVentas" to mercadillo.totalVentas,
                "totalGastos" to mercadillo.totalGastos,
                "arqueoMercadillo" to mercadillo.arqueoMercadillo,
                "activo" to mercadillo.activo,
                "version" to mercadillo.version,
                "lastModified" to mercadillo.lastModified,
                "fechaSync" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance()
                .collection("mercadillos")
                .document(mercadillo.idMercadillo)
                .set(datos).await()
            mercadilloDao.marcarComoSincronizado(mercadillo.idMercadillo)
        } catch (_: Exception) { }
    }

    private suspend fun eliminarMercadilloDeFirebase(mercadilloId: String) {
        if (!DemoAccess.canSync(ConfigurationManager.getCurrentUserId())) return
        try {
            if (connectivityObserver.isConnected.first()) {
                FirebaseFirestore.getInstance()
                    .collection("mercadillos")
                    .document(mercadilloId)
                    .delete().await()
            }
        } catch (_: Exception) { }
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
//    suspend fun corregirTotalVentasSiIncongruente(mercadilloId: String): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val m = mercadilloDao.getMercadilloById(mercadilloId) ?: return@withContext false
//            val tieneRecibos = lineasVentaDao.contarLineasPorMercadillo(mercadilloId) > 0
//            if (tieneRecibos && (m.totalVentas == 0.0)) {
//                val nuevoTotal = mercadilloDao.calcularTotalVentasDesdeRecibos(mercadilloId)
//                val now = System.currentTimeMillis()
//                mercadilloDao.actualizarTotalVentas(mercadilloId, nuevoTotal, now)
//                mercadilloDao.getMercadilloById(mercadilloId)?.let { sincronizarMercadilloConFirebase(it) }
//                true
//            } else false
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error corrigiendo totalVentas: ${e.message}", e)
//            false
//        }
//    }

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
