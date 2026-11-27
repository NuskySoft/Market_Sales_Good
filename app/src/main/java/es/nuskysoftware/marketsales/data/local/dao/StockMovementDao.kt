package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Persistencia de obligaciones y movimientos de existencias.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.nuskysoftware.marketsales.data.local.entity.StockMovementEntity

@Dao
interface StockMovementDao {
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM stock_movements WHERE idUsuario = :uid")
    suspend fun allForOwner(uid: String): List<StockMovementEntity>
    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE stock_movements SET sincronizadoFirebase = 1 WHERE idMovimiento = :id AND idUsuario = :uid")
    suspend fun markUploaded(id: String, uid: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(movements: List<StockMovementEntity>)

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM stock_movements WHERE aplicado = 0 ORDER BY creadoEn, idMovimiento")
    suspend fun pending(): List<StockMovementEntity>

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM stock_movements WHERE idMovimiento = :id")
    suspend fun byId(id: String): StockMovementEntity?

    // Consulta local de lectura; devuelve el recuento. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM stock_movements WHERE aplicado = 0")
    suspend fun pendingCount(): Int

    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE stock_movements SET aplicado = 1 WHERE idMovimiento = :id AND aplicado = 0")
    suspend fun markApplied(id: String)

    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE articulos SET stock = COALESCE(stock, 0) - :quantity, sincronizadoFirebase = 0 WHERE idArticulo = :articleId AND userId = :owner")
    suspend fun applyStock(articleId: String, owner: String, quantity: Int): Int
}
