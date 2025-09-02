// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaSaldosPendientes.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.ui.viewmodel.SaldoPendienteViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.SaldoPendienteViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.safePopBackStack
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSaldosPendientes(navController: NavController? = null) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val context = LocalContext.current
    val viewModel: SaldoPendienteViewModel =
        viewModel(factory = SaldoPendienteViewModelFactory(context))
    val saldoGuardadoState by viewModel.saldoGuardado.collectAsState()

    var fecha by remember { mutableStateOf("") }
    var lugar by remember { mutableStateOf("") }
    var organizador by remember { mutableStateOf("") }
    var saldo by remember { mutableStateOf("") }

    LaunchedEffect(saldoGuardadoState) {
        saldoGuardadoState?.let {
            fecha = it.fechaMercadillo
            lugar = it.lugarMercadillo
            organizador = it.organizadorMercadillo
            val nf = NumberFormat.getNumberInstance(Locale("es", "ES")).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            saldo = nf.format(it.saldoInicialGuardado)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = StringResourceManager.getString(
                            "saldos_pendientes_asignar",
                            currentLanguage
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.safePopBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = StringResourceManager.getString("volver", currentLanguage),
                            tint = MaterialTheme.colorScheme.onPrimary
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

        val top = MaterialTheme.colorScheme.surface
        val elevated = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(elevated.copy(alpha = 0.12f))
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Card destacada con el saldo
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = StringResourceManager.getString("saldo_guardado", currentLanguage),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = saldo.ifBlank { "—" },
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold
                    )
                    // Píldora de estado
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = StringResourceManager.getString("saldos_pendientes_asignar", currentLanguage),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Detalle del mercadillo en una card elegante
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(containerColor = top),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReadOnlyItem(
                        label = StringResourceManager.getString("fecha_mercadillo", currentLanguage),
                        value = fecha
                    )
                    Divider()
                    ReadOnlyItem(
                        label = StringResourceManager.getString("lugar_mercadillo", currentLanguage),
                        value = lugar
                    )
                    Divider()
                    ReadOnlyItem(
                        label = StringResourceManager.getString("organizador_mercadillo", currentLanguage),
                        value = organizador
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón de cierre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { navController?.safePopBackStack() }
                ) {
                    Text(StringResourceManager.getString("cerrar", currentLanguage))
                }
            }
        }
    }
    AdsBottomBar()
}

@Composable
private fun ReadOnlyItem(
    label: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Chip de etiqueta
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        // Valor
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
