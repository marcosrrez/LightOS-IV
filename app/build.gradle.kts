import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lightos.iv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lightos.iv"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "10.0.0"
    }

    signingConfigs {
        create("release") {
            // Supplied by CI (GitHub secrets) or a local ~/.lightos/keystore.properties.
            val local = Properties().apply {
                val f = File(System.getProperty("user.home"), ".lightos/keystore.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
            val ks = System.getenv("LIGHTOS_KEYSTORE")
                ?: local.getProperty("storeFile")?.let { File(System.getProperty("user.home"), ".lightos/$it").path }
            if (ks != null && File(ks).exists()) {
                storeFile = File(ks)
                storePassword = System.getenv("LIGHTOS_STORE_PASSWORD") ?: local.getProperty("storePassword")
                keyAlias = System.getenv("LIGHTOS_KEY_ALIAS") ?: local.getProperty("keyAlias")
                keyPassword = System.getenv("LIGHTOS_KEY_PASSWORD") ?: local.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    debugImplementation(libs.compose.ui.tooling)
}
