package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun PantallaLogoutSplash(
    navController: NavController
) {
    val currentLanguage = ConfigurationManager.idioma.collectAsState().value

    // Pantalla simple: informa que la sesión se ha cerrado y redirige a "mercadillos"
    LaunchedEffect(Unit) {
        // pequeña pausa para que el usuario lo vea claramente
        delay(1500)
        navController.navigate("mercadillos") {
            // elimina la pantalla de logout del back stack
            popUpTo("logout") { inclusive = true }
            launchSingleTop = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icono grande opcional (usa ic_logout si lo tienes)
                Icon(
                    painter = painterResource(id = es.nuskysoftware.marketsales.R.drawable.ic_logout),
                    contentDescription = "Sesión cerrada",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = StringResourceManager.getString("sesion_cerrada", currentLanguage),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = StringResourceManager.getString("redirigiendo_al_inicio", currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
            }
        }
    }
}
