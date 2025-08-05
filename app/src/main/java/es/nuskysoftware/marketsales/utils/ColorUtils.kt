// app/src/main/java/es/nuskysoftware/marketsales/utils/ColorUtils.kt
package es.nuskysoftware.marketsales.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.random.Random

/**
 * ColorUtils V11 - Market Sales
 *
 * Utilidades para manejo de colores en la aplicación.
 * Migrado desde Caja Mercadillos y adaptado para Market Sales.
 */

/**
 * Genera un color aleatorio en tonos pastel
 * Los colores pastel tienen valores RGB altos (150-255) para ser suaves y agradables
 */
fun generarColorAleatorioPastel(): Color {
    val r = Random.nextInt(150, 256)
    val g = Random.nextInt(150, 256)
    val b = Random.nextInt(150, 256)
    return Color(r, g, b)
}

/**
 * Convierte un Color de Compose a String HEX
 */
fun Color.toHexString(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

/**
 * Convierte un String HEX a Color de Compose
 * @param hexString String en formato "#RRGGBB"
 * @return Color o Color.White si el formato es inválido
 */
fun hexStringToColor(hexString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexString))
    } catch (e: Exception) {
        Color.White
    }
}

/**
 * Verifica si un string es un color HEX válido
 */
fun isValidHexColor(hexString: String): Boolean {
    return hexString.matches(Regex("^#[0-9A-Fa-f]{6}$"))
}

/**
 * Obtiene un color de contraste (blanco o negro) según el brillo del color de fondo
 */
fun Color.getContrastColor(): Color {
    // Calcular luminancia usando la fórmula estándar
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return if (luminance > 0.5) Color.Black else Color.White
}

/**
 * Colores predefinidos pastel para categorías
 */
object ColoresPastelPredefinidos {
    val ROSA_PASTEL = Color(0xFFFFB6C1)
    val AZUL_PASTEL = Color(0xFFADD8E6)
    val VERDE_PASTEL = Color(0xFF98FB98)
    val AMARILLO_PASTEL = Color(0xFFFFFACD)
    val LAVANDA_PASTEL = Color(0xFFE6E6FA)
    val MELOCOTON_PASTEL = Color(0xFFFFDAB9)
    val MENTA_PASTEL = Color(0xFFF0FFF0)
    val CORAL_PASTEL = Color(0xFFF08080)

    /**
     * Lista de todos los colores predefinidos
     */
    val TODOS = listOf(
        ROSA_PASTEL,
        AZUL_PASTEL,
        VERDE_PASTEL,
        AMARILLO_PASTEL,
        LAVANDA_PASTEL,
        MELOCOTON_PASTEL,
        MENTA_PASTEL,
        CORAL_PASTEL
    )

    /**
     * Obtiene un color aleatorio de los predefinidos
     */
    fun obtenerAleatorio(): Color {
        return TODOS.random()
    }
}
