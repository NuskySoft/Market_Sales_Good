// app/src/main/java/es/nuskysoftware/marketsales/ui/theme/Color.kt
package es.nuskysoftware.marketsales.ui.theme

// Responsabilidad: Define los colores utilizados por los temas y componentes de Compose.

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ===== Ciruela pastel – Light =====
val PlumPrimary            = Color(0xFF8E4D84) // ciruela
val PlumOnPrimary          = Color(0xFFFFFFFF)
val PlumPrimaryContainer   = Color(0xFFF4D9F0)
val PlumOnPrimaryContainer = Color(0xFF34162F)

val PlumSecondary            = Color(0xFF6F5A70) // mauve grisáceo
val PlumOnSecondary          = Color(0xFFFFFFFF)
val PlumSecondaryContainer   = Color(0xFFF2DDF7)
val PlumOnSecondaryContainer = Color(0xFF2A1D2D)

val PlumTertiary            = Color(0xFF7B4F7A) // orquídea suave
val PlumOnTertiary          = Color(0xFFFFFFFF)
val PlumTertiaryContainer   = Color(0xFFF5D8F1)
val PlumOnTertiaryContainer = Color(0xFF311031)

val PlumBackground   = Color(0xFFFFFBFF)
val PlumOnBackground = Color(0xFF1D1A1D)
val PlumSurface      = Color(0xFFFFFBFF)
val PlumOnSurface    = Color(0xFF1D1A1D)

val PlumSurfaceVariant   = Color(0xFFEADFE8)
val PlumOnSurfaceVariant = Color(0xFF4B3F48)
val PlumOutline          = Color(0xFF7D717A)
val PlumOutlineVariant   = Color(0xFFD7CBD5)

val PlumError            = Color(0xFFBA1A1A)
val PlumOnError          = Color(0xFFFFFFFF)
val PlumErrorContainer   = Color(0xFFFFDAD6)
val PlumOnErrorContainer = Color(0xFF410002)

// ===== Ciruela pastel – Dark =====
val PlumPrimaryDark            = Color(0xFFE8B6DE)
val PlumOnPrimaryDark          = Color(0xFF3C1237)
val PlumPrimaryContainerDark   = Color(0xFF5D2E56)
val PlumOnPrimaryContainerDark = Color(0xFFFFD7F2)

val PlumSecondaryDark            = Color(0xFFD0BFD6)
val PlumOnSecondaryDark          = Color(0xFF362B39)
val PlumSecondaryContainerDark   = Color(0xFF4D3E50)
val PlumOnSecondaryContainerDark = Color(0xFFF5E1FA)

val PlumTertiaryDark            = Color(0xFFD9B6D8)
val PlumOnTertiaryDark          = Color(0xFF351933)
val PlumTertiaryContainerDark   = Color(0xFF55384F)
val PlumOnTertiaryContainerDark = Color(0xFFFFD7F3)

val PlumBackgroundDark   = Color(0xFF141216)
val PlumOnBackgroundDark = Color(0xFFEAE0EA)
val PlumSurfaceDark      = Color(0xFF141216)
val PlumOnSurfaceDark    = Color(0xFFEAE0EA)

val PlumSurfaceVariantDark   = Color(0xFF4B3F48)
val PlumOnSurfaceVariantDark = Color(0xFFD7CBD5)
val PlumOutlineDark          = Color(0xFF968A93)
val PlumOutlineVariantDark   = Color(0xFF4B3F48)

val lightPlumScheme = lightColorScheme(
    primary = PlumPrimary,
    onPrimary = PlumOnPrimary,
    primaryContainer = PlumPrimaryContainer,
    onPrimaryContainer = PlumOnPrimaryContainer,
    secondary = PlumSecondary,
    onSecondary = PlumOnSecondary,
    secondaryContainer = PlumSecondaryContainer,
    onSecondaryContainer = PlumOnSecondaryContainer,
    tertiary = PlumTertiary,
    onTertiary = PlumOnTertiary,
    tertiaryContainer = PlumTertiaryContainer,
    onTertiaryContainer = PlumOnTertiaryContainer,
    error = PlumError,
    onError = PlumOnError,
    errorContainer = PlumErrorContainer,
    onErrorContainer = PlumOnErrorContainer,
    background = PlumBackground,
    onBackground = PlumOnBackground,
    surface = PlumSurface,
    onSurface = PlumOnSurface,
    surfaceVariant = PlumSurfaceVariant,
    onSurfaceVariant = PlumOnSurfaceVariant,
    outline = PlumOutline,
    outlineVariant = PlumOutlineVariant,
)

val darkPlumScheme = darkColorScheme(
    primary = PlumPrimaryDark,
    onPrimary = PlumOnPrimaryDark,
    primaryContainer = PlumPrimaryContainerDark,
    onPrimaryContainer = PlumOnPrimaryContainerDark,
    secondary = PlumSecondaryDark,
    onSecondary = PlumOnSecondaryDark,
    secondaryContainer = PlumSecondaryContainerDark,
    onSecondaryContainer = PlumOnSecondaryContainerDark,
    tertiary = PlumTertiaryDark,
    onTertiary = PlumOnTertiaryDark,
    tertiaryContainer = PlumTertiaryContainerDark,
    onTertiaryContainer = PlumOnTertiaryContainerDark,
    error = PlumError,
    onError = PlumOnError,
    errorContainer = PlumErrorContainer,
    onErrorContainer = PlumOnErrorContainer,
    background = PlumBackgroundDark,
    onBackground = PlumOnBackgroundDark,
    surface = PlumSurfaceDark,
    onSurface = PlumOnSurfaceDark,
    surfaceVariant = PlumSurfaceVariantDark,
    onSurfaceVariant = PlumOnSurfaceVariantDark,
    outline = PlumOutlineDark,
    outlineVariant = PlumOutlineVariantDark,
)

