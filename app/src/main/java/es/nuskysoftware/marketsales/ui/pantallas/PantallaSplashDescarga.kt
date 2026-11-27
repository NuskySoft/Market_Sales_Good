package es.nuskysoftware.marketsales.ui.pantallas
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.work.BusinessSyncScheduler
@Composable
fun PantallaSplashDescarga(navController: NavController, destinoDespues: String) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        BusinessSyncScheduler.request(context)
        navController.navigate(destinoDespues) { popUpTo("splash_descarga") { inclusive = true }; launchSingleTop = true }
    }
}
