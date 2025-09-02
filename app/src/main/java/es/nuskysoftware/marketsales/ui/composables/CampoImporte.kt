// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/CampoImporte.kt
package es.nuskysoftware.marketsales.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun CampoImporte(
    importe: String,
    modifier: Modifier = Modifier
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()

    // Intentamos parsear el string del importe (admitiendo coma o punto)
    val amountText = remember(importe, moneda) {
        val value = importe
            .replace(',', '.')
            .toDoubleOrNull() ?: 0.0
        MonedaUtils.formatearImporte(value, moneda)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = StringResourceManager.getString("importe", currentLanguage),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))
            val moneda by ConfigurationManager.moneda.collectAsState()
            val simbolo = MonedaUtils.obtenerSimboloMoneda(moneda)
            Text(
                //text = amountText,
                text = "${importe.replace('.', ',')} $simbolo",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}
