// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaArqueoCaja.kt
/**
 * Pantalla "Arqueo de caja".
 * Muestra: saldo inicial + ventas en efectivo - gastos en efectivo (de momento 0).
 * Solo presentación; el botón de confirmar quedará deshabilitado hasta conectar la lógica.
 */
package es.nuskysoftware.marketsales.ui.pantallas.arqueo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaArqueoCaja(
    navController: NavController,
    mercadilloId: String
) {
    val context = LocalContext.current
    val repo = remember { MercadilloRepository(context) }
    val vm: ArqueoViewModel = viewModel(factory = ArqueoViewModelFactory(context, repo))
    val ui by vm.ui.collectAsState()

    LaunchedEffect(mercadilloId) { vm.cargar(mercadilloId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arqueo de caja") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { pad ->
        when {
            ui.loading -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.error != null -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text(ui.error!!, color = MaterialTheme.colorScheme.error) }
            ui.mercadillo != null -> {
                val m = ui.mercadillo!!
                val gastosEfectivo = 0.0 // Pendiente módulo de gastos
                val resultado = m.saldoInicial + m.ventasEfectivo - gastosEfectivo

                Column(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KeyValueLine("Saldo inicial", vm.fmtMoneda(m.saldoInicial))
                            KeyValueLine("Ventas en efectivo", vm.fmtMoneda(m.ventasEfectivo))
                            KeyValueLine("Gastos en efectivo", vm.fmtMoneda(gastosEfectivo))
                            Divider()
                            KeyValueLine("Resultado del arqueo", vm.fmtMoneda(resultado), bold = true)
                        }
                    }
                    Button(
                        onClick = { /* lógica más adelante */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Confirmar arqueo (pendiente)") }
                }
            }
        }
    }
}

/* ====== UI helper local ====== */

@Composable
private fun KeyValueLine(label: String, value: String, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else null
        )
    }
}


