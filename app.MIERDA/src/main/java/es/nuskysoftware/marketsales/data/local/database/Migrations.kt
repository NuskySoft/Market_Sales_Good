// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/Migrations.kt
package es.nuskysoftware.marketsales.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    // (Ya existente en tu repo) Ejemplo: 6 -> 7 crea tablas recibos/lineas_venta…
    // val M6_TO_M7 = …

    // (Ya existente) 7 -> 8 … si procede
    // val M7_TO_M8 = …

    // ===== NUEVA: 8 -> 9 añade flags offline-first en Recibos y Líneas =====
    val M8_TO_M9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Recibos
            db.execSQL("ALTER TABLE recibos ADD COLUMN sincronizadoFirebase INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recibos ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE recibos ADD COLUMN lastModified INTEGER NOT NULL DEFAULT (strftime('%s','now')*1000)")
            db.execSQL("ALTER TABLE recibos ADD COLUMN syncError TEXT")

            // Líneas
            db.execSQL("ALTER TABLE lineas_venta ADD COLUMN sincronizadoFirebase INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE lineas_venta ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE lineas_venta ADD COLUMN lastModified INTEGER NOT NULL DEFAULT (strftime('%s','now')*1000)")
            db.execSQL("ALTER TABLE lineas_venta ADD COLUMN syncError TEXT")

            // Índices recomendados
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recibos_sync ON recibos(sincronizadoFirebase)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_sync ON lineas_venta(sincronizadoFirebase)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_recibo ON lineas_venta(idRecibo)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_mercadillo ON lineas_venta(idMercadillo)")
        }
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/Migrations.kt
//package es.nuskysoftware.marketsales.data.local.database
//
//import androidx.room.migration.Migration
//import androidx.sqlite.db.SupportSQLiteDatabase
//
//object Migrations {
//
//    // Migración de esquema: versión 6 -> 7
//    // Crea las tablas 'recibos' y 'lineas_venta' sin tocar lo existente.
//    val M6_TO_M7 = object : Migration(6, 7) {
//        override fun migrate(db: SupportSQLiteDatabase) {
//
//            // === Tabla RECIBOS (según ReciboEntity) ===
//            db.execSQL(
//                """
//                CREATE TABLE IF NOT EXISTS recibos (
//                    idRecibo TEXT NOT NULL PRIMARY KEY,
//                    idMercadillo TEXT NOT NULL,
//                    idUsuario TEXT NOT NULL,
//                    fechaHora INTEGER NOT NULL,
//                    metodoPago TEXT NOT NULL,
//                    totalTicket REAL NOT NULL,
//                    estado TEXT NOT NULL DEFAULT 'COMPLETADO'
//                )
//                """.trimIndent()
//            )
//
//            // Opcionalmente índices útiles:
//            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recibos_mercadillo ON recibos(idMercadillo)")
//            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recibos_usuario ON recibos(idUsuario)")
//
//            // === Tabla LINEAS_VENTA (según LineaVentaEntity) ===
//            db.execSQL(
//                """
//                CREATE TABLE IF NOT EXISTS lineas_venta (
//                    idLinea TEXT NOT NULL PRIMARY KEY,
//                    idRecibo TEXT NOT NULL,
//                    idMercadillo TEXT NOT NULL,
//                    idUsuario TEXT NOT NULL,
//                    numeroLinea INTEGER NOT NULL,
//                    tipoLinea TEXT NOT NULL,
//                    descripcion TEXT NOT NULL,
//                    idProducto TEXT,
//                    cantidad INTEGER NOT NULL,
//                    precioUnitario REAL NOT NULL,
//                    subtotal REAL NOT NULL,
//                    idLineaOriginalAbonada TEXT
//                )
//                """.trimIndent()
//            )
//
//            // Índices recomendados:
//            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_recibo ON lineas_venta(idRecibo)")
//            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_mercadillo ON lineas_venta(idMercadillo)")
//            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_usuario ON lineas_venta(idUsuario)")
//        }
//    }
//}
