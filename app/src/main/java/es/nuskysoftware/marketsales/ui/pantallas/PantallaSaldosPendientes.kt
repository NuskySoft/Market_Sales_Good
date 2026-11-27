package es.nuskysoftware.marketsales.ui.pantallas

// Responsabilidad: Lista los saldos guardados disponibles y las acciones para retirarlos.
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
import es.nuskysoftware.marketsales.data.repository.*
import es.nuskysoftware.marketsales.utils.*
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Presenta los saldos disponibles de la cuenta y permite seleccionar las acciones admitidas.
fun PantallaSaldosPendientes(navController: NavController? = null) {
    val context = LocalContext.current
    val repo = remember { SaldoGuardadoRepository(context) }
    val markets = remember { MercadilloRepository(context) }
    val balances by remember { repo.pendientes() }.collectAsState(emptyList())
    val destinations by remember { markets.getMercadillosUsuarioActual() }.collectAsState(emptyList())
    val lang by ConfigurationManager.idioma.collectAsState()
    val scope = rememberCoroutineScope()
    var delete by remember { mutableStateOf<SaldoGuardadoEntity?>(null) }
    var assign by remember { mutableStateOf<SaldoGuardadoEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun tr(key: String) = StringResourceManager.getString(key, lang)
    Scaffold(topBar = { TopAppBar(title = { Text(tr("saldos_pendientes_asignar")) }, navigationIcon = { TextButton(onClick = { navController?.popBackStack() }) { Text(tr("volver")) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (balances.isEmpty()) item { Text(tr("no_pending_balances")) }
            items(balances, key = { it.idRegistro }) { balance ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(balance.fechaMercadillo + " · " + balance.lugarMercadillo)
                        Text(MonedaUtils.formatearImporte(balance.saldoInicialGuardado, ConfigurationManager.moneda.value), style = MaterialTheme.typography.headlineSmall)
                        Row {
                            TextButton(onClick = { assign = balance }) { Text(tr("asignar_saldo")) }
                            TextButton(onClick = { delete = balance }) { Text(tr("eliminar")) }
                        }
                    }
                }
            }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        }
    }
    if (delete != null) AlertDialog(onDismissRequest = { delete = null }, title = { Text(tr("eliminar")) }, text = { Text(tr("balance_delete_confirm")) }, confirmButton = { TextButton(onClick = {
        val id = delete!!.idRegistro; delete = null
        scope.launch { try { repo.borrarGuardado(id) } catch (e: Exception) { error = e.message ?: tr("local_save_failed") } }
    }) { Text(tr("eliminar")) } }, dismissButton = { TextButton(onClick = { delete = null }) { Text(tr("cancelar")) } })
    if (assign != null) AlertDialog(onDismissRequest = { assign = null }, title = { Text(tr("asignar_saldo")) }, text = {
        Column {
            destinations.filter { it.estado in 1..3 && (it.saldoInicial == null || it.saldoInicial == 0.0) }.forEach { market ->
                TextButton(onClick = { val id = assign!!.idRegistro; assign = null; scope.launch { try { repo.asignar(id, market.idMercadillo) } catch (e: Exception) { error = e.message ?: tr("local_save_failed") } } }) { Text(market.fecha + " · " + market.lugar) }
            }
        }
    }, confirmButton = { TextButton(onClick = { assign = null }) { Text(tr("cancelar")) } })
}
