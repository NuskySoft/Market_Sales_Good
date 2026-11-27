// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/MercadilloDao.kt

package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Persistencia de mercadillos, estados, saldos, arqueos y totales.

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO principal de Mercadillos.
 *
 * IMPORTANTE:
 * Los totales de ventas ya NO se calculan desde recibos.totalTicket.
 *
 * Motivo:
 * - Las ventas normales generan líneas con subtotal positivo.
 * - Los abonos generan líneas con cantidad negativa y subtotal negativo.
 * - Por tanto, para que el arqueo cuadre, hay que sumar lineas_venta.subtotal.
 *
 * La tabla recibos se sigue usando para saber:
 * - A qué mercadillo pertenece el recibo.
 * - Si está COMPLETADO.
 * - Si está activo.
 * - Qué método de pago tiene: EFECTIVO, BIZUM o TARJETA.
 */
@Dao
interface MercadilloDao {
    /** Cuenta altas del año de celebración, incluidas las bajas lógicas y cancelaciones.
     * Excluir un identificador permite comprobar un cambio de fecha sin contar dos veces el mismo registro. */
    @Query("SELECT COUNT(*) FROM mercadillos WHERE userId = :uid AND fechaEpochDay >= :fromDay AND fechaEpochDay < :untilDay AND idMercadillo != :exceptId")
    suspend fun contarAnio(uid: String, fromDay: Long, untilDay: Long, exceptId: String): Int

    // Solo los registros vigentes ocupan una fecha concreta; se excluye el que se está editando.
    @Query("SELECT EXISTS(SELECT 1 FROM mercadillos WHERE userId = :uid AND fechaEpochDay = :day AND activo = 1 AND idMercadillo != :exceptId)")
    suspend fun ocupaFecha(uid: String, day: Long, exceptId: String): Boolean

    // Candidatos futuros de un usuario Free; la revisión no afecta a mercadillos de hoy ni históricos.
    @Query("SELECT * FROM mercadillos WHERE userId = :uid AND activo = 1 AND estado IN (1, 2, 3, 4, 5) AND fechaEpochDay > :today ORDER BY fechaEpochDay, creadoEn, idMercadillo")
    suspend fun planificadosFuturos(uid: String, today: Long): List<MercadilloEntity>

    // No retirar planificación si existe cualquier operación económica o saldo vinculado.
    @Query("SELECT EXISTS(SELECT 1 FROM recibos WHERE idUsuario = :uid AND idMercadillo = :market UNION ALL SELECT 1 FROM lineas_venta WHERE idUsuario = :uid AND idMercadillo = :market UNION ALL SELECT 1 FROM LineasGastos WHERE idUsuario = :uid AND idMercadillo = :market UNION ALL SELECT 1 FROM saldos_guardados WHERE idUsuario = :uid AND (idMercadilloOrigen = :market OR idMercadilloDestino = :market))")
    suspend fun tieneOperaciones(uid: String, market: String): Boolean

    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM mercadillos WHERE userId = :uid")
    suspend fun contarDemo(uid: String): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MercadilloEntity>)

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM mercadillos WHERE userId = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // ========== OPERACIONES BÁSICAS ==========

    @Upsert
    suspend fun upsert(mercadillo: MercadilloEntity)

    // Consulta local de lectura; comprueba la existencia de registros del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT EXISTS(SELECT 1 FROM mercadillos WHERE userId = :uid LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMercadillo(mercadillo: MercadilloEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mercadillo: MercadilloEntity)

    @Update
    suspend fun updateMercadillo(mercadillo: MercadilloEntity)

    @Delete
    suspend fun deleteMercadillo(mercadillo: MercadilloEntity)

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET activo = 0,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun desactivarMercadillo(
        id: String,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // ========== CONSULTAS FILTRADAS POR USUARIO ==========

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND fecha = :fecha
          AND activo = 1
    """)
    suspend fun getMercadillosByUserAndFecha(
        userId: String,
        fecha: String
    ): List<MercadilloEntity>

    // Consulta local de lectura; devuelve el recuento. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COUNT(*) > 0
        FROM mercadillos
        WHERE userId = :userId
          AND fecha = :fecha
          AND activo = 1
    """)
    suspend fun existeMercadilloEnFecha(
        userId: String,
        fecha: String
    ): Boolean

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND activo = 1
        ORDER BY fechaEpochDay DESC, horaInicio DESC
    """)
    fun getMercadillosByUser(userId: String): Flow<List<MercadilloEntity>>

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND fecha LIKE :mesPattern
          AND activo = 1
        ORDER BY fechaEpochDay ASC
    """)
    fun getMercadillosByUserAndMes(
        userId: String,
        mesPattern: String
    ): Flow<List<MercadilloEntity>>

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun getMercadilloById(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): MercadilloEntity?

    // Eliminación local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        DELETE FROM mercadillos
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun deleteMercadilloById(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET totalGastos = :total,
            version = version + 1,
            lastModified = strftime('%s','now') * 1000,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :idMercadillo
     AND userId = :sessionOwner """)
    suspend fun actualizarTotalGastos(
        idMercadillo: String,
        total: Double,
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // ========== CONSULTAS POR ESTADO ==========

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND estado = :estado
          AND activo = 1
        ORDER BY fechaEpochDay ASC
    """)
    fun getMercadillosByUserAndEstado(
        userId: String,
        estado: Int
    ): Flow<List<MercadilloEntity>>

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND saldoInicial IS NULL
          AND activo = 1
        ORDER BY fechaEpochDay ASC
    """)
    fun getMercadillosProgramadosParciales(userId: String): Flow<List<MercadilloEntity>>

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND pendienteArqueo = 1
          AND activo = 1
        ORDER BY fechaEpochDay ASC
    """)
    fun getMercadillosPendientesArqueo(userId: String): Flow<List<MercadilloEntity>>

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND pendienteAsignarSaldo = 1
          AND activo = 1
        ORDER BY fechaEpochDay ASC
    """)
    fun getMercadillosPendientesAsignarSaldo(userId: String): Flow<List<MercadilloEntity>>

    // ========== LÓGICA DE NEGOCIO ==========

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND fechaEpochDay >= CAST(julianday(substr(:fechaActual,7,4)||'-'||substr(:fechaActual,4,2)||'-'||substr(:fechaActual,1,2))-2440587.5 AS INTEGER)
          AND activo = 1
        ORDER BY fechaEpochDay ASC, horaInicio ASC
        LIMIT 1
    """)
    suspend fun getProximoMercadillo(
        userId: String,
        fechaActual: String
    ): MercadilloEntity?

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND fecha = :fecha
          AND activo = 1
        ORDER BY horaInicio ASC
    """)
    suspend fun getMercadillosByFecha(
        userId: String,
        fecha: String
    ): List<MercadilloEntity>

    // Consulta local de lectura; devuelve el recuento. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COUNT(*) > 0
        FROM mercadillos
        WHERE userId = :userId
          AND fecha = :fecha
          AND horaInicio = :horaInicio
          AND activo = 1
          AND idMercadillo != :excludeId
    """)
    suspend fun existeMercadilloEnFechaHora(
        userId: String,
        fecha: String,
        horaInicio: String,
        excludeId: String = ""
    ): Boolean

    // ========== SINCRONIZACIÓN ==========

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND sincronizadoFirebase = 0
    """)
    suspend fun getMercadillosNoSincronizadosByUser(userId: String): List<MercadilloEntity>

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET sincronizadoFirebase = 1,
            lastModified = :timestamp
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun marcarComoSincronizado(
        id: String,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun marcarComoNoSincronizado(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT version
        FROM mercadillos
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun getMercadilloVersion(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Long?

    // ========== ESTADOS / SALDOS ==========

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET saldoInicial = :saldoInicial,
            estado = 2,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun asignarSaldoInicial(
        id: String,
        saldoInicial: Double,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET estado = :nuevoEstado,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun actualizarEstado(
        id: String,
        nuevoEstado: Int,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET arqueoCaja = :arqueoCaja,
            saldoFinal = :saldoFinal,
            pendienteArqueo = 0,
            pendienteAsignarSaldo = 1,
            estado = 5,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun realizarArqueoCaja(
        id: String,
        arqueoCaja: Double,
        saldoFinal: Double,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET pendienteAsignarSaldo = 0,
            estado = 6,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun marcarSaldoAsignado(
        id: String,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET totalVentas = :totalVentas,
            totalGastos = :totalGastos,
            arqueoMercadillo = :arqueoMercadillo,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun actualizarTotales(
        id: String,
        totalVentas: Double,
        totalGastos: Double,
        arqueoMercadillo: Double,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET estado = 7,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun cancelarMercadillo(
        id: String,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET estado = 4,
            pendienteArqueo = 1,
            version = version + 1,
            lastModified = :timestamp,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :id
     AND userId = :sessionOwner """)
    suspend fun marcarPendienteArqueo(
        id: String,
        timestamp: Long = System.currentTimeMillis(),
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM mercadillos WHERE userId = :uid AND activo = 1 AND estado IN (1, 2, 3)")
    suspend fun pendientesAutoestado(uid: String): List<MercadilloEntity>

    // ========== UTILIDADES ==========

    // Consulta local de lectura; devuelve el recuento. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COUNT(*)
        FROM mercadillos
        WHERE userId = :userId
          AND activo = 1
    """)
    suspend fun getMercadilloCountByUser(userId: String): Int

    // Consulta local de lectura; devuelve el recuento. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT estado, COUNT(*) AS cantidad
        FROM mercadillos
        WHERE userId = :userId
          AND activo = 1
        GROUP BY estado
    """)
    suspend fun getEstadisticasPorEstado(userId: String): List<EstadisticaEstado>

    // ========== MANTENIMIENTO ==========

    // Eliminación local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        DELETE FROM mercadillos
        WHERE userId = :userId
          AND estado = 7
          AND lastModified < :limiteFecha
    """)
    suspend fun limpiarMercadillosCanceladosAntiguos(
        userId: String,
        limiteFecha: Long
    )

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET sincronizadoFirebase = 0
        WHERE userId = :userId
    """)
    suspend fun resetearSincronizacion(userId: String)

    // Consulta local de lectura; devuelve el recuento. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COUNT(*)
        FROM mercadillos
        WHERE userId = :uid
    """)
    suspend fun contarPorUsuario(uid: String): Int

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND saldoFinal IS NOT NULL
          AND pendienteAsignarSaldo = 1
          AND activo = 1
        ORDER BY fechaEpochDay DESC, horaFin DESC
        LIMIT 1
    """)
    suspend fun getUltimoMercadilloConSaldoFinal(userId: String): MercadilloEntity?

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT *
        FROM mercadillos
        WHERE userId = :userId
          AND fechaEpochDay >= CAST(julianday(substr(:fechaActual,7,4)||'-'||substr(:fechaActual,4,2)||'-'||substr(:fechaActual,1,2))-2440587.5 AS INTEGER)
          AND activo = 1
        ORDER BY fechaEpochDay ASC, horaInicio ASC
    """)
    suspend fun getMercadillosDesdeHoy(
        userId: String,
        fechaActual: String
    ): List<MercadilloEntity>

    // ========== VENTAS Y ABONOS ==========
    //
    // Estas consultas están preparadas para que los abonos descuenten correctamente.
    //
    // Antes se sumaba recibos.totalTicket.
    // Ahora se suma lineas_venta.subtotal.
    //
    // Así:
    // - Venta normal: cantidad positiva, subtotal positivo.
    // - Abono: cantidad negativa, subtotal negativo.
    // - Total real: SUM(subtotal).

    // Consulta local de lectura; devuelve el recuento. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COUNT(*)
        FROM recibos
        WHERE idMercadillo = :mercadilloId
          AND estado = 'COMPLETADO'
          AND activo = 1
     AND idUsuario = :sessionOwner """)
    suspend fun contarRecibosPorMercadillo(mercadilloId: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Int

    // Consulta local de lectura; agrega los importes o cantidades seleccionados. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COALESCE((SUM(CAST(ROUND(lv.subtotal * 100, 0) AS INTEGER)) / 100.0), 0)
        FROM lineas_venta AS lv
        INNER JOIN recibos AS r
            ON r.idRecibo = lv.idRecibo
           AND r.idMercadillo = lv.idMercadillo
        WHERE lv.idMercadillo = :mercadilloId
          AND lv.activo = 1
          AND r.activo = 1
          AND r.estado = 'COMPLETADO'
     AND lv.idUsuario = :sessionOwner AND r.idUsuario = :sessionOwner """)
    suspend fun calcularTotalVentasDesdeRecibos(mercadilloId: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Double

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        UPDATE mercadillos
        SET totalVentas = :total,
            version = version + 1,
            lastModified = :now,
            sincronizadoFirebase = 0
        WHERE idMercadillo = :mercadilloId
     AND userId = :sessionOwner """)
    suspend fun actualizarTotalVentas(
        mercadilloId: String,
        total: Double,
        now: Long,
        sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // Consulta local de lectura; agrega los importes o cantidades seleccionados. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COALESCE((SUM(CAST(ROUND(lv.subtotal * 100, 0) AS INTEGER)) / 100.0), 0)
        FROM lineas_venta AS lv
        INNER JOIN recibos AS r
            ON r.idRecibo = lv.idRecibo
           AND r.idMercadillo = lv.idMercadillo
        WHERE lv.idMercadillo = :mercadilloId
          AND lv.activo = 1
          AND r.activo = 1
          AND r.estado = 'COMPLETADO'
          AND UPPER(r.metodoPago) = 'EFECTIVO'
     AND lv.idUsuario = :sessionOwner AND r.idUsuario = :sessionOwner """)
    suspend fun getTotalVentasPorMetodoEF(mercadilloId: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Double

    // Consulta local de lectura; agrega los importes o cantidades seleccionados. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COALESCE((SUM(CAST(ROUND(lv.subtotal * 100, 0) AS INTEGER)) / 100.0), 0)
        FROM lineas_venta AS lv
        INNER JOIN recibos AS r
            ON r.idRecibo = lv.idRecibo
           AND r.idMercadillo = lv.idMercadillo
        WHERE lv.idMercadillo = :mercadilloId
          AND lv.activo = 1
          AND r.activo = 1
          AND r.estado = 'COMPLETADO'
          AND UPPER(r.metodoPago) = 'BIZUM'
     AND lv.idUsuario = :sessionOwner AND r.idUsuario = :sessionOwner """)
    suspend fun getTotalVentasPorMetodoBIZ(mercadilloId: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Double

    // Consulta local de lectura; agrega los importes o cantidades seleccionados. Los filtros y parámetros de la consulta definen su alcance.
    @Query("""
        SELECT COALESCE((SUM(CAST(ROUND(lv.subtotal * 100, 0) AS INTEGER)) / 100.0), 0)
        FROM lineas_venta AS lv
        INNER JOIN recibos AS r
            ON r.idRecibo = lv.idRecibo
           AND r.idMercadillo = lv.idMercadillo
        WHERE lv.idMercadillo = :mercadilloId
          AND lv.activo = 1
          AND r.activo = 1
          AND r.estado = 'COMPLETADO'
          AND UPPER(r.metodoPago) = 'TARJETA'
     AND lv.idUsuario = :sessionOwner AND r.idUsuario = :sessionOwner """)
    suspend fun getTotalVentasPorMetodoTAR(mercadilloId: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Double
}

/**
 * Estadística simple por estado del mercadillo.
 */
data class EstadisticaEstado(
    val estado: Int,
    val cantidad: Int
)
