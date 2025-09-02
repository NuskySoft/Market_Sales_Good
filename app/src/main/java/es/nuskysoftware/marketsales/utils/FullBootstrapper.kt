// app/src/main/java/es/nuskysoftware/marketsales/utils/FullBootstrapper.kt
package es.nuskysoftware.marketsales.utils

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.*

/**
 * Bootstrap para poblar **TODAS** las tablas de Room desde Firebase
 * cuando la base local está vacía para el usuario indicado.
 *
 * Tablas: usuarios, configuracion, categorias, articulos,
 * mercadillos, recibos, lineas_venta, lineas_gasto, saldos_guardados.
 *
 * Política: lo descargado entra con sincronizadoFirebase=true (si el entity lo tiene).
 */
object FullBootstrapper {

    private const val TAG = "FullBootstrapper"

    suspend fun runIfNeeded(context: Context, rawUserId: String?): Boolean {
        val uid = (rawUserId ?: "").ifBlank { "usuario_default" }

        val db = AppDatabase.getDatabase(context)
        val categoriaDao   = db.categoriaDao()
        val articuloDao    = db.articuloDao()
        val mercadilloDao  = db.mercadilloDao()
        val recibosDao     = db.recibosDao()
        val lineasVentaDao = db.lineasVentaDao()
        val lineasGastosDao= db.lineasGastosDao()
        val configDao      = db.configuracionDao()
        val userDao        = db.userDao()
        val saldosDao      = db.saldoGuardadoDao()

        // ¿Room vacío para este usuario?
        val vacio = !(
                categoriaDao.existeAlgunoDeUsuario(uid) ||
                        articuloDao.existeAlgunoDeUsuario(uid)  ||
                        mercadilloDao.existeAlgunoDeUsuario(uid)||
                        recibosDao.existeAlgunoDeUsuario(uid)   ||
                        lineasVentaDao.existeAlgunoDeUsuario(uid) ||
                        (saldosDao.getUltimoNoConsumido(uid) != null) ||
                        userDao.existeUsuario(uid) ||
                        configDao.existeConfiguracion()
                )

        if (!vacio) {
            Log.d(TAG, "Room NO está vacío para $uid → no bootstrap")
            return false
        }

        Log.i(TAG, "Room vacío para $uid → bootstrap desde Firebase…")
        val fs = FirebaseFirestore.getInstance()

        // ===== Descargas por colección =====
        val userSnap  = fs.collection("usuarios").document(uid).get().await()
        val confDoc   = fs.collection("configuracion").document(uid).get().await()
        val confQuery = fs.collection("configuracion").whereEqualTo("userId", uid).get().await()
//        val catsSnap  = fs.collection("categorias").whereEqualTo("userId", uid).get().await()
//        val artsSnap  = fs.collection("articulos").whereEqualTo("userId", uid).get().await()
//        val mercSnap  = fs.collection("mercadillos").whereEqualTo("userId", uid).get().await()
//        val recsSnap  = fs.collection("recibos").whereEqualTo("idUsuario", uid).get().await()
//        val linsSnap  = fs.collection("lineas_venta").whereEqualTo("idUsuario", uid).get().await()
//        val gastoSnap = fs.collection("lineas_gasto").whereEqualTo("idUsuario", uid).get().await()
//        val saldoSnap = fs.collection("saldosGuardados").whereEqualTo("idUsuario", uid).get().await()
        val catsSnap  = fs.collection("categorias")
            .whereEqualTo("userId", uid)
            .whereEqualTo("activa", true)
            .get().await()
        val artsSnap  = fs.collection("articulos")
            .whereEqualTo("userId", uid)
            .whereEqualTo("activo", true)
            .get().await()
        val mercSnap  = fs.collection("mercadillos")
            .whereEqualTo("userId", uid)
            .whereEqualTo("activo", true)
            .get().await()
        val recsSnap  = fs.collection("recibos")
            .whereEqualTo("idUsuario", uid)
            .whereEqualTo("activo", true)
            .get().await()
        val linsSnap  = fs.collection("lineas_venta")
            .whereEqualTo("idUsuario", uid)
            .whereEqualTo("activo", true)
            .get().await()
        val gastoSnap = fs.collection("lineas_gasto")
            .whereEqualTo("idUsuario", uid)
            .whereEqualTo("activo", true)
            .get().await()
        val saldoSnap = fs.collection("saldosGuardados")
            .whereEqualTo("idUsuario", uid)
            .get().await()

        // ===== Map a Entities (coincidiendo con tus data classes actuales) =====

        // usuarios
        val usuario: UserEntity? = userSnap.data?.let { m ->
            UserEntity(
                uid = uid,
                email = (m["email"] as? String) ?: "",
                displayName = (m["displayName"] as? String) ?: (m["nombreUsuario"] as? String ?: ""),
                photoUrl = (m["photoUrl"] as? String) ?: "",
                esPremium = (m["esPremium"] as? Boolean) ?: false,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                fechaCreacion = (m["fechaCreacion"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                fechaUltimaSync = (m["fechaUltimaSync"] as? String),
                sincronizadoFirebase = true,
                activo = (m["activo"] as? Boolean) ?: true
            )
        }

        // configuracion (monoregistro con PK fija)
        val configuracion: ConfiguracionEntity = when {
            confDoc.exists() && confDoc.data != null -> {
                val m = confDoc.data!!
                ConfiguracionEntity(
                    moneda = (m["moneda"] as? String) ?: "€ Euro",
                    idioma = (m["idioma"] as? String) ?: "es",
                    fuente = (m["fuente"] as? String) ?: "Montserrat",
                    temaOscuro = (m["temaOscuro"] as? Boolean) ?: false,
                    usuarioLogueado = uid,
                    numeroVersion = (m["numeroVersion"] as? String) ?: "V10.0",
                    ultimoDispositivo = m["ultimoDispositivo"] as? String,
                    fechaUltimaSync = m["fechaUltimaSync"] as? String,
                    version = (m["version"] as? Number)?.toLong() ?: 1L,
                    lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    pendienteSync = (m["pendienteSync"] as? Boolean) ?: false,
                    sincronizadoFirebase = true
                )
            }
            confQuery.documents.isNotEmpty() -> {
                val m = confQuery.documents.first().data!!
                ConfiguracionEntity(
                    moneda = (m["moneda"] as? String) ?: "€ Euro",
                    idioma = (m["idioma"] as? String) ?: "es",
                    fuente = (m["fuente"] as? String) ?: "Montserrat",
                    temaOscuro = (m["temaOscuro"] as? Boolean) ?: false,
                    usuarioLogueado = uid,
                    numeroVersion = (m["numeroVersion"] as? String) ?: "V10.0",
                    ultimoDispositivo = m["ultimoDispositivo"] as? String,
                    fechaUltimaSync = (m["fechaUltimaSync"] as? String),
                    version = (m["version"] as? Number)?.toLong() ?: 1L,
                    lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    pendienteSync = (m["pendienteSync"] as? Boolean) ?: false,
                    sincronizadoFirebase = true
                )
            }
            else -> {
                ConfiguracionEntity(
                    usuarioLogueado = uid,
                    sincronizadoFirebase = true
                )
            }
        }

        // categorias
        val categorias: List<CategoriaEntity> = catsSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            CategoriaEntity(
                idCategoria = (m["idCategoria"] as? String) ?: d.id,
                userId = (m["userId"] as? String) ?: uid,
                nombre = (m["nombre"] as? String) ?: "",
                colorHex = (m["colorHex"] as? String) ?: "#9E9E9E",
                orden = (m["orden"] as? Number)?.toInt() ?: 0,
                activa = (m["activa"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // articulos
        val articulos: List<ArticuloEntity> = artsSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            ArticuloEntity(
                idArticulo = (m["idArticulo"] as? String) ?: d.id,
                userId = (m["userId"] as? String) ?: uid,
                nombre = (m["nombre"] as? String) ?: "",
                idCategoria = (m["idCategoria"] as? String) ?: "",
                precioVenta = (m["precioVenta"] as? Number)?.toDouble()
                    ?: (m["precio"] as? Number)?.toDouble() ?: 0.0,
                precioCoste = (m["precioCoste"] as? Number)?.toDouble(),
                stock = (m["stock"] as? Number)?.toInt(),
                controlarStock = (m["controlarStock"] as? Boolean) ?: false,
                controlarCoste = (m["controlarCoste"] as? Boolean) ?: false,
                favorito = (m["favorito"] as? Boolean) ?: false,
                fotoUri = (m["fotoUri"] as? String),
                activo = (m["activo"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // mercadillos
        val mercadillos: List<MercadilloEntity> = mercSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            MercadilloEntity(
                idMercadillo = (m["idMercadillo"] as? String) ?: d.id,
                userId = (m["userId"] as? String) ?: uid,
                fecha = (m["fecha"] as? String) ?: "",
                lugar = (m["lugar"] as? String) ?: "",
                organizador = (m["organizador"] as? String) ?: "",
                esGratis = (m["esGratis"] as? Boolean) ?: true,
                importeSuscripcion = (m["importeSuscripcion"] as? Number)?.toDouble() ?: 0.0,
                requiereMesa = (m["requiereMesa"] as? Boolean) ?: true,
                requiereCarpa = (m["requiereCarpa"] as? Boolean) ?: true,
                hayPuntoLuz = (m["hayPuntoLuz"] as? Boolean) ?: false,
                horaInicio = (m["horaInicio"] as? String) ?: "09:00",
                horaFin = (m["horaFin"] as? String) ?: "14:00",
                estado = (m["estado"] as? Number)?.toInt() ?: 1,
                pendienteArqueo = (m["pendienteArqueo"] as? Boolean) ?: false,
                pendienteAsignarSaldo = (m["pendienteAsignarSaldo"] as? Boolean) ?: false,
                saldoInicial = (m["saldoInicial"] as? Number)?.toDouble(),
                saldoFinal = (m["saldoFinal"] as? Number)?.toDouble(),
                arqueoCaja = (m["arqueoCaja"] as? Number)?.toDouble(),
                totalVentas = (m["totalVentas"] as? Number)?.toDouble() ?: 0.0,
                totalGastos = (m["totalGastos"] as? Number)?.toDouble() ?: 0.0,
                arqueoMercadillo = (m["arqueoMercadillo"] as? Number)?.toDouble() ?: 0.0,
                activo = (m["activo"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // recibos
        val recibos: List<ReciboEntity> = recsSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            ReciboEntity(
                idRecibo = (m["idRecibo"] as? String) ?: d.id,
                idUsuario = (m["idUsuario"] as? String) ?: uid,
                idMercadillo = (m["idMercadillo"] as? String) ?: "",
                fechaHora = (m["fechaHora"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                metodoPago = (m["metodoPago"] as? String) ?: "EFECTIVO",
                totalTicket = (m["totalTicket"] as? Number)?.toDouble() ?: 0.0,
                estado = (m["estado"] as? String) ?: "COMPLETADO",
                activo = (m["activo"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // lineas_venta
        val lineasVenta: List<LineaVentaEntity> = linsSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            LineaVentaEntity(
                idLinea = (m["idLinea"] as? String) ?: d.id,
                idUsuario = (m["idUsuario"] as? String) ?: uid,
                idMercadillo = (m["idMercadillo"] as? String) ?: "",
                idRecibo = (m["idRecibo"] as? String) ?: "",
                numeroLinea = (m["numeroLinea"] as? Number)?.toInt() ?: 1,
                tipoLinea = (m["tipoLinea"] as? String) ?: "VENTA",
                descripcion = (m["descripcion"] as? String) ?: "",
                idProducto = (m["idProducto"] as? String) ?: (m["idArticulo"] as? String ?: ""),
                cantidad = (m["cantidad"] as? Number)?.toInt() ?: 1,
                precioUnitario = (m["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (m["subtotal"] as? Number)?.toDouble()
                    ?: (m["totalLinea"] as? Number)?.toDouble() ?: 0.0,
                activo = (m["activo"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // lineas_gasto
        // lineas_gasto  (tu entidad exige fechaHora → lo mapeo; si no existe en Firebase, uso now)
        val lineasGasto = gastoSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            LineaGastoEntity(
                numeroLinea = (m["idLinea"] as? String) ?: d.id,
                idUsuario = (m["idUsuario"] as? String) ?: uid,
                idMercadillo = (m["idMercadillo"] as? String) ?: "",
                descripcion = (m["concepto"] as? String) ?: "",
                importe = (m["importe"] as? Number)?.toDouble() ?: 0.0,
                fechaHora = (m["fechaHora"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                formaPago = (m["formaPago"] as? String) ?: "EFECTIVO",
                activo = (m["activo"] as? Boolean) ?: true,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true
            )
        }

        // saldos_guardados
        val saldosGuardados: List<SaldoGuardadoEntity> = saldoSnap.documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            SaldoGuardadoEntity(
                idRegistro = (m["idRegistro"] as? String) ?: d.id,
                idUsuario = (m["idUsuario"] as? String) ?: uid,
                idMercadilloOrigen = (m["idMercadilloOrigen"] as? String) ?: "",
                fechaMercadillo = (m["fechaMercadillo"] as? String) ?: "",
                lugarMercadillo = (m["lugarMercadillo"] as? String) ?: "",
                organizadorMercadillo = (m["organizadorMercadillo"] as? String) ?: "",
                horaInicioMercadillo = (m["horaInicioMercadillo"] as? String) ?: "",
                saldoInicialGuardado = (m["saldoInicialGuardado"] as? Number)?.toDouble() ?: 0.0,
                consumido = (m["consumido"] as? Boolean) ?: false,
                version = (m["version"] as? Number)?.toLong() ?: 1L,
                lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sincronizadoFirebase = true,
                notas = (m["notas"] as? String)
            )
        }

        // ===== Escribir TODO en transacción =====
        db.withTransaction {
            // Usuario → SQL directo para evitar depender del nombre del método en UserDao
            usuario?.let { u: UserEntity ->
                val sql = """
                    INSERT OR REPLACE INTO usuarios
                    (uid, email, displayName, photoUrl, esPremium, version, lastModified, fechaCreacion, fechaUltimaSync, sincronizadoFirebase, activo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                val args = arrayOf(
                    u.uid,
                    u.email,
                    u.displayName,
                    u.photoUrl,
                    if (u.esPremium) 1 else 0,
                    u.version,
                    u.lastModified,
                    u.fechaCreacion,
                    u.fechaUltimaSync,
                    if (u.sincronizadoFirebase) 1 else 0,
                    if (u.activo) 1 else 0
                )
                db.openHelper.writableDatabase.execSQL(sql, args)
            }

            // Configuración (PK fija ⇒ insertar/replace)
            configDao.insertarConfiguracion(configuracion)

            // Resto
            categoriaDao.upsertAll(categorias)
            articuloDao.upsertAll(articulos)
            mercadilloDao.upsertAll(mercadillos)
            recibosDao.upsertAll(recibos)
            lineasVentaDao.upsertAll(lineasVenta)
            lineasGastosDao.insertarLineas(lineasGasto)
            saldosGuardados.forEach { s -> saldosDao.upsert(s) }
        }

        Log.i(
            TAG,
            "Bootstrap OK uid=$uid → cats=${categorias.size}, arts=${articulos.size}, merc=${mercadillos.size}, rec=${recibos.size}, linV=${lineasVenta.size}, linG=${lineasGasto.size}, saldos=${saldosGuardados.size}, conf=1, user=${usuario != null}"
        )
        return true
    }
}


