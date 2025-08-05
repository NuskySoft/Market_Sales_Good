// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/gastos/PantallaGastosCarrito.kt
package es.nuskysoftware.marketsales.ui.pantallas.gastos

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
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGastosCarrito(
    navController: NavController,
    gastosViewModel: GastosViewModel
) {
    val ui by gastosViewModel.uiState.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carrito de gastos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.surface) // invisible, solo mantiene layout
                    }
                }
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
            // Lista editable
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.lineasCarrito, key = { it.id }) { linea ->
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = linea.descripcion,
                                    onValueChange = { gastosViewModel.editarDescripcionLinea(linea.id, it) },
                                    label = { Text("Descripción") },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { gastosViewModel.eliminarLinea(linea.id) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
                            }

                            OutlinedTextField(
                                value = gastosViewModel.formatearImporteEditable(linea.importe),
                                onValueChange = { gastosViewModel.editarImporteLinea(linea.id, it) },
                                label = { Text("Importe") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Total y acciones
            val totalFmt = MonedaUtils.formatearImporte(ui.totalCarrito, moneda)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: $totalFmt", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { navController.popBackStack() }) { Text("Cancelar") }
                    Button(onClick = { navController.popBackStack() }) { Text("Aceptar") }
                }
            }
        }
    }
}
