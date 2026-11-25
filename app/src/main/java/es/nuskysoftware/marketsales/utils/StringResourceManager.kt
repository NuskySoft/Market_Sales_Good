package es.nuskysoftware.marketsales.utils

import java.util.Locale

/**
 * Gestor de recursos de string que permite cambio dinámico de idioma
 * V11 - Con strings completos para PantallaPerfil + PantallaCategorias
 */


object StringResourceManager {
    fun getString(key: String, language: String = "es"): String {
        return when (language) {
            "en" -> EnglishStrings.get(key)
            "es" -> SpanishStrings.get(key)
            else -> SpanishStrings.get(key)
        }
    }
}

