import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    id("org.jetbrains.compose") version "1.8.2"
}

group = "com.moovio.desktop"
version = "0.1.1"

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "com.moovio.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Moovio"
            packageVersion = "0.1.1"
        }
    }
}

dependencies {
    val composeVersion = "1.8.2"
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.animation:animation:$composeVersion")
    implementation("org.jetbrains.compose.material:material:$composeVersion")
    // material-icons-extended is only published up to Compose 1.7.3; the core icon set is
    // Pinned at 1.7.3 - ImageVector definitions are stable across Compose versions.
    implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
}
