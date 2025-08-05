plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    // deja kapt si lo necesitas realmente:
    id("kotlin-kapt")
}

android {
    namespace = "es.nuskysoftware.marketsales"
    compileSdk = 36

    defaultConfig {
        applicationId = "es.nuskysoftware.marketsales"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // ===== Compose BOM (versiona TODAS las libs de androidx.compose.*) =====
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI (SIN versiones explícitas)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.foundation)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // ===== Tu stack existente =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // (mantén tus otras dependencias tal cual)
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.room:room-ktx:2.7.1")

    // Firebase BOM + artefactos (si ya los usabas así)
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth:22.3.0")
    // (tenías también una segunda firestore con 24.9.1; conserva solo una línea para evitar duplicados)
    // implementation("com.google.firebase:firebase-firestore:24.9.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Google Auth / Credentials
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Dependencias que dejaste duplicadas con otras versiones:
    implementation("androidx.room:room-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("com.github.skydoves:colorpicker-compose:1.0.5")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Tests
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


//plugins {
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
//    id("org.jetbrains.kotlin.plugin.compose")
//    id("com.google.devtools.ksp")
//    id("com.google.gms.google-services")
//    id ("kotlin-kapt")
//}
//
//android {
//    namespace = "es.nuskysoftware.marketsales"
//    compileSdk = 36
//
//    defaultConfig {
//        applicationId = "es.nuskysoftware.marketsales"
//        minSdk = 24
//        targetSdk = 36
//        versionCode = 1
//        versionName = "1.0"
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//
//    kotlinOptions {
//        jvmTarget = "11"
//    }
//
//    buildFeatures {
//        compose = true
//    }
//}
//
//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    implementation(libs.androidx.navigation.compose)
//    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.foundation)
//    implementation(libs.material3)
//
//
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//    ksp(libs.androidx.room.compiler)
//
//    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
//
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
//
//    implementation("androidx.room:room-ktx:2.7.1")
//
//    // ✅ Firebase BOM (maneja versiones automáticamente)
//    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
//    implementation("com.google.firebase:firebase-firestore")
//
//    implementation("androidx.work:work-runtime-ktx:2.9.0")
//
//    // ✅ FIREBASE AUTH PARA FASE 2
//    implementation("com.google.firebase:firebase-auth:22.3.0")
//    implementation("com.google.firebase:firebase-firestore:24.9.1")
//
//    // ✅ GOOGLE AUTH V8 - NUEVAS DEPENDENCIAS AGREGADAS
//    implementation("com.google.android.gms:play-services-auth:20.7.0")
//    implementation("androidx.credentials:credentials:1.3.0")
//    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
//    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
//
//    // ✅ AGREGADO SOLO LO NECESARIO PARA FASE 2:
//    implementation("androidx.room:room-ktx:2.6.0")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
//    implementation("androidx.compose.material:material-icons-extended:1.5.4")
//
//    implementation("com.github.skydoves:colorpicker-compose:1.0.5")
//}
