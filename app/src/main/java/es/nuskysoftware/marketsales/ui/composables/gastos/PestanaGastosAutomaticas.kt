// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/gastos/PestanaGastosAutomaticas.kt
package es.nuskysoftware.marketsales.ui.composables.gastos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel

@Composable
fun PestanaGastosAutomaticas(
    gastosViewModel: GastosViewModel,
    mercadilloActivo: MercadilloEntity
) {
    // Placeholder temporal (como Productos en Ventas)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Gastos automáticos — próximamente",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
