package es.nuskysoftware.marketsales.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MercadilloRecuentoTest {
    @Test
    fun cuentaMercadillosDelPropietarioSinDependerDeCategorias() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            repeat(3) { n ->
                db.categoriaDao().insertCategoria(CategoriaEntity(idCategoria = "cat-$n", userId = "alice", nombre = "Cat $n"))
            }
            val dao = db.mercadilloDao()
            assertEquals(0, dao.contarPorUsuario("alice"))
            val first = MercadilloEntity(idMercadillo = "alice-1", userId = "alice")
            dao.insertMercadillo(first)
            dao.insertMercadillo(first.copy(idMercadillo = "alice-2", estado = 7))
            repeat(4) { n ->
                dao.insertMercadillo(MercadilloEntity(idMercadillo = "bob-$n", userId = "bob"))
            }
            assertEquals(2, dao.contarPorUsuario("alice"))
            assertEquals(4, dao.contarPorUsuario("bob"))
            assertEquals(0, dao.contarPorUsuario("empty"))
            db.categoriaDao().borrarPorUsuario("alice")
            assertEquals(2, dao.contarPorUsuario("alice"))
            dao.deleteMercadillo(first)
            assertEquals(1, dao.contarPorUsuario("alice"))
        } finally {
            db.close()
        }
    }
}
