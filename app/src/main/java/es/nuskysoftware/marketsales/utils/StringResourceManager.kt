package es.nuskysoftware.marketsales.utils

// Responsabilidad: Resuelve las claves de texto según el idioma seleccionado.

import java.util.Locale

/**
 * Gestor de recursos de string que permite cambio dinámico de idioma
 * V11 - Con strings completos para PantallaPerfil + PantallaCategorias
 */


object StringResourceManager {
    // Obtiene la traducción asociada a la clave y al idioma solicitado.
    fun getString(key: String, language: String = "es"): String {
        return when (language) {
            "en" -> EnglishStrings.get(key)
            "es" -> SpanishStrings.get(key)
            else -> SpanishStrings.get(key)
        }
    }
}

