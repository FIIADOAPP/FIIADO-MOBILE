plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.fiiadoapp.fiado"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fiiadoapp.fiado"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "0.5.0"
    }

    // Clave de subida a Google Play (ADR-0009): solo desde variables de entorno, nunca desde el repositorio.
    val firma = System.getenv("FIRMA_ARCHIVO")?.takeIf { it.isNotBlank() }
    signingConfigs {
        if (firma != null) {
            create("release") {
                storeFile = file(firma)
                storePassword = System.getenv("FIRMA_CLAVE")
                keyAlias = System.getenv("FIRMA_ALIAS")
                keyPassword = System.getenv("FIRMA_CLAVE_ALIAS")
            }
        }
    }

    buildTypes {
        release {
            // Sin las variables de firma, el release se compila sin firmar.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:clientes"))
    implementation(project(":feature:movimientos"))
    implementation(project(":feature:cuenta"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
