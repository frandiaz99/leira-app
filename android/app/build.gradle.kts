import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Valores públicos en android/local.properties:
//   supabase.url=https://<ref>.supabase.co
//   supabase.anonKey=eyJ...
//   google.webClientId=<id>.apps.googleusercontent.com   (OAuth client "Web" de Google Cloud)
//   google.mapsApiKey=AIza...                              (clave de Maps SDK for Android)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Firebase (avisos): android/app/google-services.json, el archivo que da la consola de Firebase.
// Se leen solo los 4 valores que hacen falta, sin el plugin de google-services.
// Sin el archivo, la app funciona igual pero sin avisos.
val firebase: Map<String, String> = run {
    val f = file("google-services.json")
    if (!f.exists()) return@run emptyMap()
    @Suppress("UNCHECKED_CAST")
    val root = JsonSlurper().parse(f) as Map<String, Any?>
    val project = root["project_info"] as Map<String, Any?>
    val client = (root["client"] as List<Map<String, Any?>>).firstOrNull { c ->
        val info = (c["client_info"] as Map<String, Any?>)["android_client_info"] as Map<String, Any?>
        info["package_name"] == "com.fdm.plancito"
    } ?: return@run emptyMap()
    mapOf(
        "appId" to ((client["client_info"] as Map<String, Any?>)["mobilesdk_app_id"] as String),
        "apiKey" to (((client["api_key"] as List<Map<String, Any?>>).first())["current_key"] as String),
        "projectId" to (project["project_id"] as String),
        "senderId" to (project["project_number"] as String),
    )
}

android {
    namespace = "com.fdm.plancito"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fdm.plancito"
        minSdk = 26
        targetSdk = 35
        // En GitHub Actions BUILD_NUMBER = número de corrida, así cada APK reemplaza al anterior.
        val build = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "0.1.$build"
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("supabase.url", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("supabase.anonKey", "")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps.getProperty("google.webClientId", "")}\"")
        val mapsKey = localProps.getProperty("google.mapsApiKey", "")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        buildConfigField("String", "FIREBASE_APP_ID", "\"${firebase["appId"].orEmpty()}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${firebase["apiKey"].orEmpty()}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${firebase["projectId"].orEmpty()}\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"${firebase["senderId"].orEmpty()}\"")
    }

    // Firma del release: android/signing/ en local, o las variables SIGNING_* en GitHub Actions.
    // Tiene que ser siempre la misma clave, si no Android no deja actualizar la app
    // (y Google Sign-In está atado a su SHA-1).
    val signingProps = Properties().apply {
        val f = rootProject.file("signing/signing.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val keystorePath = System.getenv("SIGNING_KEYSTORE")
        ?: signingProps.getProperty("storeFile")?.let { rootProject.file("signing/$it").path }
    val keystorePassword = System.getenv("SIGNING_PASSWORD") ?: signingProps.getProperty("password")
    signingConfigs {
        if (keystorePath != null && keystorePassword != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = System.getenv("SIGNING_ALIAS") ?: signingProps.getProperty("alias", "plancito")
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.maps.compose)
    implementation(libs.work.runtime)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)

    testImplementation(libs.junit)
}
