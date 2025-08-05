// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaArticulos.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.local.entity.ArticuloEntity
import es.nuskysoftware.marketsales.ui.viewmodel.ArticuloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ArticuloViewModelFactory
import es.nuskysoftware.marketsales.ui.viewmodel.CategoriaViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.CategoriaViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.FooterMarca
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.generarColorAleatorioPastel
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.SolidColor
import es.nuskysoftware.cajamercadillos.ui.components.DialogSelectorColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaArticulos(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModels con factory
    val articuloViewModel: ArticuloViewModel = viewModel(factory = ArticuloViewModelFactory(context))
    val categoriaViewModel: CategoriaViewModel = viewModel(factory = CategoriaViewModelFactory(context))

    // Estados de configuración
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val esPremium by ConfigurationManager.esPremium.collectAsState()

    // Estados de los ViewModels
    val articulos by articuloViewModel.articulos.collectAsState()
    val categorias by categoriaViewModel.categorias.collectAsState()
    val articuloUiState by articuloViewModel.uiState.collectAsState()
    val tieneArticulos by articuloViewModel.tieneArticulos.collectAsState()

    // ======= Estados locales del formulario =======
    var mostrarFormulario by remember { mutableStateOf(false) }
    var nombreArticulo by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("") } // idCategoria
    var precioVenta by remember { mutableStateOf("") }
    var precioCoste by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var controlarStock by remember { mutableStateOf(false) }
    var controlarCoste by remember { mutableStateOf(false) }
    var favorito by remember { mutableStateOf(false) }
    var modoEdicion by remember { mutableStateOf<ArticuloEntity?>(null) }

    // Dropdown categoría
    var expandedCategoria by remember { mutableStateOf(false) }

    // Nueva categoría (inline, no modal)
    var mostrarNuevaCategoriaInline by remember { mutableStateOf(false) }
    var nombreNuevaCategoria by remember { mutableStateOf("") }
    var colorNuevaCategoria by remember { mutableStateOf(Color(0xFFD1C4E9)) }
    var mostrarColorPicker by remember { mutableStateOf(false) }
    var pendingSeleccionarCategoriaPorNombre by remember { mutableStateOf<String?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }


    // Helpers
    fun limpiarFormulario() {
        nombreArticulo = ""
        categoriaSeleccionada = ""
        precioVenta = ""
        precioCoste = ""
        stock = ""
        controlarStock = false
        controlarCoste = false
        favorito = false
        modoEdicion = null
        // al cerrar form, también ocultamos la creación inline
        mostrarNuevaCategoriaInline = false
        nombreNuevaCategoria = ""
        colorNuevaCategoria = Color(0xFFD1C4E9)
    }

    // Manejar mensajes VM
    LaunchedEffect(articuloUiState.message) {
        articuloUiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            articuloViewModel.limpiarMensaje()
        }
    }
    LaunchedEffect(articuloUiState.error) {
        articuloUiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            articuloViewModel.limpiarError()
        }
    }

    // Cuando acabamos de crear una categoría, selecciónala en el desplegable (por nombre)
    LaunchedEffect(categorias, pendingSeleccionarCategoriaPorNombre) {
        val nombre = pendingSeleccionarCategoriaPorNombre ?: return@LaunchedEffect
        categorias.firstOrNull { it.nombre == nombre }?.let { nueva ->
            categoriaSeleccionada = nueva.idCategoria
            pendingSeleccionarCategoriaPorNombre = null
            mostrarNuevaCategoriaInline = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringResourceManager.getString("articulos", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (mostrarFormulario) {
                            // Cierra el formulario en lugar de salir a otra pantalla
                            limpiarFormulario()
                            mostrarFormulario = false
                        } else {
                            navController?.popBackStack()
                        }
                    }) {
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
                        limpiarFormulario()
                        mostrarFormulario = true
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add,
                        contentDescription = StringResourceManager.getString("add_articulo", currentLanguage))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ===== FORMULARIO (fondo BLANCO) =====
            AnimatedVisibility(visible = mostrarFormulario) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,            // << Fondo BLANCO
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (modoEdicion == null)
                                StringResourceManager.getString("nuevo_articulo", currentLanguage)
                            else
                                StringResourceManager.getString("editar_articulo", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Nombre
                        OutlinedTextField(
                            value = nombreArticulo,
                            onValueChange = { nombreArticulo = it },
                            label = { Text(StringResourceManager.getString("nombre", currentLanguage)) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = nombreArticulo.isNotEmpty() &&
                                    articuloViewModel.validarNombreArticulo(nombreArticulo) != null,
                            supportingText = {
                                articuloViewModel.validarNombreArticulo(nombreArticulo)?.let { error ->
                                    Text(text = error, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true
                        )

//                        // ====== DESPLEGABLE DE CATEGORÍA (con “Crear categoría” primero) ======
//                        var textoCategoria by remember(categorias, categoriaSeleccionada) {
//                            mutableStateOf(
//                                categorias.firstOrNull { it.idCategoria == categoriaSeleccionada }?.nombre ?: ""
//                            )
//                        }
                        // ====== DESPLEGABLE DE CATEGORÍA (con “Crear categoría” primero) ======
                        var textoCategoria by remember(categorias, categoriaSeleccionada) {
                            mutableStateOf(
                                categorias.firstOrNull { it.idCategoria == categoriaSeleccionada }?.nombre ?: ""
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedCategoria,
                            onExpandedChange = { expandedCategoria = !expandedCategoria }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                readOnly = true,
                                value = textoCategoria,
                                onValueChange = { /* readOnly */ },
                                label = { Text(StringResourceManager.getString("categoria", currentLanguage), color = Color.Black) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria)
                                },
                                // Forzamos texto negro sin utilizar outlinedTextFieldColors (compat amplio)
                                textStyle = LocalTextStyle.current.copy(color = Color.Black)
                            )

                            ExposedDropdownMenu(
                                expanded = expandedCategoria,
                                onDismissRequest = { expandedCategoria = false }
                            ) {
                                // Opción “Crear categoría”
                                DropdownMenuItem(
                                    text = { Text(StringResourceManager.getString("crear_nueva_categoria", currentLanguage)) },
                                    onClick = {
                                        expandedCategoria = false
                                        mostrarNuevaCategoriaInline = true
                                    }
                                )
                                // Categorías existentes
                                categorias.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.nombre) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(cat.colorHex)))
                                            )
                                        },
                                        onClick = {
                                            categoriaSeleccionada = cat.idCategoria
                                            textoCategoria = cat.nombre
                                            mostrarNuevaCategoriaInline = false
                                            expandedCategoria = false
                                        }
                                    )
                                }
                            }
                        }


//                        ExposedDropdownMenuBox(
//                            expanded = expandedCategoria,
//                            onExpandedChange = { expandedCategoria = !expandedCategoria }
//                        ) {
//                            OutlinedTextField(
//                                modifier = Modifier
//                                    .menuAnchor()
//                                    .fillMaxWidth(),
//                                readOnly = true,
//                                value = textoCategoria,
//                                onValueChange = { /* readOnly */ },
//                                label = { Text(StringResourceManager.getString("categoria", currentLanguage), color = Color.Black) },
//                                trailingIcon = {
//                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria)
//                                },
//                                colors = TextFieldDefaults.outlinedTextFieldColors(
//                                    textColor = Color.Black,
//                                    focusedLabelColor = Color.Black,
//                                    unfocusedLabelColor = Color.Black
//                                )
//                            )
//
//                            ExposedDropdownMenu(
//                                expanded = expandedCategoria,
//                                onDismissRequest = { expandedCategoria = false }
//                            ) {
//                                // Opción “Crear categoría”
//                                DropdownMenuItem(
//                                    text = { Text(StringResourceManager.getString("crear_nueva_categoria", currentLanguage)) },
//                                    onClick = {
//                                        expandedCategoria = false
//                                        mostrarNuevaCategoriaInline = true
//                                    }
//                                )
//                                // Categorías existentes
//                                categorias.forEach { cat ->
//                                    DropdownMenuItem(
//                                        text = { Text(cat.nombre) },
//                                        leadingIcon = {
//                                            Box(
//                                                modifier = Modifier
//                                                    .size(14.dp)
//                                                    .clip(CircleShape)
//                                                    .background(Color(android.graphics.Color.parseColor(cat.colorHex)))
//                                            )
//                                        },
//                                        onClick = {
//                                            categoriaSeleccionada = cat.idCategoria
//                                            textoCategoria = cat.nombre
//                                            mostrarNuevaCategoriaInline = false
//                                            expandedCategoria = false
//                                        }
//                                    )
//                                }
//                            }
//                        }

                        // ====== FORMULARIO NUEVA CATEGORÍA (INLINE + SCROLLABLE) ======
                        AnimatedVisibility(visible = mostrarNuevaCategoriaInline) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = StringResourceManager.getString("nueva_categoria", currentLanguage),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    OutlinedTextField(
                                        value = nombreNuevaCategoria,
                                        onValueChange = { nombreNuevaCategoria = it },
                                        label = { Text(StringResourceManager.getString("nombre", currentLanguage)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    // Muestra previa + botón selector color
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = StringResourceManager.getString("seleccionar_color", currentLanguage),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(colorNuevaCategoria)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        OutlinedButton(onClick = { mostrarColorPicker = true }) {
                                            Text(StringResourceManager.getString("cambiar", currentLanguage))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            enabled = nombreNuevaCategoria.isNotBlank(),
                                            onClick = {
                                                scope.launch {
                                                    // Si el usuario no cambió color, asigna uno pastel aleatorio
                                                    val colorFinal = if (colorNuevaCategoria == Color(0xFFD1C4E9))
                                                        generarColorAleatorioPastel()
                                                    else colorNuevaCategoria
                                                    val colorHex = String.format("#%06X", 0xFFFFFF and colorFinal.toArgb())

                                                    // Crea categoría y marca que debemos seleccionarla cuando aparezca en el flow
                                                    pendingSeleccionarCategoriaPorNombre = nombreNuevaCategoria
                                                    categoriaViewModel.crearCategoria(nombreNuevaCategoria, colorHex)

                                                    // Reseteo nombre (la selección real llega por LaunchedEffect)
                                                    nombreNuevaCategoria = ""
                                                }
                                            }
                                        ) { Text(StringResourceManager.getString("crear", currentLanguage)) }

                                        OutlinedButton(onClick = {
                                            mostrarNuevaCategoriaInline = false
                                            nombreNuevaCategoria = ""
                                            colorNuevaCategoria = Color(0xFFD1C4E9)
                                        }) { Text(StringResourceManager.getString("cancelar", currentLanguage)) }
                                    }
                                }
                            }
                        }

                        // Precio venta
                        OutlinedTextField(
                            value = precioVenta,
                            onValueChange = { precioVenta = it },
                            label = { Text(StringResourceManager.getString("precio_venta", currentLanguage)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = precioVenta.isNotEmpty() && precioVenta.toDoubleOrNull()?.let {
                                articuloViewModel.validarPrecioVenta(it)
                            } != null,
                            supportingText = {
                                precioVenta.toDoubleOrNull()?.let { precio ->
                                    articuloViewModel.validarPrecioVenta(precio)?.let { error ->
                                        Text(text = error, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            singleLine = true
                        )

                        // Control coste
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = controlarCoste,
                                onCheckedChange = { if (esPremium) controlarCoste = it },
                                enabled = esPremium
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = StringResourceManager.getString("controlar_coste", currentLanguage),
                                    color = if (esPremium) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                if (!esPremium) {
                                    Text(
                                        text = StringResourceManager.getString("solo_premium_coste", currentLanguage),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        if (controlarCoste && esPremium) {
                            OutlinedTextField(
                                value = precioCoste,
                                onValueChange = { precioCoste = it },
                                label = { Text(StringResourceManager.getString("precio_coste", currentLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }

                        // Control stock
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = controlarStock,
                                onCheckedChange = { if (esPremium) controlarStock = it },
                                enabled = esPremium
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = StringResourceManager.getString("controlar_stock", currentLanguage),
                                    color = if (esPremium) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                if (!esPremium) {
                                    Text(
                                        text = StringResourceManager.getString("solo_premium_stock", currentLanguage),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        if (controlarStock && esPremium) {
                            OutlinedTextField(
                                value = stock,
                                onValueChange = { stock = it },
                                label = { Text(StringResourceManager.getString("stock", currentLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        // Favorito
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = favorito, onCheckedChange = { favorito = it })
                            Text(
                                text = StringResourceManager.getString("marcar_favorito", currentLanguage),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Botones Guardar/Cancelar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val validacionNombre =
                                            articuloViewModel.validarNombreArticulo(nombreArticulo)
                                        if (validacionNombre != null) {
                                            snackbarHostState.showSnackbar(validacionNombre); return@launch
                                        }
                                        val precio = precioVenta.toDoubleOrNull()
                                        if (precio == null) {
                                            snackbarHostState.showSnackbar("Precio de venta inválido"); return@launch
                                        }
                                        if (categoriaSeleccionada.isEmpty()) {
                                            snackbarHostState.showSnackbar("Selecciona una categoría"); return@launch
                                        }

                                        if (modoEdicion == null) {
                                            // Crear
                                            articuloViewModel.crearArticulo(
                                                nombre = nombreArticulo,
                                                idCategoria = categoriaSeleccionada,
                                                precioVenta = precio,
                                                precioCoste = if (controlarCoste && esPremium && precioCoste.isNotEmpty())
                                                    precioCoste.toDoubleOrNull() else null,
                                                stock = if (controlarStock && esPremium && stock.isNotEmpty())
                                                    stock.toIntOrNull() else null,
                                                controlarStock = controlarStock && esPremium,
                                                controlarCoste = controlarCoste && esPremium,
                                                favorito = favorito
                                            )
                                        } else {
                                            // Actualizar
                                            val actualizado = modoEdicion!!.copy(
                                                nombre = nombreArticulo,
                                                idCategoria = categoriaSeleccionada,
                                                precioVenta = precio,
                                                precioCoste = if (controlarCoste && esPremium && precioCoste.isNotEmpty())
                                                    precioCoste.toDoubleOrNull() else null,
                                                stock = if (controlarStock && esPremium && stock.isNotEmpty())
                                                    stock.toIntOrNull() else null,
                                                controlarStock = controlarStock && esPremium,
                                                controlarCoste = controlarCoste && esPremium,
                                                favorito = favorito
                                            )
                                            articuloViewModel.actualizarArticulo(actualizado)
                                        }

                                        limpiarFormulario()
                                        mostrarFormulario = false
                                    }
                                },
                                enabled = !articuloUiState.loading && nombreArticulo.isNotBlank() && precioVenta.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (articuloUiState.loading) {
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
                                    limpiarFormulario()
                                    mostrarFormulario = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(StringResourceManager.getString("cancelar", currentLanguage)) }
                        }
                    }
                }
            }

            // ===== LISTA =====
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (!tieneArticulos) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_list),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = StringResourceManager.getString("pulsa_crear_primer_articulo", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(articulos) { articulo ->
                            ArticuloCard(
                                articulo = articulo,
                                categorias = categorias,
                                esPremium = esPremium,
                                onEditar = {
                                    modoEdicion = articulo
                                    nombreArticulo = articulo.nombre
                                    categoriaSeleccionada = articulo.idCategoria
                                    precioVenta = articulo.precioVenta.toString()
                                    precioCoste = articulo.precioCoste?.toString() ?: ""
                                    stock = articulo.stock?.toString() ?: ""
                                    controlarStock = articulo.controlarStock
                                    controlarCoste = articulo.controlarCoste
                                    favorito = articulo.favorito
                                    mostrarFormulario = true
                                    mostrarNuevaCategoriaInline = false
                                },
                                onEliminar = {
                                    // Confirmación simple inline (puedes mantener tu diálogo si lo prefieres)
                                    scope.launch {
                                        articuloViewModel.eliminarArticulo(articulo)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            FooterMarca()
        }
    }

    // ===== Selector de color (con muestra previa ya visible en el formulario) =====
    if (mostrarColorPicker) {
        DialogSelectorColor(
            onColorElegido = { c ->
                colorNuevaCategoria = c
                mostrarColorPicker = false
            },
            onCancelar = { mostrarColorPicker = false }
        )
    }
}

@Composable
private fun ArticuloCard(
    articulo: ArticuloEntity,
    categorias: List<es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity>,
    esPremium: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val categoria = categorias.find { it.idCategoria == articulo.idCategoria }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = articulo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = categoria?.nombre ?: "Sin categoría",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Precio: ${articulo.precioVenta} €",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (esPremium) {
                    if (articulo.controlarStock && articulo.stock != null) {
                        Text(
                            text = "Stock: ${articulo.stock}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (articulo.controlarCoste && articulo.precioCoste != null) {
                        Text(
                            text = "Coste: ${articulo.precioCoste} €",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (articulo.favorito) {
                    Text(
                        text = "⭐ Favorito",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEditar) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}





