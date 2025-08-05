package es.nuskysoftware.marketsales.ui.components.mercadillos

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.ui.components.proximos.CardMercadillosProximos
import es.nuskysoftware.marketsales.ui.viewmodel.ProximosMercadillosViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ProximosMercadillosViewModelFactory
import es.nuskysoftware.marketsales.utils.EstadosMercadillo

@Composable
fun ProximosMercadillosSection(
    navController: NavController?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vm: ProximosMercadillosViewModel =
        viewModel(factory = ProximosMercadillosViewModelFactory(context))
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.cargar() }

    when {
        ui.loading -> LinearProgressIndicator(modifier = modifier)
        ui.error != null -> Text(
            ui.error ?: "",
            color = MaterialTheme.colorScheme.error,
            modifier = modifier
        )
        else -> {
            val proximos = ui.items.filter { m ->
                when (EstadosMercadillo.Estado.fromCodigo(m.estado)) {
                    EstadosMercadillo.Estado.PROGRAMADO_PARCIAL,
                    EstadosMercadillo.Estado.PROGRAMADO_TOTAL -> true
                    else -> false
                }
            }
            if (proximos.isNotEmpty()) {
                CardMercadillosProximos(
                    mercadillosProximos = proximos,
                    onMercadilloClick = { m -> navegarSegunEstado(navController, m) }
                )
            }
        }
    }
}

private fun navegarSegunEstado(navController: NavController?, m: MercadilloEntity) {
    val estado = EstadosMercadillo.Estado.fromCodigo(m.estado)
    val ruta =
        if (estado == EstadosMercadillo.Estado.PENDIENTE_ARQUEO ||
            estado == EstadosMercadillo.Estado.PENDIENTE_ASIGNAR_SALDO ||
            estado == EstadosMercadillo.Estado.CERRADO_COMPLETO
        ) "arqueo/${m.idMercadillo}"
        else "editar_mercadillo/${m.idMercadillo}"
    navController?.navigate(ruta)
}
