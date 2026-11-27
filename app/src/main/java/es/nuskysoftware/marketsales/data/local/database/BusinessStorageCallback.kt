package es.nuskysoftware.marketsales.data.local.database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

internal class BusinessStorageCallback : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        val monetary = mapOf(
            "articulos" to listOf("precioVenta", "precioCoste"),
            "mercadillos" to listOf("importeSuscripcion", "saldoInicial", "saldoFinal", "arqueoCaja", "totalVentas", "totalGastos", "arqueoMercadillo"),
            "recibos" to listOf("totalTicket"), "lineas_venta" to listOf("precioUnitario", "subtotal"),
            "LineasGastos" to listOf("importe"), "saldos_guardados" to listOf("saldoInicialGuardado")
        )
        // Normalize legacy values once; this is a new local precision correction to synchronize.
        for ((table, columns) in monetary) {
            val condition = columns.joinToString(" OR ") { "$it != (ROUND($it * 100, 0) / 100.0)" }
            val assignments = columns.joinToString { "$it = (ROUND($it * 100, 0) / 100.0)" }
            db.execSQL("UPDATE `$table` SET $assignments, sincronizadoFirebase = 0, version = version + 1, lastModified = ? WHERE $condition", arrayOf(System.currentTimeMillis()))
        }
        for ((table, columns) in monetary) for (event in listOf("INSERT", "UPDATE")) {
            val condition = columns.joinToString(" OR ") { "NEW.$it != (ROUND(NEW.$it * 100, 0) / 100.0)" }
            val assignments = columns.joinToString { "$it = (ROUND(NEW.$it * 100, 0) / 100.0)" }
            db.execSQL("CREATE TRIGGER IF NOT EXISTS money_${table}_$event AFTER $event ON `$table` WHEN $condition BEGIN UPDATE `$table` SET $assignments WHERE rowid = NEW.rowid; END")
        }
        for ((table, field) in listOf("mercadillos" to "fecha", "saldos_guardados" to "fechaMercadillo")) {
            val day = "CAST(julianday(substr(NEW.$field,7,4)||'-'||substr(NEW.$field,4,2)||'-'||substr(NEW.$field,1,2))-2440587.5 AS INTEGER)"
            for (event in listOf("INSERT", "UPDATE OF $field")) {
                val name = event.substringBefore(' ')
                db.execSQL("CREATE TRIGGER IF NOT EXISTS calendar_${table}_$name AFTER $event ON `$table` BEGIN UPDATE `$table` SET fechaEpochDay = $day WHERE rowid = NEW.rowid; END")
            }
        }
    }
}
