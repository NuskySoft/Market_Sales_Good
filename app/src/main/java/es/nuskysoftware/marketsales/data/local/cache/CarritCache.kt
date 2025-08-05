package es.nuskysoftware.marketsales.data.local.cache

import es.nuskysoftware.marketsales.data.repository.LineaVentaUI
import es.nuskysoftware.marketsales.data.repository.PestanaVenta
import java.util.concurrent.ConcurrentHashMap

data class DraftCarrito(
    val lineas: List<LineaVentaUI>,
    val total: Double,
    val importeActual: String,
    val descripcionActual: String,
    val pestana: PestanaVenta
)

object CarritoCache {
    private val cache = ConcurrentHashMap<String, DraftCarrito>() // key = mercadilloId

    fun save(mercadilloId: String, draft: DraftCarrito) {
        if (mercadilloId.isBlank()) return
        cache[mercadilloId] = draft
    }

    fun load(mercadilloId: String): DraftCarrito? = cache[mercadilloId]

    fun clear(mercadilloId: String) {
        cache.remove(mercadilloId)
    }
}
