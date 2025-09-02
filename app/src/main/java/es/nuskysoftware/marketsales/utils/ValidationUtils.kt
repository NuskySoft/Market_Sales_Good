package es.nuskysoftware.marketsales.utils

import java.util.Locale

object ValidationUtils {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")
    private val WHATSAPP_REGEX = Regex("^\\+[0-9]{6,15}")

    fun esEmailValido(email: String): Boolean = EMAIL_REGEX.matches(email)
    fun esNumeroWhatsappValido(numero: String): Boolean = WHATSAPP_REGEX.matches(numero)

    fun prefijoPorDefecto(): String {
        val country = Locale.getDefault().country.uppercase(Locale.ROOT)
        val map = mapOf(
            "ES" to "+34",
            "US" to "+1",
            "MX" to "+52",
            "AR" to "+54",
            "CO" to "+57"
        )
        return map[country] ?: "+"
    }
}