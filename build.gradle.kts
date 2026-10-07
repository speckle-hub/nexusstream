plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Kotlin 2.0 moved the Compose compiler out of the Kotlin plugin and into its own Gradle
    // plugin (matching the runtime version). Required for Compose 1.7's SharedTransitionLayout.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
