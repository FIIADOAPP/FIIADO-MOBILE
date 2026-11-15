plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.fiiadoapp.fiado.core.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:domain"))
}
