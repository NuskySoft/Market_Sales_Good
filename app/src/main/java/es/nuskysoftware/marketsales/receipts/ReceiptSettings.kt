package es.nuskysoftware.marketsales.receipts

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Configuración local por dispositivo/cuenta; las credenciales nunca se guardan en Room ni Firebase. */
class ReceiptSettings(private val context: Context, private val owner: String) {
    // Google Identity Services administra sus tokens en el dispositivo; no se guardan en nuestras preferencias.
    internal val appContext:Context get()=context.applicationContext
    /** La sesión puede cambiar mientras Google renueva el token. Revalidar antes de transmitir. */
    internal suspend fun activeOwner():Boolean =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid==owner &&
            es.nuskysoftware.marketsales.data.local.database.AppDatabase.getDatabase(appContext).configuracionDao().getUsuarioLogueado()==owner
    private val id = ReceiptFiles.key(owner)
    companion object {
        /** Identidad local no incluida en copias Android: evita reenviar colas restauradas en otro móvil. */
        @Synchronized fun deviceId(context:Context):String {
            val file=java.io.File(context.noBackupFilesDir,"receipt-device-id")
            if(file.isFile)file.readText().trim().takeIf{it.isNotBlank()}?.let{return it}
            val id=java.util.UUID.randomUUID().toString()
            val atomic=android.util.AtomicFile(file);val out=atomic.startWrite()
            try{out.write(id.toByteArray(Charsets.UTF_8));atomic.finishWrite(out)}
            catch(e:Exception){atomic.failWrite(out);throw e}
            return id
        }
    }
    private val preferences = context.getSharedPreferences("receipt_settings", Context.MODE_PRIVATE)
    private val secrets = context.getSharedPreferences("receipt_secrets", Context.MODE_PRIVATE)
    var ask: Boolean
        get() = preferences.getBoolean(id+":ask", false)
        set(value) { preferences.edit().putBoolean(id+":ask",value).apply() }
    var folder: String?
        get() = preferences.getString(id+":folder",null)
        set(value) { preferences.edit().putString(id+":folder",value).apply() }
    var welcomeShown: Boolean
        get() = preferences.getBoolean(id+":welcome",false)
        set(value) { preferences.edit().putBoolean(id+":welcome",value).apply() }

    /** Clave no exportable Android Keystore y cifrado autenticado AES-GCM. */
    @Synchronized private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "receipt-mail-" + id
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    fun mail(): MailConfig? = runCatching {
        val stored=secrets.getString(id,null) ?: return null
        val parts=stored.split(":")
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
        MailConfig.fromJson(JSONObject(String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8)))
    }.getOrNull()

    fun save(config: MailConfig) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        val bytes=cipher.doFinal(config.json().toString().toByteArray(Charsets.UTF_8))
        check(secrets.edit().putString(id,Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(bytes,Base64.NO_WRAP)).commit())
    }
    fun clearMail() { secrets.edit().remove(id).commit() }
}

/** Cuenta autorizada: secret vacío para Gmail OAuth; otros proveedores conservan su credencial cifrada. */
data class MailConfig(val provider:String,val name:String,val address:String,val host:String,
    val port:Int,val tls:String,val username:String,val secret:String) {
    /** La configuración antigua Gmail SMTP no habilita nuevos envíos: requiere conectar Google. */
    val usable:Boolean get()=provider!="gmail"
    fun json()=JSONObject().put("provider",provider).put("name",name).put("address",address)
        .put("host",host).put("port",port).put("tls",tls).put("username",username).put("secret",secret)
    companion object {
        fun fromJson(j:JSONObject)=MailConfig(j.getString("provider"),j.getString("name"),j.getString("address"),
            j.getString("host"),j.getInt("port"),j.getString("tls"),j.getString("username"),j.getString("secret"))
    }
}
