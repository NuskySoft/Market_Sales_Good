// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaInventario.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import androidx.compose.ui.res.painterResource
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.utils.safePopBackStack
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInventario(
    navController: NavController? = null
) {
    val currentLanguage = ConfigurationManager.idioma.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringResourceManager.getString("inventario", currentLanguage), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController?.safePopBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Atrás" // accesibilidad; no visible, se deja literal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📦", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    StringResourceManager.getString("pendiente_desarrollo", currentLanguage),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    StringResourceManager.getString("muy_pronto_gestionar_stock", currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = { navController?.safePopBackStack() }) {
                    Text(StringResourceManager.getString("volver", currentLanguage))
                }
            }
        }
        AdsBottomBar()
    }
}
