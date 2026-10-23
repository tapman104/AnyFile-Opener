import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

val requiredSigningKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val hasAllSigningProperties = requiredSigningKeys.all { key ->
    !keystoreProperties.getProperty(key).isNullOrBlank()
}

val resolvedStoreFile = if (hasAllSigningProperties) {
    rootProject.file(keystoreProperties.getProperty("storeFile"))
} else null

val isSigningConfigured = hasAllSigningProperties && resolvedStoreFile?.exists() == true

android {
    namespace = "com.anyfile.x"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.anyfile.x"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (isSigningConfigured) {
            create("release") {
                storeFile = resolvedStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (isSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
}

gradle.taskGraph.whenReady {
    val releasePackagingRequested = allTasks.any { task ->
        task.name.equals("assembleRelease") ||
        task.name.equals("bundleRelease") ||
        task.name.startsWith("packageRelease") ||
        task.name.startsWith("validateSigningRelease")
    }
    if (releasePackagingRequested && !isSigningConfigured) {
        val missingDetails = buildString {
            if (!keystorePropertiesFile.exists()) {
                append("'keystore.properties' not found in root project directory. ")
            } else if (!hasAllSigningProperties) {
                val missingKeys = requiredSigningKeys.filter { keystoreProperties.getProperty(it).isNullOrBlank() }
                append("Missing required properties in 'keystore.properties': $missingKeys. ")
            } else if (resolvedStoreFile?.exists() != true) {
                append("Keystore file '${keystoreProperties.getProperty("storeFile")}' specified in 'keystore.properties' does not exist relative to project root. ")
            }
            append("A valid, signed release configuration is required for release builds.")
        }
        throw GradleException(missingDetails)
    }
}

