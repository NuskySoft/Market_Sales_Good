package es.nuskysoftware.marketsales.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ================= COLORES PRINCIPALES - VERDE PASTEL =================

// ------------------- Tema Claro - Verde Pastel MÁS SUAVE -------------------
val PrimaryLight = Color(0xFF81C784)        // Verde pastel más suave
val OnPrimaryLight = Color(0xFFFFFFFF)      // Blanco sobre verde
val SecondaryLight = Color(0xFFA5D6A7)      // Verde pastel muy claro
val OnSecondaryLight = Color(0xFF2E7D32)    // Verde oscuro sobre verde pastel
val BackgroundLight = Color(0xFFF1F8E9)     // Verde muy claro de fondo
val OnBackgroundLight = Color(0xFF1B5E20)   // Verde muy oscuro para texto

// ------------------- Tema Oscuro - Verde Pastel MÁS SUAVE -------------------
val PrimaryDark = Color(0xFF66BB6A)         // Verde pastel medio para oscuro
val OnPrimaryDark = Color(0xFF1B5E20)       // Verde muy oscuro sobre verde pastel
val SecondaryDark = Color(0xFF81C784)       // Verde pastel más claro
val OnSecondaryDark = Color(0xFF2E7D32)     // Verde oscuro sobre verde pastel
val BackgroundDark = Color(0xFF1B5E20)      // Verde muy oscuro de fondo
val OnBackgroundDark = Color(0xFFC8E6C9)    // Verde claro para texto

// ================= COLORES EXTENDIDOS PERSONALIZADOS =================

// ------------------- Extendidos Claro -------------------
val SuccessLight = Color(0xFF4CAF50)        // Verde éxito
val OnSuccessLight = Color(0xFFFFFFFF)      // Blanco sobre verde
val WarningLight = Color(0xFFFF9800)        // Naranja advertencia
val OnWarningLight = Color(0xFFFFFFFF)      // Blanco sobre naranja
val ErrorLight = Color(0xFFF44336)          // Rojo error
val OnErrorLight = Color(0xFFFFFFFF)        // Blanco sobre rojo
val InfoLight = Color(0xFF4CAF50)           // Verde información (en vez de azul)
val OnInfoLight = Color(0xFFFFFFFF)         // Blanco sobre verde info

val SurfaceLight = Color(0xFFFFFFFF)        // Superficie blanca
val OnSurfaceLight = Color(0xFF2E7D32)      // Verde oscuro sobre superficie
val SurfaceVariantLight = Color(0xFFC8E6C9) // Superficie variante verde claro
val OnSurfaceVariantLight = Color(0xFF1B5E20) // Verde muy oscuro sobre superficie variante

// ------------------- Extendidos Oscuro -------------------
val SuccessDark = Color(0xFF388E3C)         // Verde éxito oscuro
val OnSuccessDark = Color(0xFFFFFFFF)       // Blanco sobre verde oscuro
val WarningDark = Color(0xFFF57C00)         // Naranja advertencia oscuro
val OnWarningDark = Color(0xFFFFFFFF)       // Blanco sobre naranja oscuro
val ErrorDark = Color(0xFFD32F2F)           // Rojo error oscuro
val OnErrorDark = Color(0xFFFFFFFF)         // Blanco sobre rojo oscuro
val InfoDark = Color(0xFF388E3C)            // Verde información oscuro
val OnInfoDark = Color(0xFFFFFFFF)          // Blanco sobre verde info oscuro

val SurfaceDark = Color(0xFF2E7D32)         // Superficie verde oscuro
val OnSurfaceDark = Color(0xFFA5D6A7)       // Verde claro sobre superficie oscura
val SurfaceVariantDark = Color(0xFF1B5E20)  // Superficie variante verde muy oscuro
val OnSurfaceVariantDark = Color(0xFF81C784) // Verde pastel sobre superficie variante

// ================= CLASE PARA COLORES EXTENDIDOS =================

data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val info: Color,
    val onInfo: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color
)

// ------------------- Instancias de colores extendidos -------------------
val LightExtendedColors = ExtendedColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    error = ErrorLight,
    onError = OnErrorLight,
    info = InfoLight,
    onInfo = OnInfoLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

val DarkExtendedColors = ExtendedColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    error = ErrorDark,
    onError = OnErrorDark,
    info = InfoDark,
    onInfo = OnInfoDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

// ================= COMPOSITION LOCAL PARA ACCESO GLOBAL =================

val LocalExtendedColors = staticCompositionLocalOf {
    LightExtendedColors
}

// ================= EXTENSIÓN PARA ACCESO FÁCIL =================

val ColorScheme.extended: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

// ================= COLORES ADICIONALES VERDE PASTEL =================

// Colores específicos de la aplicación
val PrimaryGreen = Color(0xFF4CAF50)
val SecondaryGreenLight = Color(0xFF81C784)
val AccentGreenPastel = Color(0xFFA5D6A7)
val WarningOrange = Color(0xFFFF9800)

// Verdes para fondos y bordes
val Green50 = Color(0xFFF1F8E9)
val Green100 = Color(0xFFC8E6C9)
val Green200 = Color(0xFFA5D6A7)
val Green300 = Color(0xFF81C784)
val Green400 = Color(0xFF66BB6A)
val Green500 = Color(0xFF4CAF50)
val Green600 = Color(0xFF43A047)
val Green700 = Color(0xFF388E3C)
val Green800 = Color(0xFF2E7D32)
val Green900 = Color(0xFF1B5E20)

// Colores verde pastel suave para Market Sales
val VerdePrimario = Color(0xFF90EE90)      // Verde claro pastel
val VerdeSecundario = Color(0xFF98FB98)     // Verde pálido
val VerdeTerciario = Color(0xFFAFEEAF)      // Verde muy claro

// Colores de fondo y superficie
val FondoClaro = Color(0xFFF5F5F5)          // Gris muy claro
val BlancoTexto = Color(0xFFFFFFFF)         // Blanco puro
val NegroTexto = Color(0xFF000000)          // Negro puro
val GrisOscuro = Color(0xFF424242)          // Gris oscuro para modo oscuro

// Colores adicionales
val GrisClaro = Color(0xFFE0E0E0)           // Gris claro para bordes
val AzulAccento = Color(0xFF2196F3)         // Azul para enlaces
val RojoError = Color(0xFFF44336)           // Rojo para errores
val VerdeExito = Color(0xFF4CAF50)          // Verde para éxito