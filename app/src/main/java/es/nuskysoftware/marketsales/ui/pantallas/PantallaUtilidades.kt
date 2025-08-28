package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaUtilidades(navController: NavController? = null) {
    val currentLanguage = ConfigurationManager.idioma.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringResourceManager.getString("utilidades", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OpcionUtilidad(
                iconRes = R.drawable.ic_money,
                title = StringResourceManager.getString("saldos_pendientes", currentLanguage)
            ) {
                navController?.navigate("saldos_pendientes")
            }
            OpcionUtilidad(
                iconRes = R.drawable.ic_account,
                title = StringResourceManager.getString("datos_empresa", currentLanguage)
            ) {
                navController?.navigate("datos_empresa")
            }
        }
    }
}

@Composable
private fun OpcionUtilidad(iconRes: Int, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}