// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/gastos/PantallaGastosCarrito.kt
package es.nuskysoftware.marketsales.ui.pantallas.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.elevatedCardColors
import androidx.compose.material3.CardDefaults.elevatedCardElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGastosCarrito(
    navController: NavController,
    gastosViewModel: GastosViewModel,
    drawerState: DrawerState? = null
) {
    val ui by gastosViewModel.uiState.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = StringResourceManager.getString("gastos_carrito", currentLanguage)
                            .ifBlank { "Carrito de gastos" },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    if (drawerState != null) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu),
                                contentDescription = StringResourceManager.getString("menu", currentLanguage)
                                    .ifBlank { "Menú" },
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomSummaryBar(
                total = ui.totalCarrito,
                moneda = moneda,
                currentLanguage = currentLanguage,
                onCancelar = { navController.popBackStack() },
                onAceptar = { navController.popBackStack() }
            )
        }
    ) { padding ->
        val top = MaterialTheme.colorScheme.surface
        val elevated = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(top, elevated),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                        tileMode = TileMode.Clamp
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // LISTA — cada gasto en UNA SOLA LÍNEA dentro de una Card estilizada
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(
                        items = ui.lineasCarrito,
                        key = { it.id }
                    ) { linea ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            elevation = elevatedCardElevation(defaultElevation = 3.dp),
                            colors = elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Acento lateral sutil
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(IntrinsicSize.Min)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                )

                                Spacer(Modifier.width(10.dp))

                                // Descripción (ocupa todo el espacio disponible)
                                OutlinedTextField(
                                    value = linea.descripcion,
                                    onValueChange = {
                                        gastosViewModel.editarDescripcionLinea(linea.id, it)
                                    },
                                    label = {
                                        Text(
                                            StringResourceManager.getString("descripcion", currentLanguage)
                                                .ifBlank { "Descripción" }
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { /* pasa al importe */ })
                                )

                                // Importe (campo MÁS PEQUEÑO)
                                OutlinedTextField(
                                    value = gastosViewModel.formatearImporteEditable(linea.importe),
                                    onValueChange = {
                                        gastosViewModel.editarImporteLinea(linea.id, it)
                                    },
                                    label = {
                                        Text(
                                            StringResourceManager.getString("importe", currentLanguage)
                                                .ifBlank { "Importe" }
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    suffix = {
                                        Text(
                                            text = moneda,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.alpha(0.7f),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier
                                        .widthIn(min = 90.dp, max = 120.dp) // compacto
                                )

                                // Eliminar
                                IconButton(
                                    onClick = { gastosViewModel.eliminarLinea(linea.id) },
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = StringResourceManager
                                            .getString("eliminar", currentLanguage)
                                            .ifBlank { "Eliminar" }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSummaryBar(
    total: Double,
    moneda: String,
    currentLanguage: String,
    onCancelar: () -> Unit,
    onAceptar: () -> Unit
) {
    val totalFmt = MonedaUtils.formatearImporte(total, moneda)
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = StringResourceManager.getString("total", currentLanguage)
                        .ifBlank { "Total" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalFmt,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            OutlinedButton(onClick = onCancelar) {
                Text(
                    StringResourceManager.getString("cancelar", currentLanguage)
                        .ifBlank { "Cancelar" }
                )
            }

            Button(onClick = onAceptar) {
                Text(
                    StringResourceManager.getString("aceptar", currentLanguage)
                        .ifBlank { "Aceptar" }
                )
            }
        }
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/gastos/PantallaGastosCarrito.kt
//package es.nuskysoftware.marketsales.ui.pantallas.gastos
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.MonedaUtils
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaGastosCarrito(
//    navController: NavController,
//    gastosViewModel: GastosViewModel
//) {
//    val ui by gastosViewModel.uiState.collectAsState()
//    val moneda by ConfigurationManager.moneda.collectAsState()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Carrito de gastos") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.surface) // invisible, solo mantiene layout
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(12.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            // Lista editable
//            LazyColumn(
//                modifier = Modifier.weight(1f),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(ui.lineasCarrito, key = { it.id }) { linea ->
//                    Card {
//                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                OutlinedTextField(
//                                    value = linea.descripcion,
//                                    onValueChange = { gastosViewModel.editarDescripcionLinea(linea.id, it) },
//                                    label = { Text("Descripción") },
//                                    modifier = Modifier.weight(1f)
//                                )
//                                IconButton(
//                                    onClick = { gastosViewModel.eliminarLinea(linea.id) },
//                                    modifier = Modifier.padding(start = 8.dp)
//                                ) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
//                            }
//
//                            OutlinedTextField(
//                                value = gastosViewModel.formatearImporteEditable(linea.importe),
//                                onValueChange = { gastosViewModel.editarImporteLinea(linea.id, it) },
//                                label = { Text("Importe") },
//                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                singleLine = true
//                            )
//                        }
//                    }
//                }
//            }
//
//            // Total y acciones
//            val totalFmt = MonedaUtils.formatearImporte(ui.totalCarrito, moneda)
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                Text("Total: $totalFmt", style = MaterialTheme.typography.titleMedium)
//                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                    OutlinedButton(onClick = { navController.popBackStack() }) { Text("Cancelar") }
//                    Button(onClick = { navController.popBackStack() }) { Text("Aceptar") }
//                }
//            }
//        }
//    }
//}
