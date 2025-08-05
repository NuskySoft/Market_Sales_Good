// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaResultadoMercadillo.kt
/**
 * Pantalla "Resultado del mercadillo".
 * Muestra: saldoInicial + (ventas por método) - (gastos por método: 0 por ahora) - suscripción.
 * Usa ArqueoViewModel para cargar datos; no modifica estado (solo lectura).
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
fun PantallaResultadoMercadillo(
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
                title = { Text("Resultado del mercadillo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { pad ->
        when {
            ui.loading -> Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.error != null -> Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) { Text(ui.error!!, color = MaterialTheme.colorScheme.error) }

            ui.mercadillo != null -> {
                val m = ui.mercadillo!!

                // Gastos por método: aún 0.0 (a falta del módulo de gastos)
                val gastosEf = 0.0
                val gastosBi = 0.0
                val gastosTa = 0.0

                val ventasTotal = m.ventasEfectivo + m.ventasBizum + m.ventasTarjeta // ≈ m.totalVentas
                val gastosTotal = gastosEf + gastosBi + gastosTa
                val resultado = m.saldoInicial + ventasTotal - gastosTotal - m.importeSuscripcion

                Column(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            KeyValueLine("Saldo inicial", vm.fmtMoneda(m.saldoInicial))
                            Divider()
                            Text("Ventas por método", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            KeyValueLine("Efectivo", vm.fmtMoneda(m.ventasEfectivo))
                            KeyValueLine("Bizum", vm.fmtMoneda(m.ventasBizum))
                            KeyValueLine("Tarjeta", vm.fmtMoneda(m.ventasTarjeta))
                            KeyValueLine("Total ventas", vm.fmtMoneda(ventasTotal), bold = true)
                            Divider()
                            Text("Gastos por método", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            KeyValueLine("Efectivo", vm.fmtMoneda(gastosEf))
                            KeyValueLine("Bizum", vm.fmtMoneda(gastosBi))
                            KeyValueLine("Tarjeta", vm.fmtMoneda(gastosTa))
                            KeyValueLine("Total gastos", vm.fmtMoneda(gastosTotal))
                            Divider()
                            KeyValueLine("Suscripción", vm.fmtMoneda(m.importeSuscripcion))
                            Divider()
                            KeyValueLine("Resultado del mercadillo", vm.fmtMoneda(resultado), bold = true)
                        }
                    }
                }
            }
        }
    }
}

/* ===== Helper local sin colisiones con otras pantallas ===== */

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

