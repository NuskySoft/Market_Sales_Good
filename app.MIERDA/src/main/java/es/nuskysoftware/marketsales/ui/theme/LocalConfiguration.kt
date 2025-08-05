// app/src/main/java/es/nuskysoftware/marketsales/ui/theme/LocalConfiguration.kt
package es.nuskysoftware.marketsales.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import es.nuskysoftware.marketsales.utils.ConfigurationManager

/**
 * CompositionLocal que proporciona acceso al ConfigurationManager
 * en cualquier parte del árbol de composición
 */
val LocalConfigurationManager = compositionLocalOf<ConfigurationManager> {
    error("ConfigurationManager no proporcionado")
}

/**
 * Función para acceder al ConfigurationManager actual
 */
@Composable
fun getConfigurationManager(): ConfigurationManager {
    return LocalConfigurationManager.current
}