import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Notificaciones push (Firebase Cloud Messaging): se activan al poner app/google-services.json (ver README).
// Sin el archivo la app compila igual y los avisos se siguen viendo solo dentro de la app.
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

android {
    namespace = "cl.ventanago.app"
    compileSdk = 36

    defaultConfig {
        // Distinto del de la app React Native (cl.ventanago.app) para que ambas puedan convivir en el teléfono.
        applicationId = "cl.ventanago.movil"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.8.0"

        // Mismo backend (Render) y misma carpeta de imágenes (front en Vercel) que la versión web.
        // Para probar contra un backend local: ./gradlew assembleRelease -Pventanago.apiUrl=http://192.168.1.10:7099
        val apiUrl = (project.findProperty("ventanago.apiUrl") as String?) ?: "https://ventanago-api.onrender.com"
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
        // HTTP sin cifrar solo cuando se apunta a un backend local.
        manifestPlaceholders["permitirHttp"] = apiUrl.startsWith("http://").toString()
        // Client ID web de Google (el mismo app.google.client-id del backend). Vacío: no se muestra "Continuar con Google".
        val googleClientId = (project.findProperty("ventanago.googleClientId") as String?).orEmpty().trim()
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "IMAGEN_BASE_URL", "\"https://ventanago.vercel.app/\"")
    }

    // Clave de firma propia del proyecto (carpeta firma/, fuera de git). Con ella el APK se actualiza encima del
    // instalado aunque se compile en otro computador, y Google identifica la app (inicio de sesión con Google, Play).
    val propiedadesFirma = rootProject.file("firma/keystore.properties")
    val firma = Properties().apply { if (propiedadesFirma.exists()) propiedadesFirma.inputStream().use { load(it) } }
    signingConfigs {
        if (propiedadesFirma.exists()) {
            create("release") {
                storeFile = rootProject.file("firma/" + firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Compose en modo debug es lento en teléfonos de gama de entrada: para probar y compartir se usa la
            // versión release (optimizada con R8).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sin la carpeta firma/ (p. ej. recién clonado) se firma con la clave de depuración, solo para probar.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Notificaciones push.
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-messaging")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // "Continuar con Google" (Credential Manager).
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")

    // Realidad aumentada (opcional en el teléfono) y cámara para la vista sin ARCore.
    implementation("com.google.ar:core:1.56.0")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")
}
