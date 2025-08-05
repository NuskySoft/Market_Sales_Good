// app/src/main/java/es/nuskysoftware/marketsales/ui/theme/Theme.kt
package es.nuskysoftware.marketsales.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

// Esquema de colores claro con verde pastel suave
//private val LightColorScheme = lightColorScheme(
//    primary = VerdePrimario,
//    onPrimary = BlancoTexto,
//    secondary = VerdeSecundario,
//    onSecondary = BlancoTexto,
//    tertiary = VerdeTerciario,
//    background = FondoClaro,
//    surface = BlancoTexto,
//    onBackground = NegroTexto,
//    onSurface = NegroTexto
//)
//
//// Esquema de colores oscuro con verde pastel suave adaptado
//private val DarkColorScheme = darkColorScheme(
//    primary = VerdePrimario,
//    onPrimary = BlancoTexto,
//    secondary = VerdeSecundario,
//    onSecondary = BlancoTexto,
//    tertiary = VerdeTerciario,
//    background = NegroTexto,
//    surface = GrisOscuro,
//    onBackground = BlancoTexto,
//    onSurface = BlancoTexto
//)

@Composable
fun MarketSalesTheme(
    configurationManager: es.nuskysoftware.marketsales.utils.ConfigurationManager,
    content: @Composable () -> Unit
) {
    // Observar cambios usando los StateFlow que SÍ existen
    val isDarkTheme by configurationManager.temaOscuro.collectAsState()
    val currentFont by configurationManager.fuente.collectAsState()
    val scheme = if (isDarkTheme) darkPlumScheme else lightPlumScheme

    // Seleccionar esquema de colores
    val colorScheme = if (isDarkTheme) {
        //DarkColorScheme
        darkPlumScheme
    } else {
        //LightColorScheme
        lightPlumScheme
    }

    // Obtener tipografía basada en la fuente configurada
    val typography = getTypographyForFont(currentFont)

    // Proporcionar el ConfigurationManager a todo el árbol de composición
    CompositionLocalProvider(
        LocalConfigurationManager provides configurationManager
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * Función para obtener la tipografía según la fuente seleccionada
 */
@Composable
private fun getTypographyForFont(fontName: String) = when (fontName) {
    "Montserrat" -> MontserratTypography
    "Poppins" -> PoppinsTypography
    "Roboto" -> RobotoTypography
    else -> MontserratTypography // Por defecto
}