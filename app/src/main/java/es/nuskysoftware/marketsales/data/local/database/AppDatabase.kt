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
        SaldoGuardadoEntity::class, // v12
        EmpresaEntity::class,       // v14
        StockMovementEntity::class  // v19
    ],
    version = 19,
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
    abstract fun saldoGuardadoDao(): SaldoGuardadoDao
    abstract fun empresaDao(): EmpresaDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Additive migration. Never infer movements from old sales: their stock may already be applied.
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS stock_movements (
                        idMovimiento TEXT NOT NULL PRIMARY KEY,
                        idUsuario TEXT NOT NULL,
                        idRecibo TEXT NOT NULL,
                        idArticulo TEXT NOT NULL,
                        cantidad INTEGER NOT NULL,
                        creadoEn INTEGER NOT NULL,
                        aplicado INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_aplicado ON stock_movements(aplicado)")
            }
        }


        // ✅ 16 -> 17: añade columna 'descontarDeCaja' a LineasGastos
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE LineasGastos ADD COLUMN descontarDeCaja INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        // ✅ 17 -> 18: Verificación de columnas 'activo' en tablas críticas
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Garantiza que las tablas principales tienen la columna ACTIVO (si no existiera)
                ensureColumnExists(db, "lineas_venta", "activo", "INTEGER NOT NULL DEFAULT 1")
                ensureColumnExists(db, "recibos", "activo", "INTEGER NOT NULL DEFAULT 1")
                ensureColumnExists(db, "LineasGastos", "activo", "INTEGER NOT NULL DEFAULT 1")
            }

            private fun ensureColumnExists(
                db: SupportSQLiteDatabase,
                table: String,
                column: String,
                definition: String
            ) {
                val cursor = db.query("PRAGMA table_info($table)")
                val exists = cursor.use {
                    var found = false
                    while (it.moveToNext()) {
                        if (it.getString(it.getColumnIndexOrThrow("name")) == column) {
                            found = true; break
                        }
                    }
                    found
                }
                if (!exists) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
                }
            }
        }

        private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(table)
            ).use { c -> return c.moveToFirst() }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marketsales.db"
                )
                    .addMigrations(
                        //MIGRATION_10_11,
                        //MIGRATION_11_12,
                        //MIGRATION_12_13,
                        //MIGRATION_13_14,
                        //MIGRATION_14_15,
                        //MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19
                    )
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
//import es.nuskysoftware.marketsales.data.local.dao.*
//import es.nuskysoftware.marketsales.data.local.entity.*
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
//        LineaGastoEntity::class,
//        SaldoGuardadoEntity::class, // v12
//        EmpresaEntity::class        // v14
//    ],
//    version = 17,                  // ⬅️ SUBE A 17
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
//    abstract fun lineasGastosDao(): LineasGastosDao
//    abstract fun saldoGuardadoDao(): SaldoGuardadoDao
//    abstract fun empresaDao(): EmpresaDao
//
//    companion object {
//        @Volatile private var INSTANCE: AppDatabase? = null
//
//        // 10 -> 11: añade formaPago a LineasGastos
//        private val MIGRATION_10_11 = object : Migration(10, 11) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL(
//                    "ALTER TABLE LineasGastos ADD COLUMN formaPago TEXT NOT NULL DEFAULT 'efectivo'"
//                )
//            }
//        }
//
//        // 11 -> 12: crea saldos_guardados
//        private val MIGRATION_11_12 = object : Migration(11, 12) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL(
//                    """
//                    CREATE TABLE IF NOT EXISTS saldos_guardados(
//                        idRegistro TEXT NOT NULL PRIMARY KEY,
//                        idUsuario TEXT NOT NULL,
//                        idMercadilloOrigen TEXT NOT NULL,
//                        fechaMercadillo TEXT NOT NULL,
//                        lugarMercadillo TEXT NOT NULL,
//                        organizadorMercadillo TEXT NOT NULL,
//                        horaInicioMercadillo TEXT NOT NULL,
//                        saldoInicialGuardado REAL NOT NULL,
//                        consumido INTEGER NOT NULL DEFAULT 0,
//                        version INTEGER NOT NULL DEFAULT 1,
//                        lastModified INTEGER NOT NULL,
//                        sincronizadoFirebase INTEGER NOT NULL DEFAULT 0,
//                        notas TEXT
//                    )
//                    """.trimIndent()
//                )
//            }
//        }
//
//        // 12 -> 13: no-op (sin cambios)
//        private val MIGRATION_12_13 = object : Migration(12, 13) {
//            override fun migrate(db: SupportSQLiteDatabase) { /* no-op */ }
//        }
//
//        // 13 -> 14: introduce tabla 'empresa'
//        private val MIGRATION_13_14 = object : Migration(13, 14) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                val hasEmpresa = tableExists(db, "empresa")
//                val hasEmpresas = tableExists(db, "empresas")
//
//                if (!hasEmpresa && hasEmpresas) {
//                    db.execSQL("ALTER TABLE empresas RENAME TO empresa")
//                }
//
//                db.execSQL(
//                    """
//                    CREATE TABLE IF NOT EXISTS empresa(
//                        id INTEGER NOT NULL PRIMARY KEY,
//                        razonSocial TEXT NOT NULL,
//                        nombre TEXT NOT NULL,
//                        nif TEXT NOT NULL,
//                        direccion TEXT NOT NULL,
//                        poblacion TEXT NOT NULL,
//                        provincia TEXT NOT NOT NULL,
//                        pais TEXT NOT NULL,
//                        codigoPostal TEXT NOT NULL,
//                        version INTEGER NOT NULL,
//                        lastModified INTEGER NOT NULL,
//                        sincronizadoFirebase INTEGER NOT NULL
//                    )
//                    """.trimIndent()
//                )
//            }
//        }
//
//        // 14 -> 15: añade campos de sincronización a recibos, lineas_venta y LineasGastos
//        private val MIGRATION_14_15 = object : Migration(14, 15) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL("ALTER TABLE recibos ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
//                db.execSQL("ALTER TABLE recibos ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
//                db.execSQL("ALTER TABLE recibos ADD COLUMN sincronizadoFirebase INTEGER NOT NULL DEFAULT 0")
//
//                db.execSQL("ALTER TABLE lineas_venta ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
//                db.execSQL("ALTER TABLE lineas_venta ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
//                db.execSQL("ALTER TABLE lineas_venta ADD COLUMN sincronizadoFirebase INTEGER NOT NULL DEFAULT 0")
//
//                db.execSQL("ALTER TABLE LineasGastos ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
//                db.execSQL("ALTER TABLE LineasGastos ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
//                db.execSQL("ALTER TABLE LineasGastos ADD COLUMN sincronizadoFirebase INTEGER NOT NULL DEFAULT 0")
//            }
//        }
//
//        // 15 -> 16: añade columna 'activo' a recibos, lineas_venta y LineasGastos
//        private val MIGRATION_15_16 = object : Migration(15, 16) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL("ALTER TABLE recibos ADD COLUMN activo INTEGER NOT NULL DEFAULT 1")
//                db.execSQL("ALTER TABLE lineas_venta ADD COLUMN activo INTEGER NOT NULL DEFAULT 1")
//                db.execSQL("ALTER TABLE LineasGastos ADD COLUMN activo INTEGER NOT NULL DEFAULT 1")
//            }
//        }
//
//        // ✅ 16 -> 17: añade columna 'descontarDeCaja' a LineasGastos
//        private val MIGRATION_16_17 = object : Migration(16, 17) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL(
//                    "ALTER TABLE LineasGastos ADD COLUMN descontarDeCaja INTEGER NOT NULL DEFAULT 1"
//                )
//            }
//        }
//
//        private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
//            db.query(
//                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
//                arrayOf(table)
//            ).use { c -> return c.moveToFirst() }
//        }
//
//        fun getDatabase(context: Context): AppDatabase =
//            INSTANCE ?: synchronized(this) {
//                Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "marketsales.db"
//                )
//                    .addMigrations(
//                        MIGRATION_10_11,
//                        MIGRATION_11_12,
//                        MIGRATION_12_13,
//                        MIGRATION_13_14,
//                        MIGRATION_14_15,
//                        MIGRATION_15_16,
//                        MIGRATION_16_17   // ⬅️ AÑADIR AQUÍ
//                    )
//                    .fallbackToDestructiveMigrationOnDowngrade()
//                    .build()
//                    .also { INSTANCE = it }
//            }
//    }
//}
//
