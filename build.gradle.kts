plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

fun fuentes(patron: String) =
    fileTree(rootDir) {
        include(patron)
        exclude("**/build/**", ".gradle/**", ".kotlin/**", "node_modules/**")
    }

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    kotlin {
        target(fuentes("**/*.kt"))
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target(fuentes("**/*.gradle.kts"))
        ktlint(ktlintVersion)
    }
}
