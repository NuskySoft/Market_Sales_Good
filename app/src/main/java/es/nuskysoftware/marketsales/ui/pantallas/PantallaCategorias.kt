// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaCategorias.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import es.nuskysoftware.marketsales.ui.viewmodel.CategoriaViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.CategoriaViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.FooterMarca
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.generarColorAleatorioPastel
import es.nuskysoftware.marketsales.utils.safePopBackStack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCategorias(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel con factory
    val categoriaViewModel: CategoriaViewModel = viewModel(
        factory = CategoriaViewModelFactory(context)
    )

    // Estados de configuración
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    // Estados del ViewModel
    val categorias by categoriaViewModel.categorias.collectAsState()
    val uiState by categoriaViewModel.uiState.collectAsState()
    val tieneCategorias by categoriaViewModel.tieneCategorias.collectAsState()

    // Estados locales
    var mostrarFormulario by remember { mutableStateOf(false) }
    var nombreCategoria by remember { mutableStateOf("") }
    var colorSeleccionado by remember { mutableStateOf(Color(0xFFD1C4E9)) }
    var modoEdicion by remember { mutableStateOf<CategoriaEntity?>(null) }

    var mostrarColorPicker by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var categoriaAEliminar by remember { mutableStateOf<CategoriaEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Manejar mensajes del ViewModel
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            categoriaViewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            categoriaViewModel.limpiarError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringResourceManager.getString("categorias", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.safePopBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!mostrarFormulario) {
                FloatingActionButton(
                    onClick = {
                        mostrarFormulario = true
                        nombreCategoria = ""
                        colorSeleccionado = Color(0xFFD1C4E9)
                        modoEdicion = null
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = StringResourceManager.getString("add_categoria", currentLanguage)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ========== FORMULARIO DESLIZANTE ==========
            AnimatedVisibility(visible = mostrarFormulario) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = if (modoEdicion == null)
                                StringResourceManager.getString("nueva_categoria", currentLanguage)
                            else
                                StringResourceManager.getString("editar_categoria", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo nombre
                        OutlinedTextField(
                            value = nombreCategoria,
                            onValueChange = { nombreCategoria = it },
                            label = { Text(StringResourceManager.getString("nombre", currentLanguage)) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = nombreCategoria.isNotEmpty() && categoriaViewModel.validarNombreCategoria(nombreCategoria) != null,
                            supportingText = {
                                categoriaViewModel.validarNombreCategoria(nombreCategoria)?.let { error ->
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selector de color
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mostrarColorPicker = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = StringResourceManager.getString("seleccionar_color", currentLanguage),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colorSeleccionado)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Botones
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val validacionNombre = categoriaViewModel.validarNombreCategoria(nombreCategoria)
                                        if (validacionNombre != null) {
                                            snackbarHostState.showSnackbar(validacionNombre)
                                            return@launch
                                        }

                                        val colorFinal = if (colorSeleccionado == Color(0xFFD1C4E9)) {
                                            generarColorAleatorioPastel()
                                        } else colorSeleccionado

                                        val colorHex = String.format("#%06X", 0xFFFFFF and colorFinal.toArgb())

                                        if (modoEdicion == null) {
                                            categoriaViewModel.crearCategoria(nombreCategoria, colorHex)
                                        } else {
                                            val categoriaEditada = modoEdicion!!.copy(
                                                nombre = nombreCategoria,
                                                colorHex = colorHex
                                            )
                                            categoriaViewModel.actualizarCategoria(categoriaEditada)
                                        }

                                        // Limpiar formulario
                                        nombreCategoria = ""
                                        colorSeleccionado = Color(0xFFD1C4E9)
                                        modoEdicion = null
                                        mostrarFormulario = false
                                    }
                                },
                                enabled = !uiState.loading && nombreCategoria.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(StringResourceManager.getString("guardar", currentLanguage))
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    mostrarFormulario = false
                                    nombreCategoria = ""
                                    colorSeleccionado = Color(0xFFD1C4E9)
                                    modoEdicion = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(StringResourceManager.getString("cancelar", currentLanguage))
                            }
                        }
                    }
                }
            }

            // ========== CONTENIDO PRINCIPAL ==========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (!tieneCategorias) {
                    // Estado vacío
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_category),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = StringResourceManager.getString("pulsa_crear_primera_categoria", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Lista de categorías en Cards
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(categorias) { categoria ->
                            CategoriaCard(
                                categoria = categoria,
                                onEditar = {
                                    mostrarFormulario = true
                                    nombreCategoria = categoria.nombre
                                    colorSeleccionado = Color(android.graphics.Color.parseColor(categoria.colorHex))
                                    modoEdicion = categoria
                                },
                                onEliminar = {
                                    categoriaAEliminar = categoria
                                    mostrarDialogoEliminar = true
                                }
                            )
                        }
                    }
                }
            }
            AdsBottomBar()

            // ========== FOOTER ==========
            FooterMarca()
        }
    }

    // ========== DIÁLOGOS ==========

    // Selector de color
    if (mostrarColorPicker) {
        ColorPickerDialog(
            onColorElegido = { color ->
                colorSeleccionado = color
                mostrarColorPicker = false
            },
            onCancelar = { mostrarColorPicker = false }
        )
    }

    // Confirmación eliminar
    if (mostrarDialogoEliminar && categoriaAEliminar != null) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoEliminar = false
                categoriaAEliminar = null
            },
            title = {
                Text(StringResourceManager.getString("eliminar_categoria", currentLanguage))
            },
            text = {
                Text(
                    StringResourceManager.getString("confirmar_eliminar_categoria", currentLanguage)
                        .replace("{nombre}", categoriaAEliminar!!.nombre)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoriaViewModel.eliminarCategoria(categoriaAEliminar!!)
                        mostrarDialogoEliminar = false
                        categoriaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        StringResourceManager.getString("eliminar", currentLanguage),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        mostrarDialogoEliminar = false
                        categoriaAEliminar = null
                    }
                ) {
                    Text(StringResourceManager.getString("cancelar", currentLanguage))
                }
            }
        )
    }
}

@Composable
private fun CategoriaCard(
    categoria: CategoriaEntity,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Círculo de color
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(categoria.colorHex)))
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Nombre
                Text(
                    text = categoria.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Botones de acción
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEditar) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = StringResourceManager.getString("editar", currentLanguage),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onEliminar) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = StringResourceManager.getString("eliminar", currentLanguage),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    onColorElegido: (Color) -> Unit,
    onCancelar: () -> Unit
) {
    val controller = rememberColorPickerController()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(StringResourceManager.getString("seleccionar_un_color", currentLanguage)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Selector principal
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp),
                    controller = controller,
                    onColorChanged = {}
                )

                // Control de transparencia
                AlphaSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    controller = controller
                )

                // Control de brillo
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    controller = controller
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorElegido(controller.selectedColor.value)
            }) {
                Text(
                    StringResourceManager.getString("aceptar", currentLanguage),
                    color = Color.Black // ✅ NEGRO como solicitaste
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(
                    StringResourceManager.getString("cancelar", currentLanguage),
                    color = Color.Black // ✅ NEGRO como solicitaste
                )
            }
        }
    )
}
