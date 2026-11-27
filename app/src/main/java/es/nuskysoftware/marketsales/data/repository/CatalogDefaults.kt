package es.nuskysoftware.marketsales.data.repository
import androidx.room.withTransaction
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
object CatalogDefaults {
    fun id(uid: String) = "uncategorized:$uid"
    fun isProtected(category: CategoriaEntity) = category.idCategoria == id(category.userId)
    suspend fun ensure(db: AppDatabase, uid: String): String = db.withTransaction {
        val id = id(uid)
        if (db.categoriaDao().getCategoriaById(id, uid) == null)
            db.categoriaDao().insertCategoria(CategoriaEntity(idCategoria = id, userId = uid, nombre = "Sin categorizar", colorHex = "#808080", orden = Int.MIN_VALUE, lastModified = 0))
        id
    }
}
