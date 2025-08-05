// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaSplashDescarga.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.FullBootstrapper
import kotlinx.coroutines.launch

@Composable
fun PantallaSplashDescarga(
    navController: NavController,
    destinoDespues: String // ruta a la que ir tras el bootstrap (ej. "mercadillos")
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var mensaje by remember { mutableStateOf("Preparando…") }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            mensaje = "Comprobando datos locales…"
            val userId = ConfigurationManager.getCurrentUserId() // null => default
            mensaje = "Descargando datos de Firebase…"
            FullBootstrapper.runIfNeeded(context, userId)
            mensaje = "Listo."
            cargando = false
            navController.navigate(destinoDespues) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            cargando = false
            error = e.message ?: "Error inesperado"
        }
    }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (cargando) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(mensaje)
            }
        } else if (error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No se pudieron cargar los datos.", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text(error!!)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    cargando = true; error = null
                    scope.launch {
                        try {
                            val userId = ConfigurationManager.getCurrentUserId()
                            FullBootstrapper.runIfNeeded(context, userId)
                            cargando = false
                            navController.navigate(destinoDespues) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            cargando = false
                            error = e.message ?: "Error inesperado"
                        }
                    }
                }) { Text("Reintentar") }
            }
        }
    }
}



//package es.nuskysoftware.marketsales.ui.pantallas
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//
//@Composable
//fun PantallaSplashDescarga(
//    navController: NavController
//) {
//    val context = LocalContext.current
//    val repo = remember { MercadilloRepository(context) }
//
//    // ✅ Descarga/merge si procede + autoestados + próximos mercadillos; después abrir "mercadillos"
//    LaunchedEffect(Unit) {
//        val userId = ConfigurationManager.getCurrentUserId()
//        try {
//            // Si no hay usuario válido (caso extraño), vamos directo
//            if (userId.isNullOrBlank() || userId == "usuario_default") {
//                navController.navigate("mercadillos") {
//                    popUpTo("splash_descarga") { inclusive = true }
//                    launchSingleTop = true
//                }
//                return@LaunchedEffect
//            }
//
//            // 1) Sincronizar (traer de Firebase si Room estaba vacío o había difs)
//            try { repo.sincronizarSinEstadosAutomaticos() } catch (_: Exception) {}
//
//            // 2) Actualizar estados automáticos
//            try { repo.actualizarEstadosAutomaticos(userId) } catch (_: Exception) {}
//
//            // 3) ✅ Llamada requerida: precalcular/precargar próximos mercadillos ANTES de abrir la lista
//            try { repo.getMercadillosDesdeHoy(userId) } catch (_: Exception) {}
//
//        } finally {
//            navController.navigate("mercadillos") {
//                popUpTo("splash_descarga") { inclusive = true }
//                launchSingleTop = true
//            }
//        }
//    }
//
//    Surface(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(
//                brush = Brush.verticalGradient(
//                    colors = listOf(
//                        MaterialTheme.colorScheme.primary,
//                        MaterialTheme.colorScheme.primaryContainer
//                    )
//                )
//            )
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(24.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                Text(
//                    text = "Preparando tus datos…",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onPrimary
//                )
//                CircularProgressIndicator(
//                    modifier = Modifier.size(32.dp),
//                    color = MaterialTheme.colorScheme.onPrimary
//                )
//                Text(
//                    text = "Sincronizando y cargando próximos mercadillos",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
//                )
//            }
//        }
//    }
//}
