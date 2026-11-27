package es.nuskysoftware.marketsales.receipts

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.ReceiptMailEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.DemoAccess
import es.nuskysoftware.marketsales.work.ReceiptWorker
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Acciones explícitas de recibos: comprueban modalidad y propietario también fuera de la interfaz. */
class ReceiptService(private val context: Context) {
    private val db=AppDatabase.getDatabase(context)
    private val dao=db.receiptDocumentDao()
    fun owner():String=ConfigurationManager.getCurrentUserId() ?: error("No hay sesión")
    fun settings(owner:String)=ReceiptSettings(context,owner)
    fun allowed(owner:String)=ConfigurationManager.getCurrentUserId()==owner &&
        ConfigurationManager.esPremiumReady.value && ConfigurationManager.getIsPremium() && !DemoAccess.isDemo()
    fun online():Boolean {
        val cm=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true
    }

    /** Recupera un original local o la copia externa; jamás fabrica un PDF a partir de ventas históricas. */
    suspend fun original(owner:String,id:String):File=withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(owner); DemoAccess.requirePremium()
        val receipt=db.recibosDao().obtenerReciboPorId(id,owner) ?: error("Recibo no disponible")
        val local=ReceiptFiles.file(context,owner,id)
        val doc=dao.find(owner,id)
        val result=when {
            local.isFile && local.length()>0 -> local
            doc!=null -> {
                // Si la copia original existe, recuperarla antes de recurrir a la instantánea inmutable.
                val destination=doc.copiedTo ?: settings(owner).folder
                val restored=destination?.let { runCatching { ReceiptFiles.external(context,owner,id,Uri.parse(it),null,true) }.getOrNull() }
                restored ?: ReceiptFiles.render(context,doc)
            }
            receipt.pdfOriginal -> settings(owner).folder?.let { ReceiptFiles.external(context,owner,id,Uri.parse(it),null,true) }
                ?: error("PDF original no disponible")
            else -> error("Esta venta no generó PDF")
        }
        check(allowed(owner));result
    }

    suspend fun enqueue(owner:String,id:String,recipient:String) {
        DemoAccess.requireOwner(owner);DemoAccess.requirePremium()
        require(ReceiptMail.validAddress(recipient))
        // La solicitud se guarda antes de programar ningún trabajo.
        check(settings(owner).mail()!=null){"Configura primero el correo"}
        db.withTransaction {
            DemoAccess.requireOwner(owner);DemoAccess.requirePremium()
            check(db.recibosDao().obtenerReciboPorId(id,owner)!=null)
            val config=settings(owner).mail() ?: error("Correo no configurado")
            val local=ReceiptFiles.file(context,owner,id)
            check(dao.find(owner,id)!=null || local.isFile){"PDF no disponible"}
            // El identificador de una solicitud es independiente del recibo para permitir reenvíos voluntarios.
            // Se conserva además la dirección autorizada para no enviar con otra cuenta tras una edición.
            val device=ReceiptSettings.deviceId(context)
            check(dao.activeRequest(owner,device,id,recipient)==0) { "Ya existe una solicitud pendiente o incierta" }
            dao.enqueue(ReceiptMailEntity(owner,UUID.randomUUID().toString(),id,recipient,System.currentTimeMillis(),sender=config.address,device=device))
        }
        runCatching { ReceiptWorker.requestMail(context) }
    }

    /** Exportación explícita: permite destinos que no soportan la selección de carpetas. */
    suspend fun export(owner:String,id:String,destination:Uri)=withContext(Dispatchers.IO) {
        val pdf=original(owner,id)
        check(allowed(owner))
        context.contentResolver.openOutputStream(destination,"w")?.use { out -> pdf.inputStream().use { it.copyTo(out) } }
            ?: error("No se pudo exportar")
    }

    /** Recuperación manual por nombre asociado a cuenta y recibo, sin reconstruir datos históricos. */
    suspend fun importCopy(owner:String,id:String,uri:Uri):File=withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(owner);DemoAccess.requirePremium()
        val receipt=db.recibosDao().obtenerReciboPorId(id,owner) ?: error("Recibo no disponible")
        check(receipt.pdfOriginal || dao.find(owner,id)!=null)
        val name=context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c -> if(c.moveToFirst())c.getString(0) else null }
        require(name==ReceiptFiles.externalName(owner,id)){"La copia no corresponde al recibo seleccionado"}
        val result=ReceiptFiles.restoreFromUri(context,owner,id,uri)
        check(allowed(owner));result
    }

    /** Comparte un URI de solo lectura con la aplicación elegida; no confirma entrega. */
    fun open(owner:String,file:File,whatsapp:Boolean=false) {
        check(allowed(owner))
        val uri=ReceiptFiles.uri(context,file)
        val intent=if(whatsapp) Intent(Intent.ACTION_SEND).apply {
            type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);setPackage("com.whatsapp")
        } else Intent(Intent.ACTION_VIEW).apply {setDataAndType(uri,"application/pdf")}
        intent.clipData=ClipData.newRawUri("PDF",uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {context.startActivity(intent)}
        catch(e:android.content.ActivityNotFoundException) {
            if(!whatsapp)throw e
            context.startActivity(Intent(intent).setPackage("com.whatsapp.w4b"))
        }
    }
}
