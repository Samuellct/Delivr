import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

// Le keystore de release est un secret. Deux sources possibles, dans cet ordre
// de priorité :
//  1. Variables d'environnement KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS /
//     KEY_PASSWORD — utilisées par le workflow GitHub Actions de release, qui
//     reconstitue le fichier .jks à partir d'un secret base64 (voir
//     .github/workflows/release.yml). Rien de tout cela n'est commité.
//  2. /keystore.properties en local (gitignoré) — pour signer une release
//     directement depuis un poste de développement.
// En l'absence des deux, les builds debug fonctionnent normalement et le
// build release reste simplement non signé.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

val envKeystoreFile = System.getenv("KEYSTORE_FILE")
val envKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
val envKeyAlias = System.getenv("KEY_ALIAS")
val envKeyPassword = System.getenv("KEY_PASSWORD")

val resolvedStoreFile: File? =
    when {
        !envKeystoreFile.isNullOrBlank() -> file(envKeystoreFile)
        keystoreProperties.containsKey("storeFile") ->
            rootProject.file(keystoreProperties["storeFile"] as String)
        else -> null
    }
val resolvedStorePassword = envKeystorePassword ?: keystoreProperties["storePassword"] as String?
val resolvedKeyAlias = envKeyAlias ?: keystoreProperties["keyAlias"] as String?
val resolvedKeyPassword = envKeyPassword ?: keystoreProperties["keyPassword"] as String?

val hasKeystoreConfig =
    resolvedStoreFile != null &&
        resolvedStoreFile.exists() &&
        resolvedStorePassword != null &&
        resolvedKeyAlias != null &&
        resolvedKeyPassword != null

// versionName/versionCode sont injectables par la CI (voir release.yml), qui
// dérive le premier des tags SemVer (Conventional Commits) et le second du
// numéro d'exécution du workflow. En local, valeurs par défaut ci-dessous.
val resolvedVersionName = (project.findProperty("delivrVersionName") as String?) ?: "0.1.0"
val resolvedVersionCode = (project.findProperty("delivrVersionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.delivr.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.delivr.app"
        minSdk = 26
        targetSdk = 35
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasKeystoreConfig) {
            create("release") {
                storeFile = resolvedStoreFile
                storePassword = resolvedStorePassword
                keyAlias = resolvedKeyAlias
                keyPassword = resolvedKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasKeystoreConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
