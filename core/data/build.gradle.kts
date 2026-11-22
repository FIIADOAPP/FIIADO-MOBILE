plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.fiiadoapp.fiado.core.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        // Dirección de la API (ADR-0005). En debug, el PC visto desde el emulador; para un teléfono
        // en el mismo wifi: ./gradlew installDebug -Pfiado.apiUrl=http://192.168.x.x:8000/
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // MigrationTestHelper lee los esquemas exportados; Room solo los publica para las pruebas instrumentadas.
    sourceSets
        .getByName("test")
        .assets.directories
        .add("$projectDir/schemas")

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_URL", "\"${providers.gradleProperty("fiado.apiUrl").getOrElse("http://10.0.2.2:8000/")}\"")
        }
        release {
            // La URL pública llega desde la variable API_URL_RELEASE del workflow de release (ADR-0009).
            buildConfigField(
                "String",
                "API_URL",
                "\"${providers.gradleProperty("fiado.apiUrlRelease").getOrElse("https://api.fiado.invalid/")}\"",
            )
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
    }
}

kotlin {
    jvmToolchain(21)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlin.test)
}
