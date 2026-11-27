/**
 * Pantalla de calendario de mercadillos.
 * - Mantiene el diseño actual.
 * - Navegación por estado: 4/5/6 → pantalla de Arqueo; 1/2/3/7 → editar mercadillo.
 * - El icono de leyenda (Info) se muestra dentro del Card del calendario (no en el TopAppBar).
 */
package es.nuskysoftware.marketsales.ui.pantallas

// Responsabilidad: Pantalla principal de mercadillos, calendario, selección y acciones según el estado.

import android.os.Build
import androidx.annotation.RequiresApi
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.SyncState
import es.nuskysoftware.marketsales.ui.components.BottomBarMercadillo
import es.nuskysoftware.marketsales.ui.components.DialogoSeleccionMercadilloActivo
import es.nuskysoftware.marketsales.ui.components.DownloadProgressBar
import es.nuskysoftware.marketsales.ui.components.MenuHamburguesa
import es.nuskysoftware.marketsales.ui.components.calendario.CalendarioGrid
import es.nuskysoftware.marketsales.ui.components.dialogs.DialogoSeleccionMercadillo
import es.nuskysoftware.marketsales.ui.components.leyenda.LeyendaColoresDialog
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModelFactory
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.ui.components.mercadillos.ProximosMercadillosSection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.ads.BannerAdBottom
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import es.nuskysoftware.marketsales.ads.AdsConsentManager
import androidx.compose.ui.res.stringResource
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Observa la lista de mercadillos y permite seleccionar fechas, consultar eventos y acceder a las acciones disponibles.
fun PantallaMercadillos(
    navController: NavController? = null
) {
    val context = LocalContext.current

    val mercadilloViewModel: MercadilloViewModel = viewModel(factory = MercadilloViewModelFactory(context))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(context.applicationContext))

    // Sync/progreso
    val syncState by authViewModel.syncState.collectAsState()
    val progress by authViewModel.downloadProgress.collectAsState()
    val message by authViewModel.downloadMessage.collectAsState()

    // Anuncios
    val canRequestAds by AdsConsentManager.canRequestAds.collectAsState()

    // Config global
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val esPremium by ConfigurationManager.esPremium.collectAsState()

    // VM-States
    val uiState by mercadilloViewModel.uiState.collectAsState()
    val calendarioState by mercadilloViewModel.calendarioState.collectAsState()
    val mercadillosPorDia by mercadilloViewModel.mercadillosPorDia.collectAsState()

    val mostrarBottomBar by mercadilloViewModel.mostrarBottomBar.collectAsState()
    val mercadillosEnCurso by mercadilloViewModel.mercadillosEnCurso.collectAsState()
    val mercadilloActivoParaOperaciones by mercadilloViewModel.mercadilloActivoParaOperaciones.collectAsState()

    // Local UI
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var mostrarLeyenda by remember { mutableStateOf(false) }
    var mostrarDialogoSeleccionEdicion by remember { mutableStateOf(false) }
    var mercadillosParaSeleccionar by remember { mutableStateOf<List<MercadilloEntity>>(emptyList()) }

    var mostrarDialogoSeleccionActivo by remember { mutableStateOf(false) }
    var accionPendiente by remember { mutableStateOf<String?>(null) } // "ventas" | "gastos" | "resumen"

    var isRefreshing by remember { mutableStateOf(false) }

    // 🔒 [PUNTO 1] Bloque de aviso de usuario invitado temporalmente desactivado

    // Mensajes snackbar desde el VM
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            mercadilloViewModel.limpiarMensaje()
        }
    }
    // Ejecuta este efecto al entrar en composición o cuando cambian sus claves (uiState.error).
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            mercadilloViewModel.limpiarError()
        }
    }

    // 🔤 Locale derivado del idioma actual
    val localeForLang = remember(currentLanguage) { languageToLocale(currentLanguage) }

    // 🗓️ Mes localizado (no dependemos del Locale del sistema)
    val nombreMesLocalizado = remember(calendarioState.mes, calendarioState.ano, localeForLang) {
        val month = Month.of(calendarioState.mes.coerceIn(1, 12))
        val raw = month.getDisplayName(TextStyle.FULL_STANDALONE, localeForLang)
        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeForLang) else it.toString() } +
                " ${calendarioState.ano}"
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                // La actualización manual es común a ambas cuentas registradas.
                if (!es.nuskysoftware.marketsales.utils.DemoAccess.isDemo()) {
                    mercadilloViewModel.forzarSincronizacion()
                }
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                navController?.let { nav -> MenuHamburguesa(navController = nav, drawerState = drawerState) }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                StringResourceManager.getString("mercadillos", currentLanguage),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu),
                                    contentDescription = StringResourceManager.getString("menu", currentLanguage)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController?.navigate("alta_mercadillo") },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = StringResourceManager.getString("add_mercadillo", currentLanguage)
                        )
                    }
                },
                bottomBar = {
                    if (mostrarBottomBar) {
                        BottomBarMercadillo(
                            mercadilloActivo = mercadilloActivoParaOperaciones,
                            onVentasClick = {
                                val (ok, m) = mercadilloViewModel.manejarNavegacionVentas()
                                if (ok && m != null) {
                                    val ruta = "ventas/${m.idMercadillo}"
                                    // Navegación sin anuncios en las operaciones del mercadillo.
                                    navController?.navigate(ruta)
                                } else {
                                    accionPendiente = "ventas"; mostrarDialogoSeleccionActivo = true
                                }
                            },
                            onGastosClick = {
                                val (ok, m) = mercadilloViewModel.manejarNavegacionGastos()
                                if (ok && m != null) {
                                    val ruta = "gastos/${m.idMercadillo}"
                                    // Navegación sin anuncios en las operaciones del mercadillo.
                                    navController?.navigate(ruta)
                                } else {
                                    accionPendiente = "gastos"; mostrarDialogoSeleccionActivo = true
                                }
                            },
                            onResumenClick = {
                                val (ok, m) = mercadilloViewModel.manejarNavegacionResumen()
                                if (ok && m != null) {
                                    val ruta = "resumen/${m.idMercadillo}"
                                    // Navegación sin anuncios en las operaciones del mercadillo.
                                    navController?.navigate(ruta)
                                } else {
                                    accionPendiente = "resumen"; mostrarDialogoSeleccionActivo = true
                                }
                            },
                            onCambiarMercadillo = {
                                mercadilloViewModel.cambiarMercadilloActivo()
                                mostrarDialogoSeleccionActivo = true
                            },
                            currentLanguage = currentLanguage,
                            mostrarBotonCambiar = (mercadilloActivoParaOperaciones != null && mercadillosEnCurso.size > 1)
                        )
                    } else {
                        AdsBottomBar()
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {

                    DownloadProgressBar(
                        visible = syncState != SyncState.Idle && syncState != SyncState.Done && syncState !is SyncState.Error,
                        progressPercent = progress,
                        message = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // ==== CALENDARIO ====
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { mercadilloViewModel.navegarMesAnterior() }) {
                                    Text("←", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    text = nombreMesLocalizado,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 👉 Icono de leyenda dentro del calendario
                                    IconButton(onClick = { mostrarLeyenda = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = StringResourceManager.getString("ver_leyenda", currentLanguage)
                                        )
                                    }
                                    IconButton(onClick = { mercadilloViewModel.navegarMesSiguiente() }) {
                                        Text("→", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val diasSemana = listOf(
                                    StringResourceManager.getString("lunes", currentLanguage),
                                    StringResourceManager.getString("martes", currentLanguage),
                                    StringResourceManager.getString("miercoles", currentLanguage),
                                    StringResourceManager.getString("jueves", currentLanguage),
                                    StringResourceManager.getString("viernes", currentLanguage),
                                    StringResourceManager.getString("sabado", currentLanguage),
                                    StringResourceManager.getString("domingo", currentLanguage)
                                )
                                diasSemana.forEach { dia ->
                                    Text(
                                        text = dia,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            CalendarioGrid(
                                ano = calendarioState.ano,
                                mes = calendarioState.mes,
                                mercadillosPorDia = mercadillosPorDia,
                                onDiaClick = { dia ->
                                    val uid = ConfigurationManager.getCurrentUserId()
                                    val delDia = (mercadillosPorDia[dia] ?: emptyList()).filter { it.userId == uid }
                                    when (delDia.size) {
                                        0 -> {
                                            // Alta rápida solo Premium; la Demo conserva el gesto anterior.
                                            if (esPremium && ConfigurationManager.esPremiumReady.value) {
                                                val day = java.time.LocalDate.of(calendarioState.ano, calendarioState.mes, dia)
                                                val fecha = es.nuskysoftware.marketsales.utils.CalendarDates.display(day.toEpochDay())
                                                navController?.navigate("alta_mercadillo_fecha/${android.net.Uri.encode(fecha)}")
                                            } else if (!es.nuskysoftware.marketsales.utils.DemoAccess.isDemo()) {
                                                scope.launch { snackbarHostState.showSnackbar(StringResourceManager.getString("premium_calendar_create", currentLanguage)) }
                                            }
                                        }
                                        1 -> {
                                            val m = delDia.first()
                                            val estado = EstadosMercadillo.Estado.fromCodigo(m.estado)
                                            val ruta =
                                                if (estado == EstadosMercadillo.Estado.PENDIENTE_ARQUEO ||
                                                    estado == EstadosMercadillo.Estado.PENDIENTE_ASIGNAR_SALDO ||
                                                    estado == EstadosMercadillo.Estado.CERRADO_COMPLETO
                                                ) {
                                                    "arqueo/${m.idMercadillo}"
                                                } else {
                                                    "editar_mercadillo/${m.idMercadillo}"
                                                }
                                            navController?.navigate(ruta)
                                        }
                                        else -> {
                                            mercadillosParaSeleccionar = delDia // SOLO del usuario
                                            mostrarDialogoSeleccionEdicion = true
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 👉 Sección de próximos mercadillos (solo estados 1 y 2), con navegación
                    ProximosMercadillosSection(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            // Leyenda
            if (mostrarLeyenda) {
                LeyendaColoresDialog(
                    onDismiss = { mostrarLeyenda = false },
                    currentLanguage = currentLanguage
                )
            }

            // Diálogo selección para editar / arqueo según estado
            if (mostrarDialogoSeleccionEdicion) {
                DialogoSeleccionMercadillo(
                    mercadillos = mercadillosParaSeleccionar,
                    currentUserId = ConfigurationManager.getCurrentUserId(),
                    onMercadilloSeleccionado = { m ->
                        mostrarDialogoSeleccionEdicion = false
                        val estado = EstadosMercadillo.Estado.fromCodigo(m.estado)
                        val ruta =
                            if (estado == EstadosMercadillo.Estado.PENDIENTE_ARQUEO ||
                                estado == EstadosMercadillo.Estado.PENDIENTE_ASIGNAR_SALDO ||
                                estado == EstadosMercadillo.Estado.CERRADO_COMPLETO
                            ) {
                                "arqueo/${m.idMercadillo}"
                            } else {
                                "editar_mercadillo/${m.idMercadillo}"
                            }
                        navController?.navigate(ruta)
                    },
                    onDismiss = { mostrarDialogoSeleccionEdicion = false },
                    currentLanguage = currentLanguage
                )
            }

            // Diálogo de selección de mercadillo activo (ventas/gastos/resumen)
            if (mostrarDialogoSeleccionActivo) {
                DialogoSeleccionMercadilloActivo(
                    mercadillosEnCurso = mercadillosEnCurso,
                    onMercadilloSeleccionado = { m ->
                        mercadilloViewModel.seleccionarMercadilloActivo(m)
                        mostrarDialogoSeleccionActivo = false
                        when (accionPendiente) {
                            "ventas" -> navController?.navigate("ventas/${m.idMercadillo}")
                            "gastos" -> navController?.navigate("gastos/${m.idMercadillo}")
                            "resumen" -> navController?.navigate("resumen/${m.idMercadillo}")
                        }
                        accionPendiente = null
                    },
                    onDismiss = {
                        mostrarDialogoSeleccionActivo = false
                        accionPendiente = null
                    },
                    currentLanguage = currentLanguage
                )
            }
        }
    }
}

/** Mapea el código de idioma de ConfigurationManager.idioma a un Locale de Java. */
private fun languageToLocale(language: String): Locale = when (language.lowercase(Locale.ROOT)) {
    "es", "es_es" -> Locale("es", "ES")
    "en", "en_us", "en_gb" -> Locale.ENGLISH
    "gl" -> Locale("gl", "ES")
    "fr" -> Locale.FRENCH
    "pt" -> Locale("pt", "PT")
    else -> Locale("es", "ES") // por defecto ES
}
