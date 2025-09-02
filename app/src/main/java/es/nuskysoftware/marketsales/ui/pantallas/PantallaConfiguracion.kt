// PantallaConfiguracion.kt V10 - SIMPLIFICADO PARA SISTEMA MONOUSUARIO
package es.nuskysoftware.marketsales.ui.pantallas

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.data.repository.AuthRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.FooterMarca
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.safePopBackStack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    // Estados V10 simplificados
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val currentFont by ConfigurationManager.fuente.collectAsState()
    val isDarkTheme by ConfigurationManager.temaOscuro.collectAsState()
    val currentMoneda by ConfigurationManager.moneda.collectAsState()
    val esPremium by ConfigurationManager.esPremium.collectAsState()
    val usuarioEmail by ConfigurationManager.usuarioEmail.collectAsState()
    val isAuthenticated by ConfigurationManager.isAuthenticated.collectAsState()
    val appVersionText = ConfigurationManager.versionAppValue

    // Permisos V10 simplificados
    val canChangeAdvanced = esPremium // En lugar de ConfigurationManager.canChangeConfiguration()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringResourceManager.getString("configuracion", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.safePopBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información de cuenta V10
                item {
                    TarjetaInformacionCuenta(
                        email = usuarioEmail ?: if (isAuthenticated) "usuario@email.com" else StringResourceManager.getString("usuario_invitado", currentLanguage),
                        esPremium = esPremium,
                        isAuthenticated = isAuthenticated,
                        version = appVersionText,
                        language = currentLanguage
                    )
                }

                // Idioma (solo Premium)
                item {
                    OpcionConfiguracionV10(
                        iconRes = R.drawable.ic_language,
                        titulo = StringResourceManager.getString("idioma", currentLanguage),
                        valorActual = if (currentLanguage == "es")
                            StringResourceManager.getString("espanol", currentLanguage)
                        else
                            StringResourceManager.getString("ingles", currentLanguage),
                        opciones = listOf(
                            "es" to StringResourceManager.getString("espanol", currentLanguage),
                            "en" to StringResourceManager.getString("ingles", currentLanguage)
                        ),
                        habilitado = canChangeAdvanced,
                        onSeleccionar = { codigo ->
                            scope.launch {
                                authRepo.updateConfiguration(idioma = codigo)
                            }
                        }
                    )
                }

                // Fuente (solo Premium)
                item {
                    OpcionConfiguracionV10(
                        iconRes = R.drawable.ic_font,
                        titulo = StringResourceManager.getString("fuente", currentLanguage),
                        valorActual = currentFont,
                        opciones = listOf(
                            "Montserrat" to StringResourceManager.getString("montserrat", currentLanguage),
                            "Poppins" to StringResourceManager.getString("poppins", currentLanguage),
                            "Roboto" to StringResourceManager.getString("roboto", currentLanguage)
                        ),
                        habilitado = canChangeAdvanced,
                        onSeleccionar = { fuente ->
                            scope.launch {
                                authRepo.updateConfiguration(fuente = fuente)
                            }
                        }
                    )
                }

                // Tema Oscuro (todos pueden cambiar)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_palette),
                                contentDescription = StringResourceManager.getString("tema", currentLanguage),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = StringResourceManager.getString("tema", currentLanguage),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isDarkTheme)
                                        StringResourceManager.getString("tema_oscuro", currentLanguage)
                                    else
                                        StringResourceManager.getString("tema_claro", currentLanguage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { nuevoValor ->
                                    scope.launch {
                                        authRepo.updateConfiguration(temaOscuro = nuevoValor)
                                    }
                                }
                            )
                        }
                    }
                }

                // Moneda (solo Premium)
                item {
                    val monedas = listOf(
                        "€ Euro" to StringResourceManager.getString("euro", currentLanguage),
                        "$ Dólar" to StringResourceManager.getString("dolar", currentLanguage),
                        "£ Libra" to StringResourceManager.getString("libra", currentLanguage),
                        "$ Peso Argentino" to StringResourceManager.getString("peso_argentino", currentLanguage),
                        "$ Peso Mexicano" to StringResourceManager.getString("peso_mexicano", currentLanguage),
                        "$ Peso Colombiano" to StringResourceManager.getString("peso_colombiano", currentLanguage),
                        "S/ Sol Peruano" to StringResourceManager.getString("sol_peruano", currentLanguage),
                        "$ Peso Chileno" to StringResourceManager.getString("peso_chileno", currentLanguage),
                        "Bs Bolívar" to StringResourceManager.getString("bolivar", currentLanguage),
                        "$ Real Brasileño" to StringResourceManager.getString("real_brasileno", currentLanguage)
                    )

                    OpcionConfiguracionV10(
                        iconRes = R.drawable.ic_money,
                        titulo = StringResourceManager.getString("moneda", currentLanguage),
                        valorActual = currentMoneda,
                        opciones = monedas,
                        habilitado = canChangeAdvanced,
                        onSeleccionar = { moneda ->
                            scope.launch {
                                authRepo.updateConfiguration(moneda = moneda)
                            }
                        }
                    )
                }

                // Promoción Premium para usuarios FREE
                if (!esPremium) {
                    item {
                        TarjetaPromocionPremium(language = currentLanguage)
                    }
                }
            }
            AdsBottomBar()

            // Footer
            FooterMarca()
        }
    }
    // 🔍 LOG TEMPORAL para debugging
    LaunchedEffect(esPremium, canChangeAdvanced) {
        Log.d("PantallaConfiguracion", "🔍 Estado actual:")
        Log.d("PantallaConfiguracion", "   - esPremium: $esPremium")
        Log.d("PantallaConfiguracion", "   - canChangeAdvanced: $canChangeAdvanced")
        Log.d("PantallaConfiguracion", "   - isAuthenticated: $isAuthenticated")
    }
}

@Composable
private fun OpcionConfiguracionV10(
    iconRes: Int,
    titulo: String,
    valorActual: String,
    opciones: List<Pair<String, String>>,
    habilitado: Boolean,
    onSeleccionar: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (habilitado)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(if (habilitado) 4.dp else 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = habilitado) {
                        if (habilitado) expanded = !expanded
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = titulo,
                    tint = if (habilitado)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (habilitado)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = valorActual,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (habilitado)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    if (!habilitado) {
                        Text(
                            text = StringResourceManager.getString("solo_premium", currentLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
                if (habilitado) {
                    Icon(
                        painter = painterResource(
                            id = if (expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                        ),
                        contentDescription = StringResourceManager.getString("expandir", currentLanguage),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (expanded && habilitado) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    opciones.forEach { (codigo, nombre) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSeleccionar(codigo)
                                    expanded = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = valorActual == codigo,
                                onClick = {
                                    onSeleccionar(codigo)
                                    expanded = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(nombre)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaInformacionCuenta(
    email: String,
    esPremium: Boolean,
    isAuthenticated: Boolean,
    version: String,
    language: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringResourceManager.getString("informacion_cuenta", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            !isAuthenticated -> Color(0xFFCCCCCC)
                            esPremium -> Color(0xFFFFD700)
                            else -> Color(0xFF90EE90)
                        }
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = when {
                            !isAuthenticated -> StringResourceManager.getString("version_invitado", language)
                            esPremium -> StringResourceManager.getString("version_premium", language)
                            else -> StringResourceManager.getString("version_free", language)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = StringResourceManager
                    .getString("app_version", language)
                    .replace("{version}", version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TarjetaPromocionPremium(language: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = StringResourceManager.getString("premium", language),
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = StringResourceManager.getString("promocion_premium", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = StringResourceManager.getString("desbloquea_funciones", language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
