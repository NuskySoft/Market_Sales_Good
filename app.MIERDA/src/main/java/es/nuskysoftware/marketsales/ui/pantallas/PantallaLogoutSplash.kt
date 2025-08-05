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

@Composable
fun PantallaLogoutSplash(
    navController: NavController
) {
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
                    text = "Sesión cerrada",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Redirigiendo al inicio…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
            }
        }
    }
}
