package es.nuskysoftware.marketsales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import es.nuskysoftware.marketsales.data.local.entity.isPremium
import es.nuskysoftware.marketsales.ui.pantallas.PantallaConfiguracion
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModel
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun MenuLateral(
    configuracionViewModel: ConfiguracionViewModel,
    authViewModel: AuthViewModel,
    currentUser: FirebaseUser?,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val configuracion by configuracionViewModel.configuracion.collectAsState()

    var mostrarConfiguracion by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }
    var mostrarDialogoLogout by remember { mutableStateOf(false) }

    // ✅ TODO: Implementar PantallaConfiguracion como Modal o Dialog
    // if (mostrarConfiguracion) {
    //     PantallaConfiguracion(
    //         configuracionViewModel = configuracionViewModel,
    //         onCloseConfiguration = { mostrarConfiguracion = false }
    //     )
    // }

    if (mostrarDialogoLogout) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLogout = false },
            title = {
                Text(StringResourceManager.getString("logout_title", "Cerrar Sesión"))
            },
            text = {
                Text(StringResourceManager.getString("logout_message", "¿Estás seguro de que deseas cerrar sesión?"))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoLogout = false
                        onLogout()
                    }
                ) {
                    Text(
                        StringResourceManager.getString("logout_confirm", "Cerrar Sesión"),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoLogout = false }
                ) {
                    Text(StringResourceManager.getString("cancel", "Cancelar"))
                }
            }
        )
    }

    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = {
                Text(StringResourceManager.getString("exit_title", "Salir de la App"))
            },
            text = {
                Text(StringResourceManager.getString("exit_message", "¿Estás seguro de que deseas salir de Market Sales?"))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoSalir = false
                        (context as? androidx.activity.ComponentActivity)?.finishAffinity()
                    }
                ) {
                    Text(
                        StringResourceManager.getString("exit_confirm", "Salir"),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoSalir = false }
                ) {
                    Text(StringResourceManager.getString("cancel", "Cancelar"))
                }
            }
        )
    }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header del menú con información del usuario
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(bottomEnd = 16.dp, bottomStart = 16.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Market Sales",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentUser != null) {
                        Text(
                            text = currentUser.email ?: "Usuario",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (configuracion?.isPremium == true) {
                                Text(
                                    text = "🚀",
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = StringResourceManager.getString("premium", "Premium"),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            } else {
                                Text(
                                    text = StringResourceManager.getString("free", "Gratuito"),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = StringResourceManager.getString("not_authenticated", "No autenticado"),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opciones del menú
            MenuLateralItem(
                icon = Icons.Default.Home,
                texto = StringResourceManager.getString("home", "Inicio"),
                onClick = {
                    onCloseDrawer()
                }
            )

            MenuLateralItem(
                icon = Icons.Default.ShoppingCart,
                texto = StringResourceManager.getString("markets", "Mercadillos"),
                onClick = {
                    onCloseDrawer()
                }
            )

            if (configuracion?.isPremium == true) {
                MenuLateralItem(
                    icon = Icons.Default.Add,
                    texto = StringResourceManager.getString("add_market", "Añadir Mercadillo"),
                    onClick = {
                        onCloseDrawer()
                        // TODO: Navegar a añadir mercadillo
                    }
                )

                MenuLateralItem(
                    icon = Icons.Default.Star,
                    texto = StringResourceManager.getString("premium_features", "Funciones Premium"),
                    onClick = {
                        onCloseDrawer()
                        // TODO: Mostrar funciones premium
                    }
                )
            } else {
                MenuLateralItem(
                    icon = Icons.Default.Star,
                    texto = "🚀 ${StringResourceManager.getString("upgrade_premium", "Actualizar a Premium")}",
                    onClick = {
                        onCloseDrawer()
                        // TODO: Mostrar pantalla de upgrade
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MenuLateralItem(
                icon = Icons.Default.Settings,
                texto = StringResourceManager.getString("configuration", "Configuración"),
                onClick = {
                    // ✅ TODO: Implementar navegación a configuración
                    onCloseDrawer()
                }
            )

            MenuLateralItem(
                icon = Icons.Default.Info,
                texto = StringResourceManager.getString("about", "Acerca de"),
                onClick = {
                    onCloseDrawer()
                    // TODO: Mostrar información de la app
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Sección de logout y salir en la parte inferior
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (currentUser != null) {
                MenuLateralItem(
                    icon = Icons.Default.ExitToApp,
                    texto = StringResourceManager.getString("logout", "Cerrar Sesión"),
                    onClick = {
                        mostrarDialogoLogout = true
                    },
                    isDestructive = true
                )
            }

            MenuLateralItem(
                icon = Icons.Default.Close,
                texto = StringResourceManager.getString("exit_app", "Salir de la App"),
                onClick = {
                    mostrarDialogoSalir = true
                },
                isDestructive = true
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MenuLateralItem(
    icon: ImageVector,
    texto: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = texto,
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        label = {
            Text(
                text = texto,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}