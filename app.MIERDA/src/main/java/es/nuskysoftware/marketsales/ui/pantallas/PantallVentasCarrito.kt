// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaVentasCarrito.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils

/**
 * Carrito de la compra (edición del ticket en curso)
 * - Encabezado: idRecibo provisional (RC hhmmss - IDIOMA)
 * - Lista de líneas en tarjetas horizontales
 * - Editar unidades y precio unitario
 * - Eliminar línea
 * - Total al pie y botones Cancelar/Aceptar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVentasCarrito(
    navController: NavController,
    // Para compartir el mismo VM de PantallaVentas, pásame su backStackEntry si lo usas así
    ventasViewModel: VentasViewModel? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Si no inyectan VM (porque llamas directo), crea uno normal; si compartes, pásalo desde el host
    val vm = ventasViewModel ?: viewModel<VentasViewModel>(factory = VentasViewModelFactory(context))

    val uiState by vm.uiState.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()
    val idioma by ConfigurationManager.idioma.collectAsState()

    // Id provisional para mostrar (no se usa para persistir)
    val idReciboHeader by remember {
        mutableStateOf("RC" + java.text.SimpleDateFormat("HHmmss").format(java.util.Date()) + "-" + idioma.uppercase())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = idReciboHeader) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painter = androidx.compose.ui.res.painterResource(id = es.nuskysoftware.marketsales.R.drawable.ic_arrow_left), contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Lista de líneas
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.lineasTicket, key = { it.id }) { linea ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Descripción
                            Text(text = linea.descripcion, style = MaterialTheme.typography.titleMedium)

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Unidades (–  [qty]  +)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            val nueva = (linea.cantidad - 1).coerceAtLeast(0)
                                            vm.editarCantidadLinea(linea.id, nueva)
                                        }
                                    ) { Text("–") }

                                    Text(text = linea.cantidad.toString(), modifier = Modifier.widthIn(min = 24.dp))

                                    TextButton(onClick = { vm.editarCantidadLinea(linea.id, linea.cantidad + 1) }) {
                                        Text("+")
                                    }
                                }

                                // Precio unitario (editable)
                                var precioTxt by remember(linea.id, linea.precioUnitario) {
                                    mutableStateOf(
                                        // mostramos con coma si así formateas en UI
                                        String.format(java.util.Locale.US, "%.2f", linea.precioUnitario).replace('.', ',')
                                    )
                                }


                                // Total (calculado)
                                Text(
                                    text = MonedaUtils.formatearImporte(linea.subtotal, moneda),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                // Eliminar
                                IconButton(onClick = { vm.eliminarLinea(linea.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar")
                                }
                            }
                        }
                    }
                }
            }

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = MonedaUtils.formatearImporte(uiState.totalTicket, moneda),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Botonera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Cancelar") }

                Button(
                    onClick = { navController.popBackStack() }, // los cambios ya están en el VM
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Aceptar") }
            }
        }
    }
}
