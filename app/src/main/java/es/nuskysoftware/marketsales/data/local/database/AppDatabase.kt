// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/AppDatabase.kt
package es.nuskysoftware.marketsales.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity
import es.nuskysoftware.marketsales.data.local.entity.UserEntity
import es.nuskysoftware.marketsales.data.local.dao.ConfiguracionDao
import es.nuskysoftware.marketsales.data.local.dao.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [ConfiguracionEntity::class, UserEntity::class], // ✅ AGREGADA UserEntity
    version = 4, // ✅ ACTUALIZADA A VERSIÓN 4
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun userDao(): UserDao // ✅ AGREGADO UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // CoroutineScope para operaciones de base de datos
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marketsales_database"
                )
                    .fallbackToDestructiveMigration() // ✅ DESTRUIR Y RECREAR EN DESARROLLO
                    .addCallback(DatabaseCallback(applicationScope))
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // Método para limpiar la instancia (útil para testing)
        fun clearInstance() {
            INSTANCE = null
        }
    }

    /**
     * Callback para inicializar la base de datos con datos por defecto
     * Implementa sistema offline-first
     */
    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            println("🔍 DEBUG: Base de datos V4 creada, inicializando configuración por defecto...")

            // Inicializar base de datos en background
            INSTANCE?.let { database ->
                scope.launch {
                    try {
                        // ✅ OFFLINE-FIRST: Crear configuración local primero
                        crearConfiguracionPorDefecto(database.configuracionDao())

                        // ✅ NUEVO: Inicializar tabla usuarios vacía (se llenará cuando se loguee)
                        println("✅ DEBUG: Tabla usuarios inicializada (vacía)")

                        println("🚀 DEBUG: Sistema híbrido offline-first V4 iniciado correctamente")

                    } catch (e: Exception) {
                        println("❌ ERROR al crear configuración por defecto: ${e.message}")
                    }
                }
            }
        }

        /**
         * Crea la configuración por defecto siguiendo el patrón offline-first
         */
        private suspend fun crearConfiguracionPorDefecto(configuracionDao: ConfiguracionDao) {
            // Verificar si ya existe configuración
            val configuracionExistente = configuracionDao.getConfiguracionSync()

            if (configuracionExistente == null) {
                println("🔍 DEBUG: No existe configuración, creando por defecto...")

                val configuracionPorDefecto = ConfiguracionEntity(
                    id = 1,
                    versionApp = 0, // FREE por defecto
                    numeroVersion = "V1.0",
                    ultimoDispositivo = android.os.Build.MODEL, // ✅ Dispositivo actual
                    usuarioEmail = null,
                    usuarioId = "usuario_default", // ✅ ID por defecto para Firebase
                    usuarioPassword = null,
                    idioma = "es", // Español por defecto
                    temaOscuro = false, // Tema claro por defecto
                    fuente = "Montserrat", // Fuente por defecto
                    moneda = "€ Euro", // Moneda por defecto
                    fechaUltimaSync = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )

                // ✅ PASO 1: Guardar en Room (offline-first)
                configuracionDao.insertOrUpdate(configuracionPorDefecto)
                println("✅ DEBUG: Configuración por defecto creada en Room")

                // ✅ PASO 2: Sincronizar inmediatamente con Firebase
                sincronizarConFirebase(configuracionPorDefecto)

            } else {
                println("✅ DEBUG: Configuración ya existe: $configuracionExistente")

                // ✅ Intentar sincronizar configuración existente si no está sincronizada
                if (configuracionExistente.fechaUltimaSync == null) {
                    sincronizarConFirebase(configuracionExistente)
                }
            }
        }

        /**
         * Sincroniza la configuración con Firebase inmediatamente
         * Sistema híbrido offline-first funcionando
         */
        private suspend fun sincronizarConFirebase(configuracion: ConfiguracionEntity) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val documentoId = configuracion.usuarioId ?: "usuario_default"
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

                val datosParaFirebase = mapOf(
                    "versionApp" to configuracion.versionApp,
                    "numeroVersion" to configuracion.numeroVersion,
                    "ultimoDispositivo" to configuracion.ultimoDispositivo,
                    "usuarioEmail" to configuracion.usuarioEmail,
                    "usuarioId" to configuracion.usuarioId,
                    "usuarioPassword" to configuracion.usuarioPassword,
                    "idioma" to configuracion.idioma,
                    "temaOscuro" to configuracion.temaOscuro,
                    "fuente" to configuracion.fuente,
                    "moneda" to configuracion.moneda,
                    "fechaUltimaSync" to dateFormat.format(java.util.Date()),
                    "fechaCreacion" to dateFormat.format(java.util.Date()),
                    "dispositivo" to android.os.Build.MODEL
                )

                // ✅ Sincronización con Firebase
                firestore.collection("configuraciones")
                    .document(documentoId)
                    .set(datosParaFirebase)
                    .addOnSuccessListener {
                        println("✅ DEBUG: Configuración sincronizada con Firebase exitosamente")
                    }
                    .addOnFailureListener { e ->
                        println("❌ DEBUG: Error sincronizando con Firebase: ${e.message}")
                        // El sistema offline-first seguirá funcionando sin Firebase
                    }

                println("🔄 DEBUG: Sincronización con Firebase iniciada")

            } catch (e: Exception) {
                println("❌ DEBUG: Error en sincronización Firebase: ${e.message}")
                // El sistema offline-first continúa funcionando sin problemas
            }
        }
    }
}


//// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/AppDatabase.kt
//package es.nuskysoftware.marketsales.data.local.database
//
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.sqlite.db.SupportSQLiteDatabase
//import android.content.Context
//import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity
//import es.nuskysoftware.marketsales.data.local.dao.ConfiguracionDao
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.launch
//
//@Database(
//    entities = [ConfiguracionEntity::class],
//    version = 3, // ✅ ACTUALIZADA A VERSIÓN 3
//    exportSchema = false
//)
//abstract class AppDatabase : RoomDatabase() {
//
//    abstract fun configuracionDao(): ConfiguracionDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        // CoroutineScope para operaciones de base de datos
//        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "marketsales_database"
//                )
//                    .fallbackToDestructiveMigration() // ✅ DESTRUIR Y RECREAR EN DESARROLLO
//                    .addCallback(DatabaseCallback(applicationScope))
//                    .build()
//
//                INSTANCE = instance
//                instance
//            }
//        }
//
//        // Método para limpiar la instancia (útil para testing)
//        fun clearInstance() {
//            INSTANCE = null
//        }
//    }
//
//    /**
//     * Callback para inicializar la base de datos con datos por defecto
//     * Implementa sistema offline-first
//     */
//    private class DatabaseCallback(
//        private val scope: CoroutineScope
//    ) : RoomDatabase.Callback() {
//
//        override fun onCreate(db: SupportSQLiteDatabase) {
//            super.onCreate(db)
//
//            println("🔍 DEBUG: Base de datos creada, inicializando configuración por defecto...")
//
//            // Inicializar base de datos en background
//            INSTANCE?.let { database ->
//                scope.launch {
//                    try {
//                        // ✅ OFFLINE-FIRST: Crear configuración local primero
//                        crearConfiguracionPorDefecto(database.configuracionDao())
//
//                        println("🚀 DEBUG: Sistema híbrido offline-first iniciado correctamente")
//
//                    } catch (e: Exception) {
//                        println("❌ ERROR al crear configuración por defecto: ${e.message}")
//                    }
//                }
//            }
//        }
//
//        /**
//         * Crea la configuración por defecto siguiendo el patrón offline-first
//         */
//        private suspend fun crearConfiguracionPorDefecto(configuracionDao: ConfiguracionDao) {
//            // Verificar si ya existe configuración
//            val configuracionExistente = configuracionDao.getConfiguracionSync()
//
//            if (configuracionExistente == null) {
//                println("🔍 DEBUG: No existe configuración, creando por defecto...")
//
//                val configuracionPorDefecto = ConfiguracionEntity(
//                    id = 1,
//                    versionApp = 0, // FREE por defecto
//                    numeroVersion = "V1.0",
//                    ultimoDispositivo = android.os.Build.MODEL, // ✅ Dispositivo actual
//                    usuarioEmail = null,
//                    usuarioId = "usuario_default", // ✅ ID por defecto para Firebase
//                    usuarioPassword = null,
//                    idioma = "es", // Español por defecto
//                    temaOscuro = false, // Tema claro por defecto
//                    fuente = "Montserrat", // Fuente por defecto
//                    moneda = "€ Euro", // Moneda por defecto
//                    fechaUltimaSync = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
//                )
//
//                // ✅ PASO 1: Guardar en Room (offline-first)
//                configuracionDao.insertOrUpdate(configuracionPorDefecto)
//                println("✅ DEBUG: Configuración por defecto creada en Room")
//
//                // ✅ PASO 2: Sincronizar inmediatamente con Firebase
//                sincronizarConFirebase(configuracionPorDefecto)
//
//            } else {
//                println("✅ DEBUG: Configuración ya existe: $configuracionExistente")
//
//                // ✅ Intentar sincronizar configuración existente si no está sincronizada
//                if (configuracionExistente.fechaUltimaSync == null) {
//                    sincronizarConFirebase(configuracionExistente)
//                }
//            }
//        }
//
//        /**
//         * Sincroniza la configuración con Firebase inmediatamente
//         * Sistema híbrido offline-first funcionando
//         */
//        private suspend fun sincronizarConFirebase(configuracion: ConfiguracionEntity) {
//            try {
//                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
//                val documentoId = configuracion.usuarioId ?: "usuario_default"
//                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
//
//                val datosParaFirebase = mapOf(
//                    "versionApp" to configuracion.versionApp,
//                    "numeroVersion" to configuracion.numeroVersion,
//                    "ultimoDispositivo" to configuracion.ultimoDispositivo,
//                    "usuarioEmail" to configuracion.usuarioEmail,
//                    "usuarioId" to configuracion.usuarioId,
//                    "usuarioPassword" to configuracion.usuarioPassword,
//                    "idioma" to configuracion.idioma,
//                    "temaOscuro" to configuracion.temaOscuro,
//                    "fuente" to configuracion.fuente,
//                    "moneda" to configuracion.moneda,
//                    "fechaUltimaSync" to dateFormat.format(java.util.Date()),
//                    "fechaCreacion" to dateFormat.format(java.util.Date()),
//                    "dispositivo" to android.os.Build.MODEL
//                )
//
//                // ✅ Sincronización con Firebase
//                firestore.collection("configuraciones")
//                    .document(documentoId)
//                    .set(datosParaFirebase)
//                    .addOnSuccessListener {
//                        println("✅ DEBUG: Configuración sincronizada con Firebase exitosamente")
//                    }
//                    .addOnFailureListener { e ->
//                        println("❌ DEBUG: Error sincronizando con Firebase: ${e.message}")
//                        // El sistema offline-first seguirá funcionando sin Firebase
//                    }
//
//                println("🔄 DEBUG: Sincronización con Firebase iniciada")
//
//            } catch (e: Exception) {
//                println("❌ DEBUG: Error en sincronización Firebase: ${e.message}")
//                // El sistema offline-first continúa funcionando sin problemas
//            }
//        }
//
//        /**
//         * @deprecated Ya no se usa - Firebase está implementado
//         */
//        @Deprecated("Firebase ya está funcionando")
//        private fun marcarComoPendienteSincronizacion(configuracion: ConfiguracionEntity) {
//            // Este método ya no se necesita
//        }
//    }
//}
