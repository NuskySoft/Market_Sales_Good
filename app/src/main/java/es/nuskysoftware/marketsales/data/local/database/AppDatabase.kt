// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/AppDatabase.kt
package es.nuskysoftware.marketsales.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import es.nuskysoftware.marketsales.data.local.dao.*
import es.nuskysoftware.marketsales.data.local.entity.*

@Database(
    entities = [
        ConfiguracionEntity::class,
        MercadilloEntity::class,
        UserEntity::class,
        CategoriaEntity::class,
        ArticuloEntity::class,
        ReciboEntity::class,
        LineaVentaEntity::class,
        LineaGastoEntity::class,
        SaldoGuardadoEntity::class, // ✅ añadida
    ],
    version = 12,                 // ✅ subimos a 12 (tabla saldos_guardados)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun mercadilloDao(): MercadilloDao
    abstract fun userDao(): UserDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun articuloDao(): ArticuloDao
    abstract fun recibosDao(): RecibosDao
    abstract fun lineasVentaDao(): LineasVentaDao
    abstract fun lineasGastosDao(): LineasGastosDao
    abstract fun saldoGuardadoDao(): SaldoGuardadoDao // ✅ faltaba

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // formaPago en gastos (ya lo tenías)
                db.execSQL(
                    "ALTER TABLE lineas_gastos ADD COLUMN formaPago TEXT NOT NULL DEFAULT 'efectivo'"
                )
            }
        }

        // ✅ Nueva tabla saldos_guardados
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saldos_guardados(
                        idRegistro TEXT NOT NULL PRIMARY KEY,
                        idUsuario TEXT NOT NULL,
                        idMercadilloOrigen TEXT NOT NULL,
                        fechaMercadillo TEXT NOT NULL,
                        lugarMercadillo TEXT NOT NULL,
                        organizadorMercadillo TEXT NOT NULL,
                        horaInicioMercadillo TEXT NOT NULL,
                        saldoInicialGuardado REAL NOT NULL,
                        consumido INTEGER NOT NULL DEFAULT 0,
                        version INTEGER NOT NULL DEFAULT 1,
                        lastModified INTEGER NOT NULL,
                        sincronizadoFirebase INTEGER NOT NULL DEFAULT 0,
                        notas TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marketsales.db"
                )
                    .addMigrations(
                        MIGRATION_10_11,
                        MIGRATION_11_12
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}




//// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/AppDatabase.kt
//package es.nuskysoftware.marketsales.data.local.database
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.room.migration.Migration
//import androidx.sqlite.db.SupportSQLiteDatabase
//import com.google.firebase.BuildConfig
//
//import es.nuskysoftware.marketsales.data.local.dao.ArticuloDao
//import es.nuskysoftware.marketsales.data.local.dao.CategoriaDao
//import es.nuskysoftware.marketsales.data.local.dao.ConfiguracionDao
//import es.nuskysoftware.marketsales.data.local.dao.LineasVentaDao
//import es.nuskysoftware.marketsales.data.local.dao.MercadilloDao
//import es.nuskysoftware.marketsales.data.local.dao.RecibosDao
//import es.nuskysoftware.marketsales.data.local.dao.UserDao
//import es.nuskysoftware.marketsales.data.local.entity.ArticuloEntity
//import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
//import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity
//import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
//import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
//
//import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
//import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
//import es.nuskysoftware.marketsales.data.local.entity.UserEntity
//
//@Database(
//    entities = [
//        ConfiguracionEntity::class,
//        MercadilloEntity::class,
//        UserEntity::class,
//        CategoriaEntity::class,
//        ArticuloEntity::class,
//        ReciboEntity::class,
//        LineaVentaEntity::class,
//        LineaGastoEntity::class
//    ],
//    version = 11, // ⬆️ subimos a 11 para añadir formaPago a LineasGastos
//    exportSchema = false
//)
//abstract class AppDatabase : RoomDatabase() {
//
//    abstract fun configuracionDao(): ConfiguracionDao
//    abstract fun mercadilloDao(): MercadilloDao
//    abstract fun userDao(): UserDao
//    abstract fun categoriaDao(): CategoriaDao
//    abstract fun articuloDao(): ArticuloDao
//    abstract fun recibosDao(): RecibosDao
//    abstract fun lineasVentaDao(): LineasVentaDao
//
//    abstract fun lineasGastosDao(): es.nuskysoftware.marketsales.data.local.dao.LineasGastosDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        // ➕ Migración NO-OP de 7 → 8 (ajusta aquí si realmente cambiaste esquema)
//        private val MIGRATION_7_8 = object : Migration(7, 8) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                // No-op: si entre 7 y 8 no hubo cambios de esquema efectivos.
//                // Si añadiste columnas/tablas de verdad, pon aquí tus ALTER TABLE … ADD COLUMN … DEFAULT …
//                // Ejemplo (coméntalo/ajústalo si aplica):
//                // db.execSQL("ALTER TABLE configuracion ADD COLUMN usuarioLogueado TEXT NOT NULL DEFAULT 'usuario_default'")
//            }
//        }
//        // 8 -> 9 (si no hubo cambios de esquema reales entre 8 y 9, es NO-OP)
//        val M8_TO_M9 = object : Migration(8, 9) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                // No-Op. Si en tu proyecto real hubo cambios en v9, añádelos aquí (ALTER TABLE ...).
//            }
//        }
//        val M9_TO_M10 = object : androidx.room.migration.Migration(9, 10) {
//            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
//                db.execSQL(
//                    """
//            CREATE TABLE IF NOT EXISTS LineasGastos (
//                idMercadillo TEXT NOT NULL,
//                numeroLinea TEXT NOT NULL,
//                idUsuario TEXT NOT NULL,
//                descripcion TEXT NOT NULL,
//                importe REAL NOT NULL,
//                fechaHora INTEGER NOT NULL,
//                PRIMARY KEY(idMercadillo, numeroLinea)
//            )
//            """.trimIndent()
//                )
//                db.execSQL("CREATE INDEX IF NOT EXISTS idx_gastos_mercadillo ON LineasGastos(idMercadillo)")
//                db.execSQL("CREATE INDEX IF NOT EXISTS idx_gastos_usuario ON LineasGastos(idUsuario)")
//            }
//        }
//
//        val M10_TO_M11 = object : androidx.room.migration.Migration(10, 11) {
//            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
//                db.execSQL("ALTER TABLE LineasGastos ADD COLUMN formaPago TEXT NOT NULL DEFAULT 'efectivo'")
//            }
//        }
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//
//                val builder = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "marketsales_database_v10"
//                )
//                    // ✅ Registra tus migraciones conocidas
//                    .addMigrations(
//                        Migrations.M6_TO_M7,       // si la tienes
//                        MIGRATION_7_8,             // tu 7->8 no-op
//                        M8_TO_M9,       // <- NUEVA/asegurar que esté
//                        M9_TO_M10,       // <- NUEVA (crea LineasGastos)
//                        M10_TO_M11   // <- registra la nueva
////                        Migrations.M6_TO_M7, // la que ya usabas
////                        MIGRATION_7_8 ,       // nueva para evitar el crash 8→7
////                        M9_TO_M10,
//                    )
//
//                // 🔧 Solo en DEBUG: si por error instalas un APK más viejo (downgrade), evita el crash
//                if (BuildConfig.DEBUG) {
//                    builder.fallbackToDestructiveMigrationOnDowngrade()
//                }
//
//                val instance = builder.build()
//                INSTANCE = instance
//                instance
//            }
//        }
//
//        fun clearInstance() {
//            INSTANCE = null
//        }
//    }
//}
