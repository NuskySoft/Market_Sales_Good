package es.nuskysoftware.marketsales.utils

// Responsabilidad: Agrupa criterios de filtrado de mercadillos.

import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity

object MercadillosFilters {
    fun soloDelUsuario(lista: List<MercadilloEntity>, userId: String?): List<MercadilloEntity> {
        val uid = userId.orEmpty()
        if (uid.isBlank()) return emptyList()
        return lista.filter { it.userId == uid }
    }
}
