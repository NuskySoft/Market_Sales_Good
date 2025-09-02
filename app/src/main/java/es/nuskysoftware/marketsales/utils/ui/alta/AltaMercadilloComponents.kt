// app/src/main/java/es/nuskysoftware/marketsales/utils/ui/alta/CamposAlta.kt
package es.nuskysoftware.marketsales.utils.ui.alta

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.utils.StringResourceManager
import java.text.SimpleDateFormat
import java.util.*

/* --------- Campos y bloques reutilizables --------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoEstadoDebug(
    estadoActual: Int,
    onEstadoChange: (EstadosMercadillo.Estado) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    fun t(k: String) = StringResourceManager.getString(k, currentLanguage)

    var expanded by remember { mutableStateOf(false) }
    var estadoSeleccionado by remember(estadoActual) {
        mutableStateOf(
            EstadosMercadillo.Estado.fromCodigo(estadoActual)
                ?: EstadosMercadillo.Estado.PROGRAMADO_PARCIAL
        )
    }

    Column(modifier = modifier) {
        Text(
            t("estado_debug_titulo"),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Red
        )
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = "${estadoSeleccionado.codigo} - ${estadoSeleccionado.descripcion}",
                onValueChange = { }, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Red,
                    unfocusedBorderColor = Color.Red.copy(alpha = 0.5f)
                ),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                EstadosMercadillo.obtenerTodosLosEstados().forEach { estado ->
                    DropdownMenuItem(
                        text = { Text("${estado.codigo} - ${estado.descripcion}") },
                        onClick = { estadoSeleccionado = estado; onEstadoChange(estado); expanded = false }
                    )
                }
            }
        }
        Text(
            t("estado_debug_aviso"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Red.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

@Composable
fun CampoFecha(
    fecha: String,
    onFechaChange: (String) -> Unit,
    onMostrarDatePicker: () -> Unit,
    enabled: Boolean
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    fun t(k: String) = StringResourceManager.getString(k, currentLanguage)

    Column {
        Text(t("fecha"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = fecha,
            onValueChange = onFechaChange,
            placeholder = { Text(t("formato_fecha_hint")) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable { onMostrarDatePicker() } else Modifier),
            readOnly = true,
            trailingIcon = { IconButton(onClick = onMostrarDatePicker, enabled = enabled) { Text("📅", fontSize = 18.sp) } }
        )
    }
}

@Composable
fun CampoTexto(
    valor: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    // El label/placeholder llegan ya localizados desde el llamador
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = valor,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ConfiguracionEconomica(
    esGratis: Boolean,
    onEsGratisChange: (Boolean) -> Unit,
    importeSuscripcion: String,
    onImporteSuscripcionChange: (String) -> Unit
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()
    val simboloMoneda = remember(moneda) { moneda.split(" ").firstOrNull() ?: "€" }
    fun t(k: String) = StringResourceManager.getString(k, currentLanguage)

    Column {
        Text(t("config_economica"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = esGratis, onCheckedChange = onEsGratisChange)
            Spacer(Modifier.width(8.dp))
            Text(t("mercadillo_gratuito"))
        }
        if (!esGratis) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = importeSuscripcion,
                onValueChange = onImporteSuscripcionChange,
                label = { Text(t("importe_suscripcion") + " (${simboloMoneda})") },
                placeholder = { Text(t("importe_placeholder")) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ConfiguracionLogistica(
    requiereMesa: Boolean,
    onRequiereMesaChange: (Boolean) -> Unit,
    requiereCarpa: Boolean,
    onRequiereCarpaChange: (Boolean) -> Unit,
    hayPuntoLuz: Boolean,
    onHayPuntoLuzChange: (Boolean) -> Unit
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    fun t(k: String) = StringResourceManager.getString(k, currentLanguage)

    Column {
        Text(t("config_logistica"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = requiereMesa, onCheckedChange = onRequiereMesaChange)
            Text(t("requiere_mesa"))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = requiereCarpa, onCheckedChange = onRequiereCarpaChange)
            Text(t("requiere_carpa"))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = hayPuntoLuz, onCheckedChange = onHayPuntoLuzChange)
            Text(t("hay_punto_luz"))
        }
    }
}

@Composable
fun ConfiguracionHorarios(
    horaInicio: String,
    onHoraInicioChange: (String) -> Unit,
    horaFin: String,
    onHoraFinChange: (String) -> Unit,
    onMostrarTimePickerInicio: () -> Unit,
    onMostrarTimePickerFin: () -> Unit
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    fun t(k: String) = StringResourceManager.getString(k, currentLanguage)

    Column {
        Text(t("horarios"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(t("hora_inicio"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                OutlinedTextField(
                    value = horaInicio,
                    onValueChange = onHoraInicioChange,
                    placeholder = { Text(t("formato_hora_hint")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMostrarTimePickerInicio() },
                    readOnly = true,
                    trailingIcon = { IconButton(onClick = onMostrarTimePickerInicio) { Text("🕐", fontSize = 18.sp) } }
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(t("hora_fin"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                OutlinedTextField(
                    value = horaFin,
                    onValueChange = onHoraFinChange,
                    placeholder = { Text(t("formato_hora_hint")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMostrarTimePickerFin() },
                    readOnly = true,
                    trailingIcon = { IconButton(onClick = onMostrarTimePickerFin) { Text("🕐", fontSize = 18.sp) } }
                )
            }
        }
    }
}

@Composable
fun CampoSaldoInicial(
    saldoInicial: String,
    onSaldoInicialChange: (String) -> Unit
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()
    val simboloMoneda = remember(moneda) { moneda.split(" ").firstOrNull() ?: "€" }
    fun t(k: String) = StringResourceManager.getString(k, currentLanguage)

    Column {
        Text(t("saldo_inicial_opcional"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = saldoInicial,
            onValueChange = onSaldoInicialChange,
            placeholder = { Text(t("importe_placeholder")) },
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text(simboloMoneda) }
        )
    }
}

/* --------- Date/Time pickers (custom) --------- */

@Composable
fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    currentLanguage: String
) {
    val calendar = remember { Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY; time = Date() } }
    var selectedDate by remember { mutableStateOf(calendar.clone() as Calendar) }
    var currentViewDate by remember { mutableStateOf(calendar.clone() as Calendar) }
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", if (currentLanguage == "es") Locale("es", "ES") else Locale.ENGLISH)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = StringResourceManager.getString("seleccionar_fecha", currentLanguage),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(320.dp)) {
                SelectedDateDisplay(selectedDate, currentLanguage)
                Spacer(Modifier.height(16.dp))
                MonthYearNavigation(currentViewDate = currentViewDate, onMonthChange = { currentViewDate = it }, currentLanguage = currentLanguage)
                Spacer(Modifier.height(16.dp))
                CalendarGrid(currentViewDate = currentViewDate, selectedDate = selectedDate, onDateSelected = { selectedDate = it }, currentLanguage = currentLanguage)
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(dateFormat.format(selectedDate.time)) }) {
                Text(StringResourceManager.getString("aceptar", currentLanguage))
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(StringResourceManager.getString("cancelar", currentLanguage)) } }
    )
}

@Composable
private fun SelectedDateDisplay(selectedDate: Calendar, currentLanguage: String) {
    val dayFormat = SimpleDateFormat("EEEE", if (currentLanguage == "es") Locale("es", "ES") else Locale.ENGLISH)
    val displayFormat = if (currentLanguage == "es") SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    else SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                dayFormat.format(selectedDate.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                displayFormat.format(selectedDate.time),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonthYearNavigation(currentViewDate: Calendar, onMonthChange: (Calendar) -> Unit, currentLanguage: String) {
    val monthFormat = if (currentLanguage == "es") SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    else SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMonthChange((currentViewDate.clone() as Calendar).apply { add(Calendar.MONTH, -1) }) }) {
            Text("←", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Text(
            monthFormat.format(currentViewDate.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = { onMonthChange((currentViewDate.clone() as Calendar).apply { add(Calendar.MONTH, 1) }) }) {
            Text("→", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalendarGrid(
    currentViewDate: Calendar,
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    currentLanguage: String
) {
    val diasSemana = if (currentLanguage == "es")
        listOf(
            StringResourceManager.getString("lunes", currentLanguage),
            StringResourceManager.getString("martes", currentLanguage),
            StringResourceManager.getString("miercoles", currentLanguage),
            StringResourceManager.getString("jueves", currentLanguage),
            StringResourceManager.getString("viernes", currentLanguage),
            StringResourceManager.getString("sabado", currentLanguage),
            StringResourceManager.getString("domingo", currentLanguage)
        )
    else listOf("M", "T", "W", "T", "F", "S", "S")

    Column {
        Row(Modifier.fillMaxWidth()) {
            diasSemana.forEach { dia ->
                Text(
                    dia,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val firstDayOfMonth = (currentViewDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val firstDayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = currentViewDate.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = 42

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(240.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(totalCells) { index ->
                val dayNumber = index - firstDayOfWeek + 1
                if (dayNumber in 1..daysInMonth) {
                    val dayDate = (currentViewDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNumber) }
                    val isSelected = isSameDay(dayDate, selectedDate)
                    val isToday = isSameDay(dayDate, Calendar.getInstance())
                    DayCell(day = dayNumber, isSelected = isSelected, isToday = isToday) { onDateSelected(dayDate) }
                } else Box(modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean =
    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

/* --------- TimePicker --------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialTime: String = "09:00",
    currentLanguage: String
) {
    val parts = initialTime.split(":")
    val initH = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val initM = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val timePickerState = rememberTimePickerState(initialHour = initH, initialMinute = initM, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(StringResourceManager.getString("seleccionar_hora", currentLanguage)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hour = "%02d".format(timePickerState.hour)
                val minute = "%02d".format(timePickerState.minute)
                onTimeSelected("$hour:$minute")
            }) { Text(StringResourceManager.getString("aceptar", currentLanguage)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(StringResourceManager.getString("cancelar", currentLanguage)) } }
    )
}
