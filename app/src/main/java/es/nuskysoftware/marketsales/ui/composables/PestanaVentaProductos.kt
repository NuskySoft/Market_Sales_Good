// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/PestanaVentaProductos.kt
package es.nuskysoftware.marketsales.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.ArticuloEntity
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.TipoLinea
import es.nuskysoftware.marketsales.ui.components.ventas.BarraAccionesVenta
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun PestanaVentaProductos(
    ventasViewModel: VentasViewModel,
    navController: NavController,
    mercadilloActivo: MercadilloEntity,
    onRealizarCargo: (totalFormateado: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val articuloDao = remember { db.articuloDao() }
    val categoriaDao = remember { db.categoriaDao() }

    val userId by ConfigurationManager.usuarioLogueado.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()
    val esPremium by ConfigurationManager.esPremium.collectAsState()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    if (!esPremium) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(StringResourceManager.getString("funcion_premium", currentLanguage))
        }
        return
    }

    var categoriaSeleccionadaId by remember { mutableStateOf<String?>(null) }
    var mostrandoBusqueda by remember { mutableStateOf(false) }

    // Categorías
    val categoriasFlow: Flow<List<CategoriaEntity>> = remember(userId) {
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else categoriaDao.getCategoriasByUser(userId!!)
    }
    val categorias by categoriasFlow.collectAsState(initial = emptyList())

    val colorPorCategoria = remember(categorias) {
        categorias.associate { it.idCategoria to parseHexColor(it.colorHex) }
    }

    // Artículos (filtra por categoría en el propio DAO para no cargar de más)
    val articulosFlow: Flow<List<ArticuloEntity>> = remember(userId, categoriaSeleccionadaId) {
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else if (categoriaSeleccionadaId == null)
            articuloDao.getArticulosByUser(userId!!)
        else
            articuloDao.getArticulosByUserAndCategoria(userId!!, categoriaSeleccionadaId!!)
    }
    val articulos by articulosFlow.collectAsState(initial = emptyList())

    val uiState by ventasViewModel.uiState.collectAsState()

    // --- Filtro de búsqueda en memoria sobre lo que ya tenemos cargado ---
    val termino = uiState.terminoBusqueda.trim()
    val articulosFiltrados = remember(articulos, termino) {
        if (termino.isEmpty()) articulos
        else articulos.filter { it.nombre.contains(termino, ignoreCase = true) }
    }

    // --- Ordenación: sin categoría => favoritos primero y luego alfabético; con categoría => alfabético puro ---
    val articulosOrdenados = remember(articulosFiltrados, categoriaSeleccionadaId) {
        if (categoriaSeleccionadaId == null) {
            articulosFiltrados.sortedWith(
                compareByDescending<ArticuloEntity> { it.favorito }
                    .thenBy { it.nombre.lowercase() }
            )
        } else {
            articulosFiltrados.sortedBy { it.nombre.lowercase() }
        }
    }

    val totalFmt = MonedaUtils.formatearImporte(uiState.totalTicket, moneda)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        // ===== Título + Lupa / Buscador =====
        if (!mostrandoBusqueda) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringResourceManager.getString("todos_los_productos", currentLanguage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { mostrandoBusqueda = true }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = StringResourceManager.getString("buscar", currentLanguage)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    mostrandoBusqueda = false
                    ventasViewModel.actualizarTerminoBusqueda("")
                }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = StringResourceManager.getString("cerrar_busqueda", currentLanguage)
                    )
                }
                TextField(
                    value = uiState.terminoBusqueda,
                    onValueChange = { ventasViewModel.actualizarTerminoBusqueda(it) },
                    placeholder = { Text(StringResourceManager.getString("buscar_placeholder", currentLanguage)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                )
            }
        }

        // ===== LazyRow de categorías (scroll horizontal) =====
        if (categorias.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                // Chip "Todas"
                item {
                    CategoriaChip(
                        label = StringResourceManager.getString("todas", currentLanguage),
                        selected = categoriaSeleccionadaId == null,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { categoriaSeleccionadaId = null }
                    )
                }
                // Resto de categorías
                items(categorias, key = { it.idCategoria }) { cat ->
                    CategoriaChip(
                        label = cat.nombre,
                        selected = categoriaSeleccionadaId == cat.idCategoria,
                        color = colorPorCategoria[cat.idCategoria] ?: MaterialTheme.colorScheme.primary,
                        onClick = { categoriaSeleccionadaId = cat.idCategoria }
                    )
                }
            }
            HorizontalDivider()
        }

        // ===== Grid de productos =====
        if (articulosOrdenados.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (userId.isNullOrBlank())
                        StringResourceManager.getString("inicia_sesion_ver_productos", currentLanguage)
                    else
                        StringResourceManager.getString("no_hay_productos", currentLanguage),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(articulosOrdenados, key = { it.idArticulo }) { art ->
                    val cantidadEnCarrito = remember(uiState.lineasTicket) {
                        uiState.lineasTicket
                            .filter { it.tipoLinea == TipoLinea.PRODUCTO && it.idProducto == art.idArticulo }
                            .sumOf { it.cantidad }
                    }

                    ProductoCardCuadrada(
                        articulo = art,
                        colorCategoria = colorPorCategoria[art.idCategoria] ?: MaterialTheme.colorScheme.primary,
                        precioFormateado = MonedaUtils.formatearImporte(art.precioVenta, moneda),
                        cantidadCarrito = cantidadEnCarrito,
                        onClick = {
                            ventasViewModel.añadirProducto(
                                idProducto = art.idArticulo,
                                descripcion = art.nombre,
                                precio = art.precioVenta
                            )
                        }
                    )
                }
            }
        }

        // ===== Franja inferior (acciones) =====
        BarraAccionesVenta(
            ventasViewModel = ventasViewModel,
            totalFormateado = totalFmt,
            enabledRealizarCargo = uiState.lineasTicket.isNotEmpty() && uiState.totalTicket > 0,
            onRealizarCargo = { if (uiState.totalTicket > 0) onRealizarCargo(totalFmt) },
            onAbrirCarrito = { navController.navigate("carrito/${mercadilloActivo.idMercadillo}") }
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CategoriaChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val fg = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .background(bg, MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ProductoCardCuadrada(
    articulo: ArticuloEntity,
    colorCategoria: Color,
    precioFormateado: String,
    cantidadCarrito: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text(
                    text = articulo.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = precioFormateado,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colorCategoria)
                )
            }

            if (cantidadCarrito > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Badge { Text(cantidadCarrito.toString(), fontSize = 10.sp) }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String?): Color =
    try {
        if (hex.isNullOrBlank()) Color(0xFFCCCCCC.toInt())
        else Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFFCCCCCC.toInt())
    }
