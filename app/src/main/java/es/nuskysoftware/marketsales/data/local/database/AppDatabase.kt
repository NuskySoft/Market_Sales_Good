// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/AppDatabase.kt
package es.nuskysoftware.marketsales.data.local.database

// Responsabilidad: Configuración de Room, entidades, DAO y migraciones registradas.

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
    version = 20,
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

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE empresa RENAME TO empresa_legacy")
                db.execSQL("CREATE TABLE empresa (id INTEGER NOT NULL, userId TEXT NOT NULL PRIMARY KEY, nif TEXT NOT NULL, nombre TEXT NOT NULL, razonSocial TEXT NOT NULL, direccion TEXT NOT NULL, poblacion TEXT NOT NULL, codigoPostal TEXT NOT NULL, provincia TEXT NOT NULL, pais TEXT NOT NULL, version INTEGER NOT NULL, lastModified INTEGER NOT NULL, sincronizadoFirebase INTEGER NOT NULL)")
                db.execSQL("INSERT INTO empresa SELECT e.id, c.usuarioLogueado, e.nif, e.nombre, e.razonSocial, e.direccion, e.poblacion, e.codigoPostal, e.provincia, e.pais, e.version, e.lastModified, 0 FROM empresa_legacy e JOIN configuracion c ON c.id = 1 WHERE c.usuarioLogueado IS NOT NULL AND c.usuarioLogueado != '' AND c.usuarioLogueado NOT IN ('demo_local_v2', 'usuario_default')")
                // Preserve unowned legacy data privately; never attach it to another account.
                db.execSQL("ALTER TABLE articulos ADD COLUMN stockBase INTEGER")
                db.execSQL("UPDATE articulos SET stockBase = stock")
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN sincronizadoFirebase INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN incorporadoEnBase INTEGER NOT NULL DEFAULT 1")
                // Historical 2.3 movements are already reflected in the baseline, not uploaded again.
                db.execSQL("ALTER TABLE saldos_guardados ADD COLUMN eliminado INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saldos_guardados ADD COLUMN idMercadilloDestino TEXT")
                db.execSQL("ALTER TABLE mercadillos ADD COLUMN fechaEpochDay INTEGER")
                db.execSQL("ALTER TABLE saldos_guardados ADD COLUMN fechaEpochDay INTEGER")
                for ((table, field) in listOf("mercadillos" to "fecha", "saldos_guardados" to "fechaMercadillo")) {
                    db.query("SELECT rowid, $field FROM $table").use { c ->
                        while (c.moveToNext()) {
                            val day = es.nuskysoftware.marketsales.utils.CalendarDates.epochDay(c.getString(1))
                            db.execSQL("UPDATE $table SET fechaEpochDay = ? WHERE rowid = ?", arrayOf(day, c.getLong(0)))
                        }
                    }
                }
            }
        }

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
                        MIGRATION_18_19,
                        MIGRATION_19_20
                    )
                    .addCallback(BusinessStorageCallback())
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
