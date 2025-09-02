// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaSplashDescarga.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.FullBootstrapper
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.launch
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import es.nuskysoftware.marketsales.data.repository.UserRepository
@Composable
fun PantallaSplashDescarga(
    navController: NavController,
    destinoDespues: String // ruta a la que ir tras el bootstrap (ej. "mercadillos")
) {
    //val context =   androidx.compose.ui.platform.LocalContext.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    var mensaje by remember { mutableStateOf(StringResourceManager.getString("preparando_datos", currentLanguage)) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarMensajeFree by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val userId = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
            val repo = MercadilloRepository(context)
            val roomVacio = repo.isRoomEmptyForUser(userId)
            val hayRemotos = try {
                FirebaseFirestore.getInstance().collection("mercadillos")
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get().await().documents.isNotEmpty()
            } catch (_: Exception) { false }

            if (!roomVacio || !hayRemotos) {
                navController.navigate(destinoDespues) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
                return@LaunchedEffect
            }

            val esPremium = ConfigurationManager.getIsPremium()
            if (esPremium) {
                mensaje = "Descargando datos de Firebase…"
                FullBootstrapper.runIfNeeded(context, userId)
                mensaje = "Listo."
                cargando = false
                navController.navigate(destinoDespues) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                cargando = false
                mostrarMensajeFree = true
//            val userId = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
//            val repo = MercadilloRepository(context)
//            val roomVacio = repo.isRoomEmptyForUser(userId)
//            val hayRemotos = try {
//                FirebaseFirestore.getInstance().collection("mercadillos")
//                    .whereEqualTo("userId", userId)
//                    .limit(1)
//                    .get().await().documents.isNotEmpty()
//            } catch (_: Exception) { false }
//
//            if (!roomVacio || !hayRemotos) {
//                navController.navigate(destinoDespues) {
//                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
//                    launchSingleTop = true
//                }
//                return@LaunchedEffect
//            }
//
//            val esPremium = ConfigurationManager.getIsPremium()
//            if (esPremium) {
//                mensaje = "Descargando datos de Firebase…"
//                FullBootstrapper.runIfNeeded(context, userId)
//                mensaje = "Listo."
//                cargando = false
//                navController.navigate(destinoDespues) {
//                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
//                    launchSingleTop = true
//                }
//            } else {
//                cargando = false
//                mostrarMensajeFree = true
            }
        } catch (e: Exception) {
            cargando = false
            error = e.message ?: StringResourceManager.getString("error_inesperado", currentLanguage)
        }
    }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (cargando) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(mensaje)
            }
        } else if (mostrarMensajeFree) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Existen datos en la nube para este usuario. Para poder descargarlos necesitas ser usuario Premium. \n\n¿Deseas hacerte Premium? Si eliges que no, los datos de la nube se borrarán.",
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = {
                        navController.navigate("suscripcion")
                    }) { Text("Sí, hacerme Premium") }
                    Button(onClick = {
                        scope.launch {
                            mostrarMensajeFree = false
                            cargando = true
                            mensaje = "Borrando datos de la nube…"
                            try {
                                val uid = ConfigurationManager.getCurrentUserId()
                                if (!uid.isNullOrBlank()) {
                                    val userRepo = UserRepository(context)
                                    //userRepo.borrarDatosUsuario(uid)
                                }
                            } catch (_: Exception) { }
                            cargando = false
                            navController.navigate(destinoDespues) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }) { Text("No, borrar datos") }
                }
            }
        } else if (error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    StringResourceManager.getString("no_se_pudieron_cargar_datos", currentLanguage),
                    color = MaterialTheme.colorScheme.error
                )
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
                            error = e.message ?: StringResourceManager.getString("error_inesperado", currentLanguage)
                        }
                    }
                }) { Text(StringResourceManager.getString("reintentar", currentLanguage)) }
            }
        }
    }
}
