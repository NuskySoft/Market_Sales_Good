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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.safePopBackStack
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
                        IconButton(onClick = { navController.safePopBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_left),
                                contentDescription = StringResourceManager.getString("volver", currentLanguage)
                                    .ifBlank { "Volver" },
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
                onCancelar = { navController.safePopBackStack() },
                onAceptar = { navController.safePopBackStack() }
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
                // Lista compacta editable
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
                                // Acento lateral
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(IntrinsicSize.Min)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                )

                                Spacer(Modifier.width(10.dp))

                                // Descripción
                                OutlinedTextField(
                                    value = linea.descripcion,
                                    onValueChange = { gastosViewModel.editarDescripcionLinea(linea.id, it) },
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
                                    keyboardActions = KeyboardActions(onNext = { /* foco al importe */ })
                                )

                                // Importe
                                OutlinedTextField(
                                    value = gastosViewModel.formatearImporteEditable(linea.importe),
                                    onValueChange = { gastosViewModel.editarImporteLinea(linea.id, it) },
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
                                    modifier = Modifier.widthIn(min = 90.dp, max = 120.dp)
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
        AdsBottomBar()
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
    Surface(tonalElevation = 4.dp, shadowElevation = 6.dp) {
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
                    text = StringResourceManager.getString("total", currentLanguage).ifBlank { "Total" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = totalFmt, style = MaterialTheme.typography.titleLarge)
            }
            OutlinedButton(onClick = onCancelar) {
                Text(StringResourceManager.getString("cancelar", currentLanguage).ifBlank { "Cancelar" })
            }
            Button(onClick = onAceptar) {
                Text(StringResourceManager.getString("aceptar", currentLanguage).ifBlank { "Aceptar" })
            }
        }
    }
}
