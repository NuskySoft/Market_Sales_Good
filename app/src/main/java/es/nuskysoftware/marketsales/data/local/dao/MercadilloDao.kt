// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/MercadilloDao.kt


//// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/MercadilloDao.kt
/**
 * MercadilloDao (COMPLETO con añadidos de ventas):
 * - Mantiene TODO lo que ya tenías.
 * - Añade:
 *   • contarRecibosPorMercadillo(...)
 *   • calcularTotalVentasDesdeRecibos(...)
 *   • actualizarTotalVentas(...)
 *   • getTotalVentasPorMetodoEF/BIZ/TAR(...)
 *   • marcarPendienteArqueo(...) (util cuando hay ventas al cerrar ventana)
 *
 * Copia y pega este archivo entero para evitar “unresolved” y mantener consistencia.
 */
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MercadilloDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MercadilloEntity>)

    @Query("DELETE FROM mercadillos WHERE userId = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // ========== OPERACIONES BÁSICAS ==========
    @Upsert
    suspend fun upsert(mercadillo: MercadilloEntity)

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

    // ========== CONSULTAS FILTRADAS POR USUARIO ==========
    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND fecha = :fecha AND activo = 1")
    suspend fun getMercadillosByUserAndFecha(userId: String, fecha: String): List<MercadilloEntity>

    @Query("SELECT COUNT(*) > 0 FROM mercadillos WHERE userId = :userId AND fecha = :fecha AND activo = 1")
    suspend fun existeMercadilloEnFecha(userId: String, fecha: String): Boolean

    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND activo = 1 ORDER BY fecha DESC, horaInicio DESC")
    fun getMercadillosByUser(userId: String): Flow<List<MercadilloEntity>>

    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND fecha LIKE :mesPattern AND activo = 1 ORDER BY fecha ASC")
    fun getMercadillosByUserAndMes(userId: String, mesPattern: String): Flow<List<MercadilloEntity>>

    @Query("SELECT * FROM mercadillos WHERE idMercadillo = :id")
    suspend fun getMercadilloById(id: String): MercadilloEntity?

    @Query("DELETE FROM mercadillos WHERE idMercadillo = :id")
    suspend fun deleteMercadilloById(id: String)

    @Query("""
    UPDATE mercadillos
    SET totalGastos = :total,
        version = version + 1,
        lastModified = strftime('%s','now')*1000,
        sincronizadoFirebase = 0
    WHERE idMercadillo = :idMercadillo
""")
    suspend fun actualizarTotalGastos(idMercadillo: String, total: Double)

    // ========== CONSULTAS POR ESTADO ==========
    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND estado = :estado AND activo = 1 ORDER BY fecha ASC")
    fun getMercadillosByUserAndEstado(userId: String, estado: Int): Flow<List<MercadilloEntity>>

    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND saldoInicial IS NULL AND activo = 1 ORDER BY fecha ASC")
    fun getMercadillosProgramadosParciales(userId: String): Flow<List<MercadilloEntity>>

    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND pendienteArqueo = 1 AND activo = 1 ORDER BY fecha ASC")
    fun getMercadillosPendientesArqueo(userId: String): Flow<List<MercadilloEntity>>

    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND pendienteAsignarSaldo = 1 AND activo = 1 ORDER BY fecha ASC")
    fun getMercadillosPendientesAsignarSaldo(userId: String): Flow<List<MercadilloEntity>>

    // ========== LÓGICA DE NEGOCIO ==========
    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND fecha >= :fechaActual AND activo = 1 ORDER BY fecha ASC, horaInicio ASC LIMIT 1")
    suspend fun getProximoMercadillo(userId: String, fechaActual: String): MercadilloEntity?

    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND fecha = :fecha AND activo = 1 ORDER BY horaInicio ASC")
    suspend fun getMercadillosByFecha(userId: String, fecha: String): List<MercadilloEntity>

    @Query("""
        SELECT COUNT(*) > 0 FROM mercadillos
        WHERE userId = :userId AND fecha = :fecha AND horaInicio = :horaInicio AND activo = 1
        AND idMercadillo != :excludeId
    """)
    suspend fun existeMercadilloEnFechaHora(userId: String, fecha: String, horaInicio: String, excludeId: String = ""): Boolean

    // ========== SINCRONIZACIÓN ==========
    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND sincronizadoFirebase = 0")
    suspend fun getMercadillosNoSincronizadosByUser(userId: String): List<MercadilloEntity>

    @Query("UPDATE mercadillos SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idMercadillo = :id")
    suspend fun marcarComoSincronizado(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE mercadillos SET sincronizadoFirebase = 0 WHERE idMercadillo = :id")
    suspend fun marcarComoNoSincronizado(id: String)

    @Query("SELECT version FROM mercadillos WHERE idMercadillo = :id")
    suspend fun getMercadilloVersion(id: String): Long?

    // ========== ESTADOS / SALDOS ==========
    @Query("UPDATE mercadillos SET saldoInicial = :saldoInicial, estado = 2, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
    suspend fun asignarSaldoInicial(id: String, saldoInicial: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE mercadillos SET estado = :nuevoEstado, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
    suspend fun actualizarEstado(id: String, nuevoEstado: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE mercadillos SET arqueoCaja = :arqueoCaja, saldoFinal = :saldoFinal, pendienteArqueo = 0, pendienteAsignarSaldo = 1, estado = 5, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
    suspend fun realizarArqueoCaja(id: String, arqueoCaja: Double, saldoFinal: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE mercadillos SET pendienteAsignarSaldo = 0, estado = 6, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
    suspend fun marcarSaldoAsignado(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE mercadillos SET totalVentas = :totalVentas, totalGastos = :totalGastos, arqueoMercadillo = :arqueoMercadillo, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
    suspend fun actualizarTotales(id: String, totalVentas: Double, totalGastos: Double, arqueoMercadillo: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE mercadillos SET estado = 7, /*activo = 0,*/ version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
    suspend fun cancelarMercadillo(id: String, timestamp: Long = System.currentTimeMillis())

    // Útil cuando cierra ventana y hay ventas: ponemos 4 + pendienteArqueo = 1
    @Query("UPDATE mercadillos SET estado = 4, pendienteArqueo = 1, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
    suspend fun marcarPendienteArqueo(id: String, timestamp: Long = System.currentTimeMillis())

    // ========== UTILIDADES ==========
    @Query("SELECT COUNT(*) FROM mercadillos WHERE userId = :userId AND activo = 1")
    suspend fun getMercadilloCountByUser(userId: String): Int

    @Query("SELECT estado, COUNT(*) as cantidad FROM mercadillos WHERE userId = :userId AND activo = 1 GROUP BY estado")
    suspend fun getEstadisticasPorEstado(userId: String): List<EstadisticaEstado>

    // ========== MANTENIMIENTO ==========
    @Query("DELETE FROM mercadillos WHERE userId = :userId AND estado = 7 AND lastModified < :limiteFecha")
    suspend fun limpiarMercadillosCanceladosAntiguos(userId: String, limiteFecha: Long)

    @Query("UPDATE mercadillos SET sincronizadoFirebase = 0 WHERE userId = :userId")
    suspend fun resetearSincronizacion(userId: String)


    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND saldoFinal IS NOT NULL AND pendienteAsignarSaldo = 1 AND activo = 1 ORDER BY fecha DESC, horaFin DESC LIMIT 1")
    suspend fun getUltimoMercadilloConSaldoFinal(userId: String): MercadilloEntity?

    @Query("""
        SELECT * FROM mercadillos 
        WHERE userId = :userId 
          AND fecha >= :fechaActual 
          AND activo = 1 
        ORDER BY fecha ASC, horaInicio ASC
    """)
    suspend fun getMercadillosDesdeHoy(userId: String, fechaActual: String): List<MercadilloEntity>

    // ========== VENTAS (desde recibos) ==========
    @Query("""
        SELECT COUNT(*) 
        FROM recibos 
        WHERE idMercadillo = :mercadilloId AND estado = 'COMPLETADO'
    """)
    suspend fun contarRecibosPorMercadillo(mercadilloId: String): Int

    @Query("""
        SELECT COALESCE(SUM(totalTicket), 0) 
        FROM recibos 
        WHERE idMercadillo = :mercadilloId AND estado = 'COMPLETADO'
    """)
    suspend fun calcularTotalVentasDesdeRecibos(mercadilloId: String): Double

    @Query("""
        UPDATE mercadillos 
        SET totalVentas = :total, 
            version = version + 1, 
            lastModified = :now, 
            sincronizadoFirebase = 0
        WHERE idMercadillo = :mercadilloId
    """)
    suspend fun actualizarTotalVentas(mercadilloId: String, total: Double, now: Long)

    @Query("""
        SELECT COALESCE(SUM(totalTicket), 0)
        FROM recibos
        WHERE idMercadillo = :mercadilloId
          AND estado = 'COMPLETADO'
          AND metodoPago = 'EFECTIVO'
    """)
    suspend fun getTotalVentasPorMetodoEF(mercadilloId: String): Double

    @Query("""
        SELECT COALESCE(SUM(totalTicket), 0)
        FROM recibos
        WHERE idMercadillo = :mercadilloId
          AND estado = 'COMPLETADO'
          AND metodoPago = 'BIZUM'
    """)
    suspend fun getTotalVentasPorMetodoBIZ(mercadilloId: String): Double

    @Query("""
        SELECT COALESCE(SUM(totalTicket), 0)
        FROM recibos
        WHERE idMercadillo = :mercadilloId
          AND estado = 'COMPLETADO'
          AND metodoPago = 'TARJETA'
    """)
    suspend fun getTotalVentasPorMetodoTAR(mercadilloId: String): Double

}

/** Estadística simple por estado */
data class EstadisticaEstado(
    val estado: Int,
    val cantidad: Int
)



//// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/MercadilloDao.kt
///**
// * MercadilloDao (COMPLETO con añadidos de ventas):
// * - Mantiene TODO lo que ya tenías.
// * - Añade:
// *   • contarRecibosPorMercadillo(...)
// *   • calcularTotalVentasDesdeRecibos(...)
// *   • actualizarTotalVentas(...)
// *   • getTotalVentasPorMetodoEF/BIZ/TAR(...)
// *   • marcarPendienteArqueo(...) (util cuando hay ventas al cerrar ventana)
// *
// * Copia y pega este archivo entero para evitar “unresolved” y mantener consistencia.
// */
//package es.nuskysoftware.marketsales.data.local.dao
//
//import androidx.room.*
//import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface MercadilloDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsertAll(items: List<MercadilloEntity>)
//
//    @Query("DELETE FROM mercadillos WHERE userId = :userId")
//    suspend fun borrarPorUsuario(userId: String)
//
//    // ========== OPERACIONES BÁSICAS ==========
//    @Upsert
//    suspend fun upsert(mercadillo: MercadilloEntity)
//
//    @Query("SELECT EXISTS(SELECT 1 FROM mercadillos WHERE userId = :uid LIMIT 1)")
//    suspend fun existeAlgunoDeUsuario(uid: String): Boolean
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertMercadillo(mercadillo: MercadilloEntity)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertOrUpdate(mercadillo: MercadilloEntity)
//
//    @Update
//    suspend fun updateMercadillo(mercadillo: MercadilloEntity)
//
//    @Delete
//    suspend fun deleteMercadillo(mercadillo: MercadilloEntity)
//
//    // ========== CONSULTAS FILTRADAS POR USUARIO ==========
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND fecha = :fecha AND activo = 1")
//    suspend fun getMercadillosByUserAndFecha(userId: String, fecha: String): List<MercadilloEntity>
//
//    @Query("SELECT COUNT(*) > 0 FROM mercadillos WHERE userId = :userId AND fecha = :fecha AND activo = 1")
//    suspend fun existeMercadilloEnFecha(userId: String, fecha: String): Boolean
//
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND activo = 1 ORDER BY fecha DESC, horaInicio DESC")
//    fun getMercadillosByUser(userId: String): Flow<List<MercadilloEntity>>
//
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND fecha LIKE :mesPattern AND activo = 1 ORDER BY fecha ASC")
//    fun getMercadillosByUserAndMes(userId: String, mesPattern: String): Flow<List<MercadilloEntity>>
//
//    @Query("SELECT * FROM mercadillos WHERE idMercadillo = :id")
//    suspend fun getMercadilloById(id: String): MercadilloEntity?
//
//    @Query("DELETE FROM mercadillos WHERE idMercadillo = :id")
//    suspend fun deleteMercadilloById(id: String)
//
//    @Query("""
//    UPDATE mercadillos
//    SET totalGastos = :total,
//        version = version + 1,
//        lastModified = strftime('%s','now')*1000,
//        sincronizadoFirebase = 0
//    WHERE idMercadillo = :idMercadillo
//""")
//    suspend fun actualizarTotalGastos(idMercadillo: String, total: Double)
//
//    // ========== CONSULTAS POR ESTADO ==========
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND estado = :estado AND activo = 1 ORDER BY fecha ASC")
//    fun getMercadillosByUserAndEstado(userId: String, estado: Int): Flow<List<MercadilloEntity>>
//
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND saldoInicial IS NULL AND activo = 1 ORDER BY fecha ASC")
//    fun getMercadillosProgramadosParciales(userId: String): Flow<List<MercadilloEntity>>
//
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND pendienteArqueo = 1 AND activo = 1 ORDER BY fecha ASC")
//    fun getMercadillosPendientesArqueo(userId: String): Flow<List<MercadilloEntity>>
//
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND pendienteAsignarSaldo = 1 AND activo = 1 ORDER BY fecha ASC")
//    fun getMercadillosPendientesAsignarSaldo(userId: String): Flow<List<MercadilloEntity>>
//
//    // ========== LÓGICA DE NEGOCIO ==========
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND fecha >= :fechaActual AND activo = 1 ORDER BY fecha ASC, horaInicio ASC LIMIT 1")
//    suspend fun getProximoMercadillo(userId: String, fechaActual: String): MercadilloEntity?
//
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND fecha = :fecha AND activo = 1 ORDER BY horaInicio ASC")
//    suspend fun getMercadillosByFecha(userId: String, fecha: String): List<MercadilloEntity>
//
//    @Query("""
//        SELECT COUNT(*) > 0 FROM mercadillos
//        WHERE userId = :userId AND fecha = :fecha AND horaInicio = :horaInicio AND activo = 1
//        AND idMercadillo != :excludeId
//    """)
//    suspend fun existeMercadilloEnFechaHora(userId: String, fecha: String, horaInicio: String, excludeId: String = ""): Boolean
//
//    // ========== SINCRONIZACIÓN ==========
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND sincronizadoFirebase = 0")
//    suspend fun getMercadillosNoSincronizadosByUser(userId: String): List<MercadilloEntity>
//
//    @Query("UPDATE mercadillos SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idMercadillo = :id")
//    suspend fun marcarComoSincronizado(id: String, timestamp: Long = System.currentTimeMillis())
//
//    @Query("UPDATE mercadillos SET sincronizadoFirebase = 0 WHERE idMercadillo = :id")
//    suspend fun marcarComoNoSincronizado(id: String)
//
//    @Query("SELECT version FROM mercadillos WHERE idMercadillo = :id")
//    suspend fun getMercadilloVersion(id: String): Long?
//
//    // ========== ESTADOS / SALDOS ==========
//    @Query("UPDATE mercadillos SET saldoInicial = :saldoInicial, estado = 2, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
//    suspend fun asignarSaldoInicial(id: String, saldoInicial: Double, timestamp: Long = System.currentTimeMillis())
//
//    @Query("UPDATE mercadillos SET estado = :nuevoEstado, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
//    suspend fun actualizarEstado(id: String, nuevoEstado: Int, timestamp: Long = System.currentTimeMillis())
//
//    @Query("UPDATE mercadillos SET arqueoCaja = :arqueoCaja, saldoFinal = :saldoFinal, pendienteArqueo = 0, pendienteAsignarSaldo = 1, estado = 5, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
//    suspend fun realizarArqueoCaja(id: String, arqueoCaja: Double, saldoFinal: Double, timestamp: Long = System.currentTimeMillis())
//
//    @Query("UPDATE mercadillos SET pendienteAsignarSaldo = 0, estado = 6, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
//    suspend fun marcarSaldoAsignado(id: String, timestamp: Long = System.currentTimeMillis())
//
//    @Query("UPDATE mercadillos SET totalVentas = :totalVentas, totalGastos = :totalGastos, arqueoMercadillo = :arqueoMercadillo, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
//    suspend fun actualizarTotales(id: String, totalVentas: Double, totalGastos: Double, arqueoMercadillo: Double, timestamp: Long = System.currentTimeMillis())
//
//    @Query("UPDATE mercadillos SET estado = 7, /*activo = 0,*/ version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
//    suspend fun cancelarMercadillo(id: String, timestamp: Long = System.currentTimeMillis())
//
//    // Útil cuando cierra ventana y hay ventas: ponemos 4 + pendienteArqueo = 1
//    @Query("UPDATE mercadillos SET estado = 4, pendienteArqueo = 1, version = version + 1, lastModified = :timestamp, sincronizadoFirebase = 0 WHERE idMercadillo = :id")
//    suspend fun marcarPendienteArqueo(id: String, timestamp: Long = System.currentTimeMillis())
//
//    // ========== UTILIDADES ==========
//    @Query("SELECT COUNT(*) FROM mercadillos WHERE userId = :userId AND activo = 1")
//    suspend fun getMercadilloCountByUser(userId: String): Int
//
//    @Query("SELECT estado, COUNT(*) as cantidad FROM mercadillos WHERE userId = :userId AND activo = 1 GROUP BY estado")
//    suspend fun getEstadisticasPorEstado(userId: String): List<EstadisticaEstado>
//
//    // ========== MANTENIMIENTO ==========
//    @Query("DELETE FROM mercadillos WHERE userId = :userId AND estado = 7 AND lastModified < :limiteFecha")
//    suspend fun limpiarMercadillosCanceladosAntiguos(userId: String, limiteFecha: Long)
//
//    @Query("UPDATE mercadillos SET sincronizadoFirebase = 0 WHERE userId = :userId")
//    suspend fun resetearSincronizacion(userId: String)
//
//
//    @Query("SELECT * FROM mercadillos WHERE userId = :userId AND saldoFinal IS NOT NULL AND pendienteAsignarSaldo = 1 AND activo = 1 ORDER BY fecha DESC, horaFin DESC LIMIT 1")
//      suspend fun getUltimoMercadilloConSaldoFinal(userId: String): MercadilloEntity?
//
//    @Query("""
//        SELECT * FROM mercadillos
//        WHERE userId = :userId
//          AND fecha >= :fechaActual
//          AND activo = 1
//        ORDER BY fecha ASC, horaInicio ASC
//    """)
//    suspend fun getMercadillosDesdeHoy(userId: String, fechaActual: String): List<MercadilloEntity>
//
//    // ========== VENTAS (desde recibos) ==========
//    @Query("""
//        SELECT COUNT(*)
//        FROM recibos
//        WHERE idMercadillo = :mercadilloId AND estado = 'COMPLETADO'
//    """)
//    suspend fun contarRecibosPorMercadillo(mercadilloId: String): Int
//
//    @Query("""
//        SELECT COALESCE(SUM(totalTicket), 0)
//        FROM recibos
//        WHERE idMercadillo = :mercadilloId AND estado = 'COMPLETADO'
//    """)
//    suspend fun calcularTotalVentasDesdeRecibos(mercadilloId: String): Double
//
//    @Query("""
//        UPDATE mercadillos
//        SET totalVentas = :total,
//            version = version + 1,
//            lastModified = :now,
//            sincronizadoFirebase = 0
//        WHERE idMercadillo = :mercadilloId
//    """)
//    suspend fun actualizarTotalVentas(mercadilloId: String, total: Double, now: Long)
//
//    @Query("""
//        SELECT COALESCE(SUM(totalTicket), 0)
//        FROM recibos
//        WHERE idMercadillo = :mercadilloId
//          AND estado = 'COMPLETADO'
//          AND metodoPago = 'EFECTIVO'
//    """)
//    suspend fun getTotalVentasPorMetodoEF(mercadilloId: String): Double
//
//    @Query("""
//        SELECT COALESCE(SUM(totalTicket), 0)
//        FROM recibos
//        WHERE idMercadillo = :mercadilloId
//          AND estado = 'COMPLETADO'
//          AND metodoPago = 'BIZUM'
//    """)
//    suspend fun getTotalVentasPorMetodoBIZ(mercadilloId: String): Double
//
//    @Query("""
//        SELECT COALESCE(SUM(totalTicket), 0)
//        FROM recibos
//        WHERE idMercadillo = :mercadilloId
//          AND estado = 'COMPLETADO'
//          AND metodoPago = 'TARJETA'
//    """)
//    suspend fun getTotalVentasPorMetodoTAR(mercadilloId: String): Double
//
//}
//
///** Estadística simple por estado */
//data class EstadisticaEstado(
//    val estado: Int,
//    val cantidad: Int
//)
//
//
