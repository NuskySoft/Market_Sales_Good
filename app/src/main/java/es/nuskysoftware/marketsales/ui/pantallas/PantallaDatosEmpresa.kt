package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ui.viewmodel.EmpresaViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.EmpresaViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDatosEmpresa(navController: NavController? = null) {
    val currentLanguage = ConfigurationManager.idioma.collectAsState().value
    val context = LocalContext.current
    val viewModel: EmpresaViewModel = viewModel(factory = EmpresaViewModelFactory(context))
    val empresaState by viewModel.empresa.collectAsState()

    var nif by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var razon by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var poblacion by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }
    var provincia by remember { mutableStateOf("") }
    var pais by remember { mutableStateOf("") }
    var cargado by remember { mutableStateOf(false) }

    LaunchedEffect(empresaState) {
        if (!cargado && empresaState != null) {
            nif = empresaState!!.nif
            nombre = empresaState!!.nombre
            razon = empresaState!!.razonSocial
            direccion = empresaState!!.direccion
            poblacion = empresaState!!.poblacion
            codigoPostal = empresaState!!.codigoPostal
            provincia = empresaState!!.provincia
            pais = empresaState!!.pais
            cargado = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringResourceManager.getString("datos_empresa", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nif,
                onValueChange = { nif = it },
                label = { Text(StringResourceManager.getString("nif_dni", currentLanguage)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(StringResourceManager.getString("nombre_empresa", currentLanguage)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = razon,
                onValueChange = { razon = it },
                label = { Text(StringResourceManager.getString("razon_social", currentLanguage)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text(StringResourceManager.getString("direccion", currentLanguage)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = poblacion,
                onValueChange = { poblacion = it },
                label = { Text(StringResourceManager.getString("poblacion", currentLanguage)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = codigoPostal,
                onValueChange = { codigoPostal = it },
                label = { Text(StringResourceManager.getString("codigo_postal", currentLanguage)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = provincia,
                onValueChange = { provincia = it },
                label = { Text(StringResourceManager.getString("provincia", currentLanguage)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pais,
                onValueChange = { pais = it },
                label = { Text(StringResourceManager.getString("pais", currentLanguage)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = { navController?.popBackStack() }) {
                    Text(StringResourceManager.getString("cancelar", currentLanguage))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        viewModel.guardarOActualizar(
                            nif,
                            nombre,
                            razon,
                            direccion,
                            poblacion,
                            codigoPostal,
                            provincia,
                            pais
                        ) {
                            navController?.popBackStack()
                        }
                    }
                ) {
                    val key = if (empresaState == null) "guardar" else "actualizar"
                    Text(StringResourceManager.getString(key, currentLanguage))
                }
            }
        }
    }
}

//package es.nuskysoftware.marketsales.ui.pantallas
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.R
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.StringResourceManager
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaDatosEmpresa(navController: NavController? = null) {
//    val currentLanguage = ConfigurationManager.idioma.collectAsState().value
//
//    var nif by remember { mutableStateOf("") }
//    var nombre by remember { mutableStateOf("") }
//    var razon by remember { mutableStateOf("") }
//    var direccion by remember { mutableStateOf("") }
//    var poblacion by remember { mutableStateOf("") }
//    var codigoPostal by remember { mutableStateOf("") }
//    var provincia by remember { mutableStateOf("") }
//    var pais by remember { mutableStateOf("") }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        StringResourceManager.getString("datos_empresa", currentLanguage),
//                        fontWeight = FontWeight.Bold
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController?.popBackStack() }) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_arrow_left),
//                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
//                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
//                )
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp)
//                .verticalScroll(rememberScrollState()),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            OutlinedTextField(
//                value = nif,
//                onValueChange = { nif = it },
//                label = { Text(StringResourceManager.getString("nif_dni", currentLanguage)) },
//                modifier = Modifier.fillMaxWidth()
//            )
//            OutlinedTextField(
//                value = nombre,
//                onValueChange = { nombre = it },
//                label = { Text(StringResourceManager.getString("nombre_empresa", currentLanguage)) },
//                modifier = Modifier.fillMaxWidth()
//            )
//            OutlinedTextField(
//                value = razon,
//                onValueChange = { razon = it },
//                label = { Text(StringResourceManager.getString("razon_social", currentLanguage)) },
//                modifier = Modifier.fillMaxWidth()
//            )
//            OutlinedTextField(
//                value = direccion,
//                onValueChange = { direccion = it },
//                label = { Text(StringResourceManager.getString("direccion", currentLanguage)) },
//                modifier = Modifier.fillMaxWidth()
//            )
//            OutlinedTextField(
//                value = poblacion,
//                onValueChange = { poblacion = it },
//                label = { Text(StringResourceManager.getString("poblacion", currentLanguage)) },
//                modifier = Modifier.fillMaxWidth()
//            )
//            OutlinedTextField(
//                value = codigoPostal,
//                onValueChange = { codigoPostal = it },
//                label = { Text(StringResourceManager.getString("codigo_postal", currentLanguage)) },
//                modifier = Modifier.fillMaxWidth()
//            )
//            OutlinedTextField(
//                value = provincia,
//                onValueChange = { provincia = it },
//                label = { Text(StringResourceManager.getString("provincia", currentLanguage)) },
//                modifier = Modifier.fillMaxWidth()
//            )
//            OutlinedTextField(
//                value = pais,
//                onValueChange = { pais = it },
//                label = { Text(StringResourceManager.getString("pais", currentLanguage)) },
//                modifier = Modifier.fillMaxWidth()
//            )
//            Button(
//                onClick = { navController?.popBackStack() },
//                modifier = Modifier.align(Alignment.End)
//            ) {
//                Text(StringResourceManager.getString("guardar", currentLanguage))
//            }
//        }
//    }
//}