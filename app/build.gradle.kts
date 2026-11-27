// Configuración del módulo Android: plugins, firma, variantes, SDK, versión y dependencias.
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    // deja kapt si lo necesitas realmente:
    //id("kotlin-kapt")

    id("androidx.baselineprofile")

}

// Lee la configuración de firma privada si existe. Debug no necesita la clave release.
val signingProperties = Properties().apply {
    val signingFile = rootProject.file("keystore.properties")
    if (signingFile.isFile) signingFile.inputStream().use { load(it) }
}

android {
    // La firma release se conserva fuera del repositorio para no publicar contraseñas.
    signingConfigs {
        create("release") {
            storeFile = signingProperties.getProperty("storeFile")?.let { rootProject.file(it) }
            storePassword = signingProperties.getProperty("storePassword")
            keyAlias = signingProperties.getProperty("keyAlias")
            keyPassword = signingProperties.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug {
            // App ID de PRUEBA oficial de Google (para desarrollar sin líos)
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            // TU App ID REAL (con ~)
            isMinifyEnabled = false
        }
    }
    // Espacio de nombres Kotlin/Android y SDK utilizado para compilar.
    namespace = "es.nuskysoftware.marketsales"
    compileSdk = 36

    defaultConfig {
        applicationId = "es.nuskysoftware.marketsales"
        // Android 8 (API 26) es el mínimo declarado por la aplicación.
        minSdk = 26
        // Nivel de API para el que se declara el comportamiento objetivo.
        targetSdk = 36
        // El código numérico aumenta en cada entrega; la interfaz muestra VERSION_NAME.
        versionCode = 11
        versionName = "2.7"
        // Identificador público Microsoft Entra, no es una contraseña; vacío hasta registrar el cliente.
        val msMailClient = providers.gradleProperty("MS_MAIL_CLIENT_ID").orElse("").get()
        require(msMailClient.isEmpty() || msMailClient.matches(Regex("[0-9a-fA-F-]{36}")))
        buildConfigField("String", "MS_MAIL_CLIENT_ID", "\"$msMailClient\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            manifestPlaceholders += mapOf("ADMOB_APP_ID" to "ca-app-pub-9343856188038526~1418564772")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug { }
    }

    // Mantiene el nivel de bytecode de Java y Kotlin configurado en la versión anterior.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Habilita Compose y la generación de BuildConfig, fuente de versión y variante.
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // SMTP autenticado desde Android, sin intermediario de Market Sales.
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    // ===== Compose BOM (versiona TODAS las libs de androidx.compose.*) =====
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI (SIN versiones explícitas)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.foundation)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.runtime.saveable)
    //  implementation(libs.androidx.benchmark.baseline.profile.gradle.plugin)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Integración Android, ciclo de vida, actividad Compose y navegación.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Room: acceso a SQLite y generación de código de las consultas y entidades mediante KSP.
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Selector de color, ViewModel Compose y extensiones Kotlin de Room.
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.room:room-ktx:2.7.1")

    // Firebase: BOM, Firestore para datos remotos y Auth para autenticación de cuentas.
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth:22.3.0")
    // (tenías también una segunda firestore con 24.9.1; conserva solo una línea para evitar duplicados)
    // implementation("com.google.firebase:firebase-firestore:24.9.1")

    // WorkManager: trabajos persistentes de sincronización, stock y estados.
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Google Auth / Credentials
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Dependencias ya presentes en 2.4. Se conservan sus declaraciones para no alterar la resolución de Gradle.
    implementation("androidx.room:room-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("com.github.skydoves:colorpicker-compose:1.0.5")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("com.google.android.gms:play-services-ads:24.5.0")
    // UMP (User Messaging Platform) para consentimiento
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")


    // JUnit para pruebas locales; AndroidX y Espresso para pruebas instrumentadas.
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


}



/*
IMPORTANTE:
- Quita cualquier dependencia a:
    "androidx.compose.material:material-pull-refresh"
  y elimina los imports antiguos:
    androidx.compose.material.pullrefresh.*
- Usa SIEMPRE los nuevos imports de Material 3:
    androidx.compose.material3.pulltorefresh.*
*/
