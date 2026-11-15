plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core:domain"))
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
