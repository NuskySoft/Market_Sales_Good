package es.nuskysoftware.marketsales.ui.components.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import java.util.Calendar

@Composable
fun CalendarioGrid(
    ano: Int,
    mes: Int, // 1..12
    mercadillosPorDia: Map<Int, List<MercadilloEntity>>,
    onDiaClick: (Int) -> Unit
) {
    val diasDelMes = when (mes) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (ano % 4 == 0 && (ano % 100 != 0 || ano % 400 == 0)) 29 else 28
        else -> 31
    }

    val calendar = Calendar.getInstance().apply { set(ano, mes - 1, 1) }
    val primerDiaSemana = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // L=0 ... D=6

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(primerDiaSemana) { Box(modifier = Modifier.size(40.dp)) }

        items((1..diasDelMes).toList()) { dia ->
            DiaCalendario(
                dia = dia,
                mercadillos = mercadillosPorDia[dia] ?: emptyList(),
                onClick = { onDiaClick(dia) }
            )
        }
    }
}

@Composable
private fun DiaCalendario(
    dia: Int,
    mercadillos: List<MercadilloEntity>,
    onClick: () -> Unit
) {
    val estados = mercadillos.map { m ->
        EstadosMercadillo.Estado.fromCodigo(m.estado) ?: EstadosMercadillo.Estado.PROGRAMADO_PARCIAL
    }.sortedBy { EstadosMercadillo.obtenerPrioridad(it) }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
            .background(
                color = if (estados.size == 1) {
                    EstadosMercadillo.obtenerColor(estados.first())
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dia.toString(),
                color = if (estados.size == 1) {
                    EstadosMercadillo.obtenerColorTexto(estados.first())
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontSize = 13.sp
            )

            if (estados.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
                    estados.take(2).forEach { estado ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(EstadosMercadillo.obtenerColor(estado), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
