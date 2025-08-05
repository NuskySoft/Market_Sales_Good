// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaAsignarSaldo.kt
/**
 * Pantalla "Asignar saldo":
 * - Lista de mercadillos en estados 1 y 2 (solo lectura por ahora) para ver potenciales destinos.
 * - Muestra fecha/lugar/estado y si tiene saldo inicial.
 */
package es.nuskysoftware.marketsales.ui.pantallas.arqueo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModelFactory
import es.nuskysoftware.marketsales.utils.EstadosMercadillo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAsignarSaldo(
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
                title = { Text("Asignar saldo") },
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
            else -> {
                val destinos = ui.destinosSaldo
                Column(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (destinos.isEmpty()) {
                        Text("No hay mercadillos en estado 1/2 disponibles.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(destinos) { d ->
                                DestinoCard(item = d, fmt = vm::fmtMoneda)
                            }
                        }
                    }
                    Button(
                        onClick = { /* lógica posterior */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Asignar saldo (pendiente)") }
                }
            }
        }
    }
}

@Composable
private fun DestinoCard(item: MercadilloEntity, fmt: (Double) -> String) {
    val est = EstadosMercadillo.Estado.fromCodigo(item.estado) ?: EstadosMercadillo.Estado.PROGRAMADO_PARCIAL
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${item.fecha} • ${item.lugar}", style = MaterialTheme.typography.titleSmall)
            Text("Estado: ${est.descripcion}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Saldo inicial: ${item.saldoInicial?.let { fmt(it) } ?: fmt(0.0)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

