plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // The desktop module applies the plain JVM flavour of the same compiler. It has to be declared
    // (unapplied) here with its version, because `:desktop` cannot ask for a *versioned* Kotlin
    // plugin once `org.jetbrains.kotlin.android` has put Kotlin on the root build classpath — Gradle
    // then refuses with "already on the classpath with an unknown version", which is what broke
    // every root `./gradlew` invocation.
    id("org.jetbrains.kotlin.jvm") version "2.0.20" apply false
    // Kotlin 2.0 moved the Compose compiler out of the Kotlin plugin and into its own Gradle
    // plugin (matching the runtime version).
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
