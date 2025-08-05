// app/src/main/java/es/nuskysoftware/marketsales/data/local/database/Migrations.kt
package es.nuskysoftware.marketsales.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    // Migración de esquema: versión 6 -> 7
    // Crea las tablas 'recibos' y 'lineas_venta' sin tocar lo existente.
    val M6_TO_M7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {

            // === Tabla RECIBOS (según ReciboEntity) ===
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recibos (
                    idRecibo TEXT NOT NULL PRIMARY KEY,
                    idMercadillo TEXT NOT NULL,
                    idUsuario TEXT NOT NULL,
                    fechaHora INTEGER NOT NULL,
                    metodoPago TEXT NOT NULL,
                    totalTicket REAL NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'COMPLETADO'
                )
                """.trimIndent()
            )
            // 8 -> 9 (si no hubo cambios de esquema reales entre 8 y 9, es NO-OP)
            val M8_TO_M9 = object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // No-Op. Si en tu proyecto real hubo cambios en v9, añádelos aquí (ALTER TABLE ...).
                }
            }

            // 9 -> 10: crea la tabla LineasGastos
            val M9_TO_M10 = object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
            CREATE TABLE IF NOT EXISTS LineasGastos(
                idMercadillo TEXT NOT NULL,
                idUsuario TEXT NOT NULL,
                numeroLinea TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                importe REAL NOT NULL,
                fechaHora INTEGER NOT NULL,
                PRIMARY KEY(idMercadillo, numeroLinea)
            )
            """.trimIndent()
                    )
                }
            }
            // Opcionalmente índices útiles:
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recibos_mercadillo ON recibos(idMercadillo)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recibos_usuario ON recibos(idUsuario)")

            // === Tabla LINEAS_VENTA (según LineaVentaEntity) ===
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS lineas_venta (
                    idLinea TEXT NOT NULL PRIMARY KEY,
                    idRecibo TEXT NOT NULL,
                    idMercadillo TEXT NOT NULL,
                    idUsuario TEXT NOT NULL,
                    numeroLinea INTEGER NOT NULL,
                    tipoLinea TEXT NOT NULL,
                    descripcion TEXT NOT NULL,
                    idProducto TEXT,
                    cantidad INTEGER NOT NULL,
                    precioUnitario REAL NOT NULL,
                    subtotal REAL NOT NULL,
                    idLineaOriginalAbonada TEXT
                )
                """.trimIndent()
            )

            // Índices recomendados:
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_recibo ON lineas_venta(idRecibo)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_mercadillo ON lineas_venta(idMercadillo)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_usuario ON lineas_venta(idUsuario)")
        }
        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        """
                    ALTER TABLE lineas_gastos
                    ADD COLUMN formaPago TEXT NOT NULL DEFAULT 'efectivo'
                    """.trimIndent()
                    )
                } catch (_: Exception) {
                }
            }
        }

    }
}
