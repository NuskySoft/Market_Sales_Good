package es.nuskysoftware.marketsales.ui.components

// Responsabilidad: Presenta las opciones de navegación lateral y sus acciones.

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
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
// Presenta las opciones de navegación y los eventos de cierre del menú.
fun MenuLateral(
    configuracionViewModel: ConfiguracionViewModel,
    authViewModel: AuthViewModel,
    currentUser: FirebaseUser?,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit
) {
    val currentLanguage = ConfigurationManager.idioma.collectAsState().value
    val context = LocalContext.current
    val configuracion by configuracionViewModel.configuracion.collectAsState()

    var mostrarConfiguracion by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }
    var mostrarDialogoLogout by remember { mutableStateOf(false) }

    // ✅ TODO: Implementar PantallaConfiguracion como Modal o Dialog

    if (mostrarDialogoLogout) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLogout = false },
            title = {
                Text(StringResourceManager.getString("cerrar_sesion", currentLanguage))
            },
            text = {
                Text(StringResourceManager.getString("logout_message", currentLanguage))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoLogout = false
                        onLogout()
                    }
                ) {
                    Text(
                        StringResourceManager.getString("cerrar_sesion", currentLanguage),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoLogout = false }
                ) {
                    Text(StringResourceManager.getString("cancelar", currentLanguage))
                }
            }
        )
    }

    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = {
                Text(StringResourceManager.getString("salir", currentLanguage))
            },
            text = {
                Text(StringResourceManager.getString("exit_message", currentLanguage))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoSalir = false
                        (context as? androidx.activity.ComponentActivity)?.finishAffinity()
                    }
                ) {
                    Text(
                        StringResourceManager.getString("exit_confirm", currentLanguage),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoSalir = false }
                ) {
                    Text(StringResourceManager.getString("cancelar", currentLanguage))
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
                                    text = StringResourceManager.getString("premium", currentLanguage),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            } else {
                                Text(
                                    text = StringResourceManager.getString("free", currentLanguage),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = StringResourceManager.getString("not_authenticated", currentLanguage),
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
                texto = StringResourceManager.getString("home", currentLanguage),
                onClick = {
                    onCloseDrawer()
                }
            )

            MenuLateralItem(
                icon = Icons.Default.ShoppingCart,
                texto = StringResourceManager.getString("markets", currentLanguage),
                onClick = {
                    onCloseDrawer()
                }
            )

            if (configuracion?.isPremium == true) {
                MenuLateralItem(
                    icon = Icons.Default.Add,
                    texto = StringResourceManager.getString("nuevo_mercadillo", currentLanguage),
                    onClick = {
                        onCloseDrawer()
                        // TODO: Navegar a añadir mercadillo
                    }
                )

                MenuLateralItem(
                    icon = Icons.Default.Star,
                    texto = StringResourceManager.getString("premium_features", currentLanguage),
                    onClick = {
                        onCloseDrawer()
                        // TODO: Mostrar funciones premium
                    }
                )
            } else {
                MenuLateralItem(
                    icon = Icons.Default.Star,
                    texto = "🚀 ${StringResourceManager.getString("go_premium", currentLanguage)}",
                    onClick = {
                        onCloseDrawer()
                        // TODO: Mostrar pantalla de upgrade
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MenuLateralItem(
                icon = Icons.Default.Settings,
                texto = StringResourceManager.getString("configuracion", currentLanguage),
                onClick = {
                    // ✅ TODO: Implementar navegación a configuración
                    onCloseDrawer()
                }
            )

            MenuLateralItem(
                icon = Icons.Default.Info,
                texto = StringResourceManager.getString("about", currentLanguage),
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
                    texto = StringResourceManager.getString("cerrar_sesion", currentLanguage),
                    onClick = {
                        mostrarDialogoLogout = true
                    },
                    isDestructive = true
                )
            }

            MenuLateralItem(
                icon = Icons.Default.Close,
                texto = StringResourceManager.getString("salir", currentLanguage),
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