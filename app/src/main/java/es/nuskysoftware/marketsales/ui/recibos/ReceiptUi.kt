package es.nuskysoftware.marketsales.ui.recibos

import android.content.Intent
import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.tasks.await
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.AtomicFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.receipts.*
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.DemoAccess
import es.nuskysoftware.marketsales.utils.safePopBackStack
import es.nuskysoftware.marketsales.work.ReceiptWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Texto breve bilingüe de los componentes nuevos, según la preferencia actual. */
@Composable private fun language():Boolean = ConfigurationManager.idioma.collectAsState().value=="en"
private fun t(en:Boolean,es:String,english:String)=if(en)english else es

/** Preferencias por dispositivo, sin enviar documentos ni permisos SAF a Firebase. */
@Composable fun ReceiptPreferences() {
    val context=LocalContext.current; val en=language()
    val owner by ConfigurationManager.usuarioLogueado.collectAsState()
    val uid=owner.orEmpty();val service=remember { ReceiptService(context) }
    val settings=remember(uid){service.settings(uid)}
    var ask by remember(uid){mutableStateOf(settings.ask)}
    var folder by remember(uid){mutableStateOf(settings.folder)}
    var message by remember{mutableStateOf("")}
    val copies by remember(uid,folder){AppDatabase.getDatabase(context).receiptDocumentDao().pendingCopies(uid,folder.orEmpty())}.collectAsState(initial=0)
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->
        if(uri!=null && service.allowed(uid)) {
            try {
                context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                settings.folder=uri.toString();folder=uri.toString()
                ReceiptWorker.requestCopy(context)
            }catch(e:Exception){message=t(en,"La carpeta no permite acceso persistente","Folder does not allow persistent access")}
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(t(en,"Recibos PDF","PDF receipts"),style=MaterialTheme.typography.titleMedium)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(t(en,"Solicitar siempre envío de recibo","Always ask to send receipt"),Modifier.weight(1f))
                Switch(ask,{if(service.allowed(uid)){ask=it;settings.ask=it}})
            }
            HorizontalDivider()
            Text(t(en,"Almacenamiento local siempre activo","Local storage always enabled"))
            Text(if(folder!=null)t(en,"Copia externa configurada","External copy configured") else t(en,"Sin copia externa. Si pierde el dispositivo o desinstala la app puede perder los PDF.","No external copy. PDF files may be lost if the device is lost or the app is uninstalled."),style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={picker.launch(null)}) {Text(t(en,"Seleccionar carpeta externa","Choose external folder"))}
            if(folder!=null && copies>0)Text(t(en,"Copias pendientes: ","Pending copies: ")+copies,style=MaterialTheme.typography.bodySmall)
            if(folder!=null)TextButton(onClick={if(service.allowed(uid)){settings.folder=null;folder=null}}){Text(t(en,"Desactivar copia externa","Disable external copy"))}
            Text(t(en,"La nube disponible depende del proveedor instalado. Las copias pendientes se reintentan.","Available cloud folders depend on the installed provider. Pending copies are retried."),style=MaterialTheme.typography.bodySmall)
            if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.error)
        }
    }
}

/** Aviso de datos incompletos, fuera de las pantallas de venta. */
@Composable fun CompanyMissingNotice() {
    val context=LocalContext.current;val en=language();val owner by ConfigurationManager.usuarioLogueado.collectAsState()
    val db=remember{AppDatabase.getDatabase(context)}
    val company by remember(owner){db.empresaDao().getEmpresaFlow(owner.orEmpty())}.collectAsState(initial=null)
    if(company==null || company!!.nombre.isBlank() || company!!.nif.isBlank() || company!!.direccion.isBlank())
        Text(t(en,"Completa los datos de empresa para personalizar tus recibos.","Complete your company details to personalize receipts."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Se presenta una vez al entrar en mercadillos con Premium, nunca al cerrar una venta. */
@Composable fun ReceiptWelcome(nav:NavController) {
    val context=LocalContext.current;val en=language();val owner by ConfigurationManager.usuarioLogueado.collectAsState()
    val premium by ConfigurationManager.esPremium.collectAsState();val ready by ConfigurationManager.esPremiumReady.collectAsState()
    var show by remember(owner){mutableStateOf(false)}
    val uid=owner.orEmpty();val settings=remember(uid){ReceiptSettings(context,uid)}
    LaunchedEffect(uid,premium,ready) {
        if(premium && ready && uid.isNotBlank() && !settings.welcomeShown) {
            val company=withContext(Dispatchers.IO){AppDatabase.getDatabase(context).empresaDao().getEmpresa(uid)}
            if(company==null || company.nombre.isBlank() || company.nif.isBlank() || company.direccion.isBlank())show=true
            else settings.welcomeShown=true
        }
    }
    if(show && premium && ready)AlertDialog(
        onDismissRequest={show=false;settings.welcomeShown=true},
        title={Text(t(en,"Personaliza tus recibos Premium","Personalize your Premium receipts"))},
        text={Text(t(en,"Completa los datos de empresa y, si quieres, añade un logotipo. Podrás hacerlo más adelante en Utilidades.","Complete your company details and optionally add a logo. You can do this later in Utilities."))},
        confirmButton={TextButton(onClick={show=false;settings.welcomeShown=true;nav.navigate("datos_empresa")}){Text(t(en,"Datos de empresa","Company details"))}},
        dismissButton={TextButton(onClick={show=false;settings.welcomeShown=true}){Text(t(en,"Más tarde","Later"))}}
    )
}

/** Reduce imágenes grandes antes de conservar el logotipo; no solicita permisos de galería globales. */
@Composable fun CompanyLogo() {
    val context=LocalContext.current; val en=language();val scope=rememberCoroutineScope()
    val owner by ConfigurationManager.usuarioLogueado.collectAsState();val uid=owner.orEmpty()
    val service=remember{ReceiptService(context)}
    var bitmap by remember(uid){mutableStateOf<Bitmap?>(null)}
    var error by remember{mutableStateOf("")}
    LaunchedEffect(uid){bitmap=withContext(Dispatchers.IO){BitmapFactory.decodeFile(ReceiptFiles.logo(context,uid).absolutePath)}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null && service.allowed(uid))scope.launch {
            try {
                bitmap=withContext(Dispatchers.IO){
                    val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                    context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
                    require(bounds.outWidth>0 && bounds.outHeight>0)
                    var sample=1
                    while(maxOf(bounds.outWidth,bounds.outHeight)/sample>320)sample*=2
                    val image=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})} ?: error("Imagen no válida")
                    check(service.allowed(uid))
                    val atomic=AtomicFile(ReceiptFiles.logo(context,uid));val stream=atomic.startWrite()
                    try{check(image.compress(Bitmap.CompressFormat.PNG,100,stream));atomic.finishWrite(stream)}
                    catch(e:Exception){atomic.failWrite(stream);throw e}
                    image
                };error=""
            }catch(e:CancellationException){throw e}
            catch(e:Exception){error=t(en,"No se pudo guardar el logotipo","Could not save logo")}
        }
    }
    Text(t(en,"Logotipo del recibo (opcional, en este dispositivo)","Receipt logo (optional, on this device)"))
    bitmap?.let{Image(it.asImageBitmap(),contentDescription=t(en,"Logotipo","Logo"),modifier=Modifier.height(72.dp))}
    Row {
        OutlinedButton(onClick={picker.launch("image/*")}){Text(t(en,"Elegir imagen","Choose image"))}
        if(bitmap!=null)TextButton(onClick={if(service.allowed(uid)){ReceiptFiles.logo(context,uid).delete();bitmap=null}}){Text(t(en,"Quitar","Remove"))}
    }
    if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
}

/** Configuración SMTP: guardar únicamente después de enviar una prueba a la cuenta propia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MailSettingsScreen(nav:NavController) {
    val context=LocalContext.current;val en=language();val scope=rememberCoroutineScope()
    val service=remember{ReceiptService(context)};val owner=service.owner();val settings=remember(owner){service.settings(owner)}
    var previous by remember(owner){mutableStateOf(settings.mail())}
    var provider by rememberSaveable(owner){mutableStateOf(previous?.provider?.let{if(it==GmailMail.PROVIDER)"gmail" else it} ?: "gmail")}
    var name by remember{mutableStateOf(previous?.name.orEmpty())}
    var address by remember{mutableStateOf(previous?.address.orEmpty())}
    var host by remember{mutableStateOf(previous?.host ?: "smtp.gmail.com")}
    var port by remember{mutableStateOf(previous?.port?.takeIf{it>0}?.toString() ?: "465")}
    var tls by remember{mutableStateOf(previous?.tls?.takeIf{it.isNotBlank()} ?: "SSL")}
    var user by remember{mutableStateOf(previous?.username.orEmpty())}
    var secret by remember{mutableStateOf("")}
    var message by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
    var mailError by remember{mutableStateOf(false)}
    var code by remember{mutableStateOf<MicrosoftMail.DeviceCode?>(null)}
    var authorized by remember{mutableStateOf<MailConfig?>(previous?.takeIf{it.provider=="outlook"})}
    var authJob by remember{mutableStateOf<kotlinx.coroutines.Job?>(null)}
    // Se conserva únicamente la dirección elegida al volver de la pantalla de consentimiento.
    var chosenGoogle by rememberSaveable(owner){mutableStateOf("")}
    fun saveTest(config:MailConfig) {
        busy=true;message="";mailError=false
        // Separar aceptación SMTP del guardado cifrado: un fallo de Keystore no es un fallo de conexión.
        var accepted=false
        var phase="session"
        scope.launch {
            try {
                val tested=withContext(Dispatchers.IO){
                    DemoAccess.requireOwner(owner);DemoAccess.requirePremium()
                    phase="send"
                    val result=ReceiptMail.send(config,config.address,null,settings,en,false)
                    accepted=true;phase="save"
                    DemoAccess.requireOwner(owner);DemoAccess.requirePremium()
                    settings.save(result);result
                }
                if(tested.provider=="outlook")authorized=tested
                previous=tested;secret="";message=t(en,"Prueba aceptada. Correo guardado.","Test accepted. Mail settings saved.")
                runCatching { ReceiptWorker.requestMail(context) }
            }catch(e:CancellationException){throw e}
            catch(e:Exception){
                mailError=true
                message=when {
                    accepted -> t(en,"El servidor aceptó la prueba, pero no se pudo guardar la configuración cifrada.","The server accepted the test, but encrypted settings could not be saved.")
                    e is MailFailure -> e.message.orEmpty()
                    phase=="session" -> t(en,"La sesión Premium no está disponible. Vuelve a abrir la pantalla.","Premium session unavailable. Reopen this screen.")
                    else -> t(en,"Error interno de correo (","Internal mail error (")+e.javaClass.simpleName+")."
                }
                // Solo fase y tipo: no imprimir excepciones, direcciones, respuestas SMTP ni credenciales.
                android.util.Log.w("MarketSalesMail","Test phase="+phase+" type="+e.javaClass.simpleName)
            }
            finally{busy=false}
        }
    }
    /** Finaliza el consentimiento y solo guarda Gmail si el envío de prueba es aceptado. */
    fun completeGoogle(result:AuthorizationResult) {
        scope.launch {
            try {
                DemoAccess.requireOwner(owner);DemoAccess.requirePremium()
                val config=GmailMail.configuration(result,chosenGoogle)
                if(service.allowed(owner))saveTest(config) else busy=false
            }catch(e:CancellationException){throw e}
            catch(e:Exception){busy=false;mailError=true;message=if(e is MailFailure)e.message.orEmpty() else t(en,"Google no confirmó la autorización.","Google did not confirm authorization.")}
        }
    }
    val googleConsent=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){result->
        if(result.resultCode==Activity.RESULT_OK){
            try{completeGoogle(Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data))}
            catch(e:Exception){busy=false;mailError=true;message=t(en,"Google no completó la autorización.","Google authorization was not completed.")}
        }else{busy=false;mailError=true;message=t(en,"Autorización cancelada.","Authorization cancelled.")}
    }
    // Selector independiente: no cambia el usuario con el que se ha iniciado sesión en Market Sales.
    val googlePicker=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val selected=result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if(result.resultCode==Activity.RESULT_OK && !selected.isNullOrBlank()){
            chosenGoogle=selected
            scope.launch {
                try{
                    DemoAccess.requireOwner(owner);DemoAccess.requirePremium()
                    val auth=kotlinx.coroutines.withTimeoutOrNull(30_000L){Identity.getAuthorizationClient(context).authorize(GmailMail.request(selected)).await()}
                        ?: throw MailFailure("PENDING","Google no respondió a tiempo.")
                    if(auth.hasResolution()){
                        val pending=auth.pendingIntent ?: error("Consentimiento no disponible")
                        googleConsent.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    }else completeGoogle(auth)
                }catch(e:CancellationException){throw e}
                catch(e:Exception){
                    busy=false;mailError=true
                    val status=(e as? com.google.android.gms.common.api.ApiException)?.statusCode
                    message=if(e is MailFailure)e.message.orEmpty() else t(en,"No se pudo conectar con Google. Comprueba los servicios de Google y la configuración OAuth.","Could not connect to Google. Check Google services and OAuth settings.")+(status?.let{" (Google "+it+")"}.orEmpty())
                }
            }
        }else{busy=false;message=t(en,"No se seleccionó una cuenta.","No account selected.")}
    }
    Scaffold(topBar={TopAppBar(title={Text(t(en,"Configuración de correo","Mail settings"))},
        navigationIcon={TextButton(onClick={nav.safePopBackStack()}){Text("←")}},
        colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.primary,titleContentColor=MaterialTheme.colorScheme.onPrimary,navigationIconContentColor=MaterialTheme.colorScheme.onPrimary))}){padding->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            previous?.takeIf{it.usable}?.let { saved -> Text(t(en,"Cuenta configurada en este dispositivo: ","Configured account on this device: ")+saved.address,style=MaterialTheme.typography.bodySmall) }
            listOf("gmail" to "Gmail","outlook" to "Outlook / Hotmail","smtp" to t(en,"Otro proveedor SMTP","Other SMTP provider")).forEach{(value,label)->
                Row(verticalAlignment=Alignment.CenterVertically){
                    RadioButton(provider==value,{provider=value;secret="";message="";mailError=false;if(value=="smtp"){host="";port="465";tls="SSL";user=""}},enabled=!busy)
                    Text(label)
                }
            }
            if(provider=="gmail"){
                Text(t(en,"Conecta Google para enviar recibos. Market Sales no necesita tu contraseña ni permiso para leer tus correos.","Connect Google to send receipts. Market Sales does not need your password or permission to read mail."))
                if(previous?.provider=="gmail")Text(t(en,"Conecta de nuevo tu cuenta: la contraseña de aplicación ya no se utiliza.","Reconnect your account: app passwords are no longer used."),style=MaterialTheme.typography.bodySmall)
                Button(enabled=!busy,onClick={
                    busy=true;mailError=false;message=""
                    try{googlePicker.launch(AccountManager.newChooseAccountIntent(null,null,arrayOf("com.google"),null,null,null,null))}
                    catch(e:Exception){busy=false;mailError=true;message=t(en,"No se pudo abrir el selector de cuentas Google.","Could not open the Google account picker.")}
                }){Text(t(en,"Conectar con Google","Connect with Google"))}
            }else if(provider=="outlook"){
                Text(t(en,"Autoriza la cuenta en Microsoft. No se solicita tu contraseña.","Authorize your account with Microsoft. Your password is not requested."))
                if(!MicrosoftMail.available)Text(t(en,"Outlook todavía requiere registrar el cliente Microsoft de Market Sales.","Outlook still requires Market Sales Microsoft client registration."),color=MaterialTheme.colorScheme.error)
                authorized?.let{Text(it.address)}
                code?.let{c->
                    Text(t(en,"Código de autorización: ","Authorization code: ")+c.code,style=MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(c.url)))}){Text(t(en,"Abrir Microsoft","Open Microsoft"))}
                    TextButton(onClick={authJob?.cancel();code=null;busy=false}){Text(t(en,"Cancelar","Cancel"))}
                }
                Button(enabled=!busy && MicrosoftMail.available,onClick={
                    busy=true;message="";authorized=null
                    authJob=scope.launch {
                        try {
                            val request=withContext(Dispatchers.IO){MicrosoftMail.begin()};code=request
                            val auth=withContext(Dispatchers.IO){MicrosoftMail.authorize(request)}
                            if(service.allowed(owner))authorized=auth
                            message=t(en,"Autorizado. Pulsa comprobar y guardar.","Authorized. Test and save.")
                        }catch(e:CancellationException){throw e}
                        catch(e:Exception){message=t(en,"No se completó la autorización.","Authorization was not completed.")}
                        finally{code=null;busy=false}
                    }
                }){Text(t(en,"Autorizar Outlook","Authorize Outlook"))}
                Button(enabled=!busy && authorized!=null,onClick={authorized?.let{saveTest(it)}}){Text(t(en,"Comprobar y guardar","Test and save"))}
            }else{
                OutlinedTextField(name,{name=it},label={Text(t(en,"Nombre del remitente","Sender name"))},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!busy)
                OutlinedTextField(address,{address=it},label={Text(t(en,"Correo de la empresa","Company email"))},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!busy)
                OutlinedTextField(host,{host=it},label={Text("SMTP")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!busy)
                OutlinedTextField(port,{port=it.filter{ch->ch.isDigit()}.take(5);if(port=="465")tls="SSL";if(port=="587")tls="STARTTLS"},label={Text(t(en,"Puerto","Port"))},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!busy)
                Row(verticalAlignment=Alignment.CenterVertically){listOf("STARTTLS","SSL").forEach{v->RadioButton(tls==v,{tls=v;if(port in listOf("465","587"))port=if(v=="SSL")"465" else "587"},enabled=!busy);Text(v)}}
                OutlinedTextField(user,{user=it},label={Text(t(en,"Usuario (opcional si coincide con el correo)","User (optional if same as email)"))},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!busy)
                OutlinedTextField(secret,{secret=it},label={Text(t(en,"Contraseña de aplicación / SMTP","App password / SMTP"))},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!busy)
                Button(enabled=!busy && ReceiptMail.validAddress(address.trim()) && secret.isNotBlank() && host.isNotBlank() && port.toIntOrNull() in 1..65535,onClick={
                    saveTest(MailConfig("smtp",name.trim(),address.trim(),host.trim(),port.toInt(),tls,user.trim().ifBlank{address.trim()},secret))
                }){Text(t(en,"Comprobar y guardar","Test and save"))}
            }
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(message.isNotBlank())Text(message,style=MaterialTheme.typography.bodySmall,color=if(mailError)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            HorizontalDivider()
            Text(t(en,"Correo Market Sales · servicio adicional de pago previsto, aún no disponible.","Market Sales mail · planned paid add-on, not yet available."),style=MaterialTheme.typography.bodySmall)
            TextButton(enabled=!busy,onClick={if(service.allowed(owner)){settings.clearMail();previous=null;authorized=null;message=t(en,"Configuración eliminada. Puedes retirar el permiso de Gmail en tu cuenta de Google.","Settings removed. You can revoke Gmail access in your Google account.")}}){Text(t(en,"Desconectar correo","Disconnect mail"))}
        }
    }
}

/** Consume el evento de venta solo si hay PDF y la preferencia local pide envío. */
@Composable fun ReceiptAfterSale(id:String,onDismiss:()->Unit) {
    val context=LocalContext.current; val service=remember{ReceiptService(context)}
    var show by remember(id){mutableStateOf(false)}
    LaunchedEffect(id){
        val owner=service.owner()
        if(!service.allowed(owner) || !service.settings(owner).ask) { onDismiss();return@LaunchedEffect }
        val document=withContext(Dispatchers.IO){AppDatabase.getDatabase(context).receiptDocumentDao().find(owner,id)}
        if(document!=null)show=true else onDismiss()
    }
    if(show)ReceiptActions(id,onDismiss,afterSale=true)
}

/** Acciones del documento. Los errores se limitan al diálogo y nunca cambian la venta. */
@Composable fun ReceiptActions(id:String,onDismiss:()->Unit,afterSale:Boolean=false) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val en=language()
    val service=remember{ReceiptService(context)};val owner=service.owner()
    val premium by ConfigurationManager.esPremium.collectAsState()
    var email by remember{mutableStateOf("")};var enterEmail by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
    val configured=remember(owner){service.settings(owner).mail()?.usable==true}
    var preview by remember { mutableStateOf<java.io.File?>(null) }
    val exportPicker=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
        if(uri!=null)scope.launch{
            busy=true
            try{service.export(owner,id,uri);message=t(en,"Copia guardada","Copy saved")}
            catch(e:CancellationException){throw e}
            catch(e:Exception){message=t(en,"No se pudo exportar la copia","Could not export copy")}
            finally{busy=false}
        }
    }
    val importPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)scope.launch{
            busy=true
            try{preview=service.importCopy(owner,id,uri);message=""}
            catch(e:CancellationException){throw e}
            catch(e:Exception){message=t(en,"No se pudo recuperar el PDF original. Comprueba el nombre del archivo.","Could not restore the original PDF. Check the file name.")}
            finally{busy=false}
        }
    }
    fun open(whatsapp:Boolean) {
        if(whatsapp && !service.online()){
            enterEmail=configured;message=if(configured)t(en,"Sin conexión. Introduce el correo para enviarlo después.","Offline. Enter email to send later.") else t(en,"Sin conexión y correo sin configurar. Puedes reenviar después.","Offline and no mail configured. You can resend later.");return
        }
        busy=true
        scope.launch{
            try{val file=service.original(owner,id);if(whatsapp){service.open(owner,file,true);onDismiss()}else preview=file}
            catch(e:CancellationException){throw e}
            catch(e:Exception){if(whatsapp && configured)enterEmail=true;message=if(whatsapp)t(en,"No se pudo compartir por WhatsApp. Puedes usar correo si está configurado.","Could not share via WhatsApp. Use mail if configured.") else t(en,"PDF original no disponible o sin visor PDF.","Original PDF unavailable or no PDF viewer.")}
            finally{busy=false}
        }
    }
    LaunchedEffect(premium){if(!premium)onDismiss()}
    if(!premium)return
    preview?.let { file -> ReceiptPreview(file, onClose={preview=null});return }
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text(if(afterSale)t(en,"¿Enviar recibo al cliente?","Send receipt to customer?") else t(en,"Recibo PDF","PDF receipt"))},
        text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            if(!afterSale){
                OutlinedButton(enabled=!busy,onClick={open(false)},modifier=Modifier.fillMaxWidth()){Text(t(en,"Ver PDF","View PDF"))}
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{service.original(owner,id);exportPicker.launch(ReceiptFiles.externalName(owner,id))}catch(e:CancellationException){throw e}catch(e:Exception){message=t(en,"PDF original no disponible","Original PDF unavailable")}finally{busy=false}}}){Text(t(en,"Exportar copia","Export copy"))}
                    TextButton(enabled=!busy,onClick={importPicker.launch(arrayOf("application/pdf"))}){Text(t(en,"Recuperar copia","Restore copy"))}
                }
            }
            OutlinedButton(enabled=!busy,onClick={open(true)},modifier=Modifier.fillMaxWidth()){Text("WhatsApp")}
            OutlinedButton(enabled=!busy && configured,onClick={enterEmail=true},modifier=Modifier.fillMaxWidth()){Text(t(en,"Correo electrónico","Email"))}
            if(!configured)Text(t(en,"Configura el correo en Utilidades para activar el envío automático.","Configure mail in Utilities to enable automatic sending."),style=MaterialTheme.typography.bodySmall)
            if(enterEmail)OutlinedTextField(email,{email=it},label={Text(t(en,"Correo del cliente","Customer email"))},singleLine=true,enabled=!busy)
            if(message.isNotBlank())Text(message,style=MaterialTheme.typography.bodySmall)
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }},
        confirmButton={if(enterEmail)TextButton(enabled=!busy && ReceiptMail.validAddress(email.trim()),onClick={
            busy=true;scope.launch{
                try{
                    // Una venta local tiene instantánea aunque no se haya generado el PDF todavía.
                    val doc=withContext(Dispatchers.IO){AppDatabase.getDatabase(context).receiptDocumentDao().find(owner,id)}
                    if(doc==null)service.original(owner,id)
                    service.enqueue(owner,id,email.trim());onDismiss()
                }catch(e:CancellationException){throw e}
                catch(e:Exception){message=if(e.message=="Ya existe una solicitud pendiente o incierta")t(en,"Ya existe un envío pendiente o incierto. Revisa Utilidades.","A pending or uncertain request already exists. Check Utilities.") else t(en,"No se pudo guardar la solicitud. El recibo no cambia.","Could not save request. Receipt is unchanged.")}
                finally{busy=false}
            }
        }){Text(t(en,"Enviar","Send"))}},
        dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text(if(afterSale)t(en,"No, finalizar","No, finish") else t(en,"Cerrar","Close"))}})
}

/** Visor PDF local con paginación, sin depender de un lector instalado. */
@Composable private fun ReceiptPreview(file:java.io.File,onClose:()->Unit) {
    val en=language()
    var page by remember(file){mutableStateOf(0)}
    var count by remember(file){mutableStateOf(1)}
    var image by remember(file){mutableStateOf<Bitmap?>(null)}
    var failed by remember(file){mutableStateOf(false)}
    LaunchedEffect(file,page){
        try {
            val result=withContext(Dispatchers.IO){
                val fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer=try {android.graphics.pdf.PdfRenderer(fd)} catch(e:Exception){fd.close();throw e}
                renderer.use{
                    it.openPage(page).use{p->
                        // Acotar memoria incluso para documentos externos de tamaño poco habitual.
                        val scale=minOf(2f,1600f/maxOf(p.width,p.height))
                        val bmp=Bitmap.createBitmap((p.width*scale).toInt().coerceAtLeast(1),(p.height*scale).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        p.render(bmp,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp to it.pageCount
                    }
                }
            }
            image=result.first;count=result.second;failed=false
        }catch(e:CancellationException){throw e}
        catch(e:Exception){failed=true}
    }
    AlertDialog(onDismissRequest=onClose,title={Text(t(en,"Recibo PDF","PDF receipt"))},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(failed)Text(t(en,"No se pudo abrir el PDF","Could not open PDF"))
            else image?.let{Image(it.asImageBitmap(),contentDescription=t(en,"Página del recibo","Receipt page"),modifier=Modifier.fillMaxWidth().heightIn(max=430.dp))} ?: LinearProgressIndicator(Modifier.fillMaxWidth())
            if(count>1)Row(verticalAlignment=Alignment.CenterVertically){
                TextButton(enabled=page>0,onClick={page--}){Text("‹")}
                Text("${page+1} / $count",Modifier.weight(1f))
                TextButton(enabled=page+1<count,onClick={page++}){Text("›")}
            }
        }},confirmButton={TextButton(onClick=onClose){Text(t(en,"Cerrar","Close"))}})
}

/** Entrada discreta en Utilidades y contador de solicitudes no resueltas. */
@Composable fun MailQueueEntry(onClick:()->Unit) {
    val context=LocalContext.current;val en=language()
    val owner by ConfigurationManager.usuarioLogueado.collectAsState();val premium by ConfigurationManager.esPremium.collectAsState()
    val rows by remember(owner){AppDatabase.getDatabase(context).receiptDocumentDao().observeMail(owner.orEmpty(),ReceiptSettings.deviceId(context))}.collectAsState(initial=emptyList())
    if(premium || rows.isNotEmpty()){
        val pending=rows.count{it.state in listOf("PENDING","SENDING","ERROR","UNCERTAIN")}
        OutlinedButton(onClick=onClick,modifier=Modifier.fillMaxWidth()){Text(t(en,"Envíos de recibos","Receipt deliveries") + if(pending>0)" ($pending)" else "")}
    }
}

/** Lista simple de estados; los reintentos inciertos requieren confirmación explícita. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MailQueueScreen(nav:NavController) {
    val context=LocalContext.current;val en=language();val scope=rememberCoroutineScope()
    val service=remember{ReceiptService(context)};val owner=service.owner()
    val dao=remember{AppDatabase.getDatabase(context).receiptDocumentDao()}
    val rows by remember(owner){dao.observeMail(owner,ReceiptSettings.deviceId(context))}.collectAsState(initial=emptyList())
    val premium by ConfigurationManager.esPremium.collectAsState()
    var retry by remember{mutableStateOf<es.nuskysoftware.marketsales.data.local.entity.ReceiptMailEntity?>(null)}
    var recipient by remember{mutableStateOf("")}
    Scaffold(topBar={TopAppBar(title={Text(t(en,"Envíos de recibos","Receipt deliveries"))},navigationIcon={TextButton(onClick={nav.safePopBackStack()}){Text("←")}})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(rows.isEmpty())item{Text(t(en,"No hay solicitudes de envío.","No mail requests."))}
            items(rows,key={it.id}){mail->
                Card(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(12.dp)){
                        Text(mail.recipient,style=MaterialTheme.typography.titleSmall)
                        Text(mail.receiptId,style=MaterialTheme.typography.bodySmall)
                        val status=when(mail.state){
                            "PENDING"->t(en,"Pendiente","Pending");"SENDING"->t(en,"En proceso","Sending")
                            "SENT"->t(en,"Aceptado por el servidor","Accepted by server")
                            "ERROR"->t(en,"Requiere atención","Needs attention")
                            "UNCERTAIN"->t(en,"Resultado incierto","Uncertain result")
                            else->t(en,"Cancelado","Cancelled")
                        }
                        Text(status,color=if(mail.state in listOf("ERROR","UNCERTAIN"))MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        if(mail.detail.isNotBlank() && mail.state!="SENT")Text(mail.detail,style=MaterialTheme.typography.bodySmall)
                        Row{
                            if(mail.state=="PENDING")TextButton(onClick={scope.launch{DemoAccess.requireOwner(owner);dao.transition(owner,mail.id,"PENDING","CANCELLED","")}}){Text(t(en,"Cancelar","Cancel"))}
                            if(premium && mail.state in listOf("ERROR","UNCERTAIN"))TextButton(onClick={recipient=mail.recipient;retry=mail}){Text(t(en,"Revisar y reenviar","Review and resend"))}
                        }
                    }
                }
            }
        }
    }
    retry?.let{mail->
        AlertDialog(onDismissRequest={retry=null},title={Text(t(en,"Reintentar envío","Retry sending"))},text={Column{
            if(mail.state=="UNCERTAIN")Text(t(en,"El correo podría haberse enviado ya. Reintentar puede duplicarlo.","Mail may already have been sent. Retrying may duplicate it."))
            OutlinedTextField(recipient,{recipient=it},label={Text(t(en,"Destinatario","Recipient"))},singleLine=true)
        }},confirmButton={TextButton(enabled=premium && ReceiptMail.validAddress(recipient.trim()),onClick={
            scope.launch{DemoAccess.requireOwner(owner);DemoAccess.requirePremium();dao.retry(owner,mail.id,recipient.trim());ReceiptWorker.requestMail(context);retry=null}
        }){Text(t(en,"Reintentar","Retry"))}},dismissButton={TextButton(onClick={retry=null}){Text(t(en,"Cancelar","Cancel"))}})
    }
}
