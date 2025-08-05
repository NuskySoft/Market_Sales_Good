// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/AppDatabase.kt
package es.nuskysoftware.marketsales.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.BuildConfig
import es.nuskysoftware.marketsales.data.local.dao.ArticuloDao
import es.nuskysoftware.marketsales.data.local.dao.CategoriaDao
import es.nuskysoftware.marketsales.data.local.dao.ConfiguracionDao
import es.nuskysoftware.marketsales.data.local.dao.LineasVentaDao
import es.nuskysoftware.marketsales.data.local.dao.MercadilloDao
import es.nuskysoftware.marketsales.data.local.dao.RecibosDao
import es.nuskysoftware.marketsales.data.local.dao.UserDao
import es.nuskysoftware.marketsales.data.local.entity.ArticuloEntity
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import es.nuskysoftware.marketsales.data.local.entity.ConfiguracionEntity
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.local.entity.ReciboEntity
import es.nuskysoftware.marketsales.data.local.entity.UserEntity

@Database(
    entities = [
        ConfiguracionEntity::class,
        MercadilloEntity::class,
        UserEntity::class,
        CategoriaEntity::class,
        ArticuloEntity::class,
        ReciboEntity::class,
        LineaVentaEntity::class
    ],
    version = 9, // ⬆️ nunca bajar versión; antes estaba en 7 y la BD del dispositivo en 8
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ➕ Migración NO-OP de 7 → 8 (ajústala si realmente cambiaste el esquema entre esas versiones)
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op. Si en tu proyecto hubo cambios reales 7→8, añádelos aquí con ALTER TABLE…
                // Ejemplo:
                // db.execSQL("ALTER TABLE configuracion ADD COLUMN usuarioLogueado TEXT NOT NULL DEFAULT 'usuario_default'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marketsales_database_v10"
                )
                    // ✅ Registra aquí TODAS tus migraciones conocidas en orden
                    .addMigrations(
                        MIGRATION_7_8,        // 7 → 8
                        Migrations.M8_TO_M9   // 8 → 9 (flags offline-first en Recibos/Líneas)
                    )

                // 🔧 Solo en DEBUG: si instalas por error un APK más viejo, evita el crash por downgrade
                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigrationOnDowngrade()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE = null
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
//import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
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
//        LineaVentaEntity::class
//    ],
//    version = 9, // ⬆️ nunca bajar versión; antes estaba en 7 y la BD del dispositivo en 8
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
//                        Migrations.M8_TO_M9, // la que ya usabas
//                        MIGRATION_7_8        // nueva para evitar el crash 8→7
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
