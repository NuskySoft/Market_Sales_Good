package es.nuskysoftware.marketsales.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.*
import es.nuskysoftware.marketsales.receipts.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Pruebas instrumentadas de 2.7. No utilizan cuentas de correo reales ni servidores externos. */
@RunWith(AndroidJUnit4::class)
class Receipt27Test {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migrationPreservesLegacyAndCreatesQueue(){
        // Base SQLite mínima de la versión anterior: prueba la migración real, no simula un downgrade de Room.
        val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null).callback(object:SupportSQLiteOpenHelper.Callback(21){
                override fun onCreate(db:SupportSQLiteDatabase){
                    db.execSQL("CREATE TABLE recibos(idRecibo TEXT PRIMARY KEY, totalTicket REAL NOT NULL)")
                    db.execSQL("INSERT INTO recibos VALUES('legacy',12.50)")
                }
                override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int)=Unit
            }).build())
        try{
            val db=helper.writableDatabase
            AppDatabase.MIGRATION_21_22.migrate(db)
            db.query("SELECT totalTicket,pdfOriginal FROM recibos WHERE idRecibo='legacy'").use{
                assertTrue(it.moveToFirst());assertEquals(12.50,it.getDouble(0),0.0);assertEquals(0,it.getInt(1))
            }
            db.query("SELECT name FROM sqlite_master WHERE name IN ('receipt_documents','receipt_mail')").use{assertEquals(2,it.count)}
        }finally{helper.close()}
    }

    @Test fun pendingMailIsScopedAndInterruptedMailIsNotRetriedBlindly()=runBlocking{
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try{
            val dao=db.receiptDocumentDao()
            val request=ReceiptMailEntity("one","q","r","client@example.com",1,sender="shop@example.com",device="a")
            dao.enqueue(request)
            assertEquals(1,dao.pending("one","a").size)
            assertTrue(dao.pending("one","b").isEmpty())
            assertTrue(dao.pending("two","a").isEmpty())
            assertEquals(1,dao.activeRequest("one","a","r","CLIENT@example.com"))
            assertEquals(1,dao.transition("one","q","PENDING","SENDING",""))
            assertEquals(0,dao.transition("one","q","PENDING","SENDING",""))
            dao.recoverInterrupted("one","a")
            assertTrue(dao.pending("one","a").isEmpty())
            assertEquals(1,dao.activeRequest("one","a","r","client@example.com"))
            assertEquals(1,dao.retry("one","q","client@example.com"))
            assertEquals(1,dao.transition("one","q","PENDING","SENDING",""))
            assertEquals(1,dao.transition("one","q","SENDING","SENT",""))
            assertEquals(0,dao.activeRequest("one","a","r","client@example.com"))
            assertEquals(0,dao.retry("one","q","other@example.com"))
        }finally{db.close()}
    }

    @Test fun originalPdfIsImmutableAndPaged(){
        val owner="test-"+UUID.randomUUID()
        val receipt=ReciboEntity(idRecibo="RC-test-ES",idMercadillo="market",idUsuario=owner,fechaHora=1L,metodoPago="EFECTIVO",totalTicket=200.0,pdfOriginal=true)
        val lines=(1..100).map{n->LineaVentaEntity(idLinea=n.toString(),idRecibo=receipt.idRecibo,idMercadillo="market",idUsuario=owner,numeroLinea=n,tipoLinea="MANUAL",descripcion="Concepto de prueba",cantidad=1,precioUnitario=2.0,subtotal=2.0)}
        val snapshot=ReceiptFiles.snapshot(receipt,lines,null,"Prueba","€ Euro","es","","")
        val doc=ReceiptDocumentEntity(owner,receipt.idRecibo,snapshot)
        try{
            val file=ReceiptFiles.render(context,doc)
            assertTrue(file.isFile);val original=file.readBytes()
            assertTrue(String(original.copyOfRange(0,5),Charsets.US_ASCII)=="%PDF-")
            assertArrayEquals(original,ReceiptFiles.render(context,doc.copy(snapshot="{}")).readBytes())
            val fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            android.graphics.pdf.PdfRenderer(fd).use{assertTrue(it.pageCount>1)}
        }finally{ReceiptFiles.folder(context,owner).deleteRecursively()}
    }
}
