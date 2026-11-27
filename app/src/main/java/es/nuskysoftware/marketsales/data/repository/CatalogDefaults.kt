package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Garantiza la categoría protegida Sin categorizar, identificada de forma estable por propietario.
import androidx.room.withTransaction
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
object CatalogDefaults {
    // Deriva la clave reservada de cada cuenta.
    fun id(uid: String) = "uncategorized:$uid"
    // Comprueba si la categoría corresponde a la clave reservada del propietario.
    fun isProtected(category: CategoriaEntity) = category.idCategoria == id(category.userId)
    // Garantiza en una transacción que la categoría automática exista y devuelve su identificador.
    suspend fun ensure(db: AppDatabase, uid: String): String = db.withTransaction {
        val id = id(uid)
        if (db.categoriaDao().getCategoriaById(id, uid) == null)
            db.categoriaDao().insertCategoria(CategoriaEntity(idCategoria = id, userId = uid, nombre = "Sin categorizar", colorHex = "#808080", orden = Int.MIN_VALUE, lastModified = 0))
        id
    }
}
