package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.nuskysoftware.marketsales.data.local.entity.StockMovementEntity

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE idUsuario = :uid")
    suspend fun allForOwner(uid: String): List<StockMovementEntity>
    @Query("UPDATE stock_movements SET sincronizadoFirebase = 1 WHERE idMovimiento = :id AND idUsuario = :uid")
    suspend fun markUploaded(id: String, uid: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(movements: List<StockMovementEntity>)

    @Query("SELECT * FROM stock_movements WHERE aplicado = 0 ORDER BY creadoEn, idMovimiento")
    suspend fun pending(): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements WHERE idMovimiento = :id")
    suspend fun byId(id: String): StockMovementEntity?

    @Query("SELECT COUNT(*) FROM stock_movements WHERE aplicado = 0")
    suspend fun pendingCount(): Int

    @Query("UPDATE stock_movements SET aplicado = 1 WHERE idMovimiento = :id AND aplicado = 0")
    suspend fun markApplied(id: String)

    @Query("UPDATE articulos SET stock = COALESCE(stock, 0) - :quantity, sincronizadoFirebase = 0 WHERE idArticulo = :articleId AND userId = :owner")
    suspend fun applyStock(articleId: String, owner: String, quantity: Int): Int
}
