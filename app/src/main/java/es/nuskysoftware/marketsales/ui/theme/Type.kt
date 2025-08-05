package es.nuskysoftware.marketsales.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.R


/**
 * Type.kt - Market Sales
 * ✅ Múltiples familias tipográficas dinámicas
 */

// ------------------- Definimos las familias tipográficas -------------------
val Montserrat = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

val Roboto = FontFamily.Default // Roboto viene por defecto en Android

// ------------------- Función para crear Typography con fuente específica -------------------
private fun createTypography(fontFamily: FontFamily): Typography {
    return Typography(
        // Título grande (pantallas principales)
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        ),
        // Título medio
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp
        ),
        // Texto normal
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        ),
        // Texto pequeño
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        )
    )
}

// ------------------- Tipografías precreadas -------------------
val MontserratTypography = createTypography(Montserrat)
val PoppinsTypography = createTypography(Poppins)
val RobotoTypography = createTypography(Roboto)