package es.nuskysoftware.marketsales.receipts

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.DocumentsContract
import android.util.AtomicFile
import android.util.Base64
import androidx.core.content.FileProvider
import es.nuskysoftware.marketsales.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Archivos privados por cuenta; nunca se envían a Firebase. */
object ReceiptFiles {
    fun key(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    fun folder(context: Context, owner: String): File =
        File(context.filesDir, "receipts/" + key(owner)).apply { mkdirs() }
    fun file(context: Context, owner: String, id: String) = File(folder(context, owner), key(id) + ".pdf")
    /** Nombre reconocible, con identificador de cuenta no reversible y recibo único. */
    fun externalName(owner:String,id:String):String =
        "MarketSales-" + key(owner).take(16) + "-" + id.replace(Regex("[^A-Za-z0-9_-]"),"_") + ".pdf"
    fun logo(context: Context, owner: String) = File(folder(context, owner), "logo.png")
    fun uri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    /** Captura datos comerciales originales; no consulta el catálogo durante un reintento. */
    fun snapshot(receipt: ReciboEntity, lines: List<LineaVentaEntity>, company: EmpresaEntity?,
                 market: String, currency: String, language: String, logo: String, original: String): String {
        val fields = company?.let { listOf(it.nombre, it.razonSocial, it.nif, it.direccion,
            listOf(it.codigoPostal, it.poblacion, it.provincia, it.pais).filter { s -> s.isNotBlank() }.joinToString(" ")) }
            ?: emptyList()
        return JSONObject().put("id", receipt.idRecibo).put("owner", receipt.idUsuario)
            .put("date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).format(Date(receipt.fechaHora)))
            .put("market", market).put("currency", currency).put("language", language)
            .put("method", receipt.metodoPago).put("total", receipt.totalTicket)
            .put("company", JSONArray(fields.filter { it.isNotBlank() })).put("logo", logo)
            .put("original", original).put("refund", lines.any { it.idLineaOriginalAbonada != null })
            .put("lines", JSONArray(lines.map { JSONObject().put("description", it.descripcion)
                .put("quantity", it.cantidad).put("price", it.precioUnitario).put("amount", it.subtotal) })).toString()
    }

    /** Genera A4 paginado con escritura atómica: nunca reemplaza un original ya creado. */
    @Synchronized
    fun render(context: Context, doc: ReceiptDocumentEntity): File {
        val output = file(context, doc.owner, doc.receiptId)
        if (output.isFile && output.length() > 0) return output
        val s = JSONObject(doc.snapshot)
        val en = s.optString("language") == "en"
        fun tr(es: String, english: String) = if (en) english else es
        val money = s.getString("currency")
        fun amount(value: Double) = String.format(if (en) Locale.US else Locale("es", "ES"), "%.2f", value)
        val pdf = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun line(text: String, x: Float = 40f, size: Float = 11f, bold: Boolean = false) {
            paint.color = Color.rgb(32, 38, 45); paint.textSize = size
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            page!!.canvas.drawText(text, x, y, paint)
        }
        fun finish() {
            if (page == null) return
            y = 800f; line("Market Sales  ·  " + pageNumber, size = 10f)
            pdf.finishPage(page!!); page = null
        }
        fun next() {
            finish(); pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 45f
            line(if (s.getBoolean("refund")) tr("RECIBO DE ABONO","REFUND RECEIPT") else tr("RECIBO DE VENTA","SALES RECEIPT"), size=19f,bold=true)
            y += 22; line(s.getString("id"), size=10f); y += 17; line(s.getString("date")); y += 20
        }
        fun ensure(height: Float) { if (y + height > 770f) next() }
        fun wrapped(text: String, max: Float = 510f) {
            paint.textSize=11f; paint.typeface=Typeface.DEFAULT
            for (paragraph in text.split('\n')) {
                var rest = paragraph
                while (rest.isNotEmpty()) {
                    val count = paint.breakText(rest, true, max, null).coerceAtLeast(1)
                    val cut = if (count < rest.length) rest.lastIndexOf(' ', count - 1).takeIf { it > 0 } ?: count else count
                    ensure(16f); line(rest.take(cut)); y += 16; rest = rest.drop(cut).trimStart()
                }
            }
        }
        try {
            next()
            val logoBytes = s.optString("logo")
            if (logoBytes.isNotBlank()) {
                runCatching {
                    val bytes = Base64.decode(logoBytes, Base64.NO_WRAP)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { image ->
                        val scale = minOf(115f/image.width, 72f/image.height)
                        page!!.canvas.drawBitmap(image, null, RectF(440f, 35f, 440f+image.width*scale,35f+image.height*scale), paint)
                        image.recycle()
                    }
                }
            }
            val company = s.getJSONArray("company")
            for (i in 0 until company.length()) wrapped(company.getString(i))
            wrapped(s.getString("market"))
            if (s.optString("original").isNotBlank()) wrapped(tr("Recibo original: ","Original receipt: ") + s.getString("original"))
            y += 14
            val lines = s.getJSONArray("lines")
            for (i in 0 until lines.length()) {
                val l = lines.getJSONObject(i); ensure(50f)
                wrapped(l.getString("description"))
                line(l.getInt("quantity").toString() + " × " + amount(l.getDouble("price")) + "  =  " + amount(l.getDouble("amount")))
                y += 19
                paint.color=Color.LTGRAY; page!!.canvas.drawLine(40f,y,555f,y,paint); y += 15
            }
            ensure(92f); y+=10; line(tr("TOTAL ","TOTAL ") + amount(s.getDouble("total")) + " " + money, size=17f,bold=true)
            y+=24; wrapped(tr("Método de pago: ","Payment method: ") + s.getString("method"))
            y+=15; wrapped(tr("Gracias por su compra","Thank you for your purchase"))
            finish()
            val atomic = AtomicFile(output)
            val stream = atomic.startWrite()
            try { pdf.writeTo(stream); atomic.finishWrite(stream) }
            catch (e: Exception) { atomic.failWrite(stream); throw e }
            return output
        } finally { if (page != null) finish(); pdf.close() }
    }

    /** Copia a un proveedor SAF elegido por el usuario; no sobrescribe un original distinto. */
    fun external(context: Context, owner: String, id: String, tree: Uri, source: File?, restore: Boolean): File? {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = externalName(owner,id)
        var existing: Uri? = null
        resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null,null,null)?.use { c ->
            while(c.moveToNext()) if (c.getString(1)==name) {
                existing=DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0)); break
            }
        }
        if (restore) return existing?.let { restoreFromUri(context,owner,id,it) }
        require(source != null)
        if(existing != null) {
            // Si el proveedor ya contiene una copia idéntica no se crea otra.
            val same = resolver.openInputStream(existing!!)?.use { input ->
                source.inputStream().use { local ->
                    val a=ByteArray(8192); val b=ByteArray(8192)
                    var equal=true
                    while(true){val n=input.readNBytesCompat(a);val m=local.readNBytesCompat(b)
                        if(n!=m || !a.copyOf(n).contentEquals(b.copyOf(m))){equal=false;break}
                        if(n==0)break
                    };equal
                }
            } ?: false
            check(same) { "Existe una copia distinta; no se sobrescribe" }
            return source
        }
        val destination = DocumentsContract.createDocument(resolver,parent,"application/pdf",name) ?: error("No se pudo crear copia")
        try { resolver.openOutputStream(destination,"w")?.use { out -> source.inputStream().use { it.copyTo(out) } } ?: error("Sin permiso de escritura") }
        catch(e:Exception){runCatching{DocumentsContract.deleteDocument(resolver,destination)};throw e}
        return source
    }
    /** Valida cabecera y estructura PDF antes de promover la copia; no sustituye un original existente. */
    @Synchronized
    fun restoreFromUri(context:Context,owner:String,id:String,uri:Uri):File {
        val target=file(context,owner,id)
        if(target.isFile && target.length()>0)return target
        val temporary=File.createTempFile("restore-", ".pdf", folder(context,owner))
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { out ->
                    val header=ByteArray(5);var n=0
                    while(n<5){val read=input.read(header,n,5-n);if(read<0)break;n+=read}
                    require(n==5 && String(header,Charsets.US_ASCII)=="%PDF-")
                    out.write(header)
                    val buffer=ByteArray(8192);var total=5L
                    while(true){val count=input.read(buffer);if(count<0)break;total+=count;require(total<=50_000_000L);out.write(buffer,0,count)}
                }
            } ?: error("No se puede leer el archivo")
            // PdfRenderer adquiere la propiedad del descriptor al construirse.
            val fd=android.os.ParcelFileDescriptor.open(temporary,android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer=try { android.graphics.pdf.PdfRenderer(fd) } catch(e:Exception){fd.close();throw e}
            renderer.use { check(it.pageCount>0) }
            val atomic=AtomicFile(target);val out=atomic.startWrite()
            try {temporary.inputStream().use{it.copyTo(out)};atomic.finishWrite(out)}
            catch(e:Exception){atomic.failWrite(out);throw e}
            return target
        } finally {temporary.delete()}
    }

    private fun java.io.InputStream.readNBytesCompat(buffer: ByteArray): Int {
        var count=0
        while(count<buffer.size){val n=read(buffer,count,buffer.size-count);if(n<=0)break;count+=n}
        return count
    }
}
