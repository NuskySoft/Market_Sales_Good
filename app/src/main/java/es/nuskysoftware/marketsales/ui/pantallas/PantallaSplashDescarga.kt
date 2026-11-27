package es.nuskysoftware.marketsales.ui.pantallas

// Responsabilidad: Pantalla intermedia de descarga y recuperación de datos tras el acceso.
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.work.BusinessSyncScheduler
@Composable
// Pantalla intermedia de descarga y recuperación de datos tras el acceso.
fun PantallaSplashDescarga(navController: NavController, destinoDespues: String) {
    val context = LocalContext.current
    // Ejecuta este efecto al entrar en composición o cuando cambian sus claves (Unit).
    LaunchedEffect(Unit) {
        BusinessSyncScheduler.request(context)
        navController.navigate(destinoDespues) { popUpTo("splash_descarga") { inclusive = true }; launchSingleTop = true }
    }
}
