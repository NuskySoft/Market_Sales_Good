// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/BotonCarritoVentas.kt
package es.nuskysoftware.marketsales.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel

/**
 * Botón de carrito (solo diseño):
 * - Icono grande 40.dp
 * - Badge grande 22.dp, borde blanco 2.dp y texto 12.sp en bold
 * - Sin cambios de lógica
 */
@Composable

fun BotonCarritoVentas(
    ventasViewModel: VentasViewModel,
    onClick: () -> Unit
) {
    val uiState by ventasViewModel.uiState.collectAsState()
    val numLineas = uiState.lineasTicket.size

    Box(
        modifier = Modifier
            .size(90.dp) // Espacio para el FAB + badge
    ) {
        // FAB de carrito
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(70.dp),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shopping_cart_24),
                contentDescription = "Carrito",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(34.dp) // Icono más grande
            )
        }

        // Badge fuera del FAB, no se recorta
        if (numLineas > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp) // Posición del badge
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = numLineas.toString(),
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
