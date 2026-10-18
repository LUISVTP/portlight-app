import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

// ---------------------------------------------------------------------------
// Firma de release (Fase 4b): solo LEE keystore.properties (no versionado) o
// variables de entorno. Este script nunca genera claves ni escribe contraseñas;
// ver docs/RELEASING.md. Si falta la firma, `assembleDebug` sigue funcionando y
// cualquier tarea de release falla aquí con un mensaje claro (ES/EN).
// ---------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun releaseSigningProp(fileKey: String, envKey: String): String? =
    keystoreProperties.getProperty(fileKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val releaseStorePath = releaseSigningProp("storeFile", "PORTLIGHT_KEYSTORE_PATH")
val releaseStorePassword = releaseSigningProp("storePassword", "PORTLIGHT_KEYSTORE_PASSWORD")
val releaseKeyAliasName = releaseSigningProp("keyAlias", "PORTLIGHT_KEY_ALIAS")
val releaseKeyPassword = releaseSigningProp("keyPassword", "PORTLIGHT_KEY_PASSWORD")
val releaseSigningReady = releaseStorePath != null &&
    releaseStorePassword != null &&
    releaseKeyAliasName != null &&
    releaseKeyPassword != null

// Solo para medir el tamaño en informes (APK sin firmar): -PportlightReleaseUnsigned
val releaseUnsignedForSize = providers.gradleProperty("portlightReleaseUnsigned").isPresent

val requestsReleaseBuild = gradle.startParameter.taskNames
    .any { it.contains("release", ignoreCase = true) }
if (requestsReleaseBuild && !releaseSigningReady && !releaseUnsignedForSize) {
    error(
        "Firma de release no configurada: falta keystore.properties " +
            "(copia keystore.properties.example) o las variables PORTLIGHT_KEYSTORE_PATH / " +
            "PORTLIGHT_KEYSTORE_PASSWORD / PORTLIGHT_KEY_ALIAS / PORTLIGHT_KEY_PASSWORD. " +
            "Ver docs/RELEASING.md.\n" +
            "Release signing not configured: missing keystore.properties (copy " +
            "keystore.properties.example) or the PORTLIGHT_KEYSTORE_* / PORTLIGHT_KEY_* " +
            "environment variables. See docs/RELEASING.md.",
    )
}

android {
    namespace = "com.luisvtp.portlight"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.luisvtp.portlight"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
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
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        lintConfig = file("lint.xml")
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAliasName
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // Minify/shrink con reglas en proguard-rules.pro (kotlinx.serialization
            // y OkHttp). Con la firma ausente y -PportlightReleaseUnsigned se
            // genera un APK sin firmar solo para medir el tamaño.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseSigningReady && !releaseUnsignedForSize) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    // Fase 2B: FileProvider + ContextCompat.registerReceiver (instalación de APKs).
    implementation(libs.core.ktx)
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.ui.tooling)

    testImplementation(libs.junit)

    // Detector Lint propio (texto hardcodeado en Compose), severidad error
    // Checks propios ejecutados por el lint de este módulo. (lintPublish solo
    // empaqueta checks en AARs publicados, algo propio de módulos librería.)
    lintChecks(project(":lint-rules"))
}
