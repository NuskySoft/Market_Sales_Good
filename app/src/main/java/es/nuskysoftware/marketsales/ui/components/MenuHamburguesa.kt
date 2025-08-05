

package es.nuskysoftware.marketsales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.launch

@Composable
fun MenuHamburguesa(
    navController: NavController,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ✅ OPCIÓN B: AuthViewModel para detectar estado de usuario
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val currentUser by authViewModel.currentUser.collectAsState()

    // Obtener valores del ConfigurationManager usando StateFlows
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val versionApp by ConfigurationManager.versionApp.collectAsState()

    // Calcular valores derivados
    val versionText = if (versionApp == 1) "Premium V1.0" else "Free V1.0"

    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        // Header del menú
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = StringResourceManager.getString("app_name", "Market Sales"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Text(
                    text = versionText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )

                // ✅ MOSTRAR EMAIL SI ESTÁ AUTENTICADO
                if (currentUser != null) {
                    Text(
                        text = "👤 ${currentUser?.email ?: "Usuario"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Opciones del menú
        LazyColumn {
            item {
                MenuOption(
                    iconRes = R.drawable.ic_store,
                    title = StringResourceManager.getString("mercadillos", currentLanguage),
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("mercadillos") {
                            popUpTo("mercadillos") { inclusive = true }
                        }
                    }
                )
            }

            item {
                MenuOption(
                    iconRes = R.drawable.ic_list,
                    title = StringResourceManager.getString("articulos", currentLanguage),
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("articulos")
                    }
                )
            }

            item {
                MenuOption(
                    iconRes = R.drawable.ic_category,
                    title = StringResourceManager.getString("categorias", currentLanguage),
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("categorias")
                    }
                )
            }

            item {
                MenuOption(
                    iconRes = R.drawable.ic_inventory,
                    title = StringResourceManager.getString("inventario", currentLanguage),
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("inventario")
                    }
                )
            }

            item {
                MenuOption(
                    iconRes = R.drawable.ic_list,
                    title = StringResourceManager.getString("listados", currentLanguage),
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("listados")
                    }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }

            item {
                MenuOption(
                    iconRes = R.drawable.ic_settings,
                    title = StringResourceManager.getString("configuracion", currentLanguage),
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("configuracion")
                    }
                )
            }

            // ✅ OPCIÓN B: LÓGICA CONDICIONAL LOGIN/LOGOUT
            item {
                if (currentUser != null) {
                    // Usuario autenticado - mostrar LOGOUT
                    MenuOption(
                        iconRes = R.drawable.ic_logout,
                        title = StringResourceManager.getString("cerrar_sesion", currentLanguage),
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                authViewModel.logout()
                            }
                        }
                    )
                } else {
                    // Usuario NO autenticado - mostrar LOGIN
                    MenuOption(
                        iconRes = R.drawable.ic_login,
                        title = StringResourceManager.getString("iniciar_sesion", currentLanguage),
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("login")
                        }
                    )
                }
            }

            item {
                MenuOption(
                    iconRes = R.drawable.ic_exit,
                    title = StringResourceManager.getString("salir", currentLanguage),
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Salir de la aplicación
                        (context as? androidx.activity.ComponentActivity)?.finishAffinity()
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuOption(
    iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
