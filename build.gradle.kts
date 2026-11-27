// Configuración común del proyecto: declara las versiones de plugins que utilizan sus módulos.
plugins {
    // Plugins declarados en la raíz; apply false permite aplicarlos donde se necesitan.
    id("com.android.application") version "9.3.3" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    // KSP genera código a partir de anotaciones, entre ellas las de Room.
    id("com.google.devtools.ksp") version "2.3.2"
    id("com.google.gms.google-services") version "4.4.3" apply false
    
    // Plugin de perfiles de rendimiento utilizado por el módulo Android.
    id("androidx.baselineprofile") version "1.4.0" apply false

}