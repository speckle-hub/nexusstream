import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing credentials live outside the build script. Provide them via an untracked
// `keystore.properties` at the repo root, or via environment variables. Never hard-code them here.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingCredential(propKey: String, envKey: String): String? =
    keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

android {
    namespace = "com.novastream.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.novastream.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.1.1"
        vectorDrawables { useSupportLibrary = true }
        // TMDB key lives in the gitignored local.properties (or the TMDB_API_KEY env var), never in source.
        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        buildConfigField(
            "String",
            "TMDB_API_KEY",
            "\"${localProps.getProperty("TMDB_API_KEY") ?: System.getenv("TMDB_API_KEY") ?: ""}\"",
        )
        ndk {
            // jlibtorrent (torrent streaming engine) ships native libs for these ABIs.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile") ?: "keystore/novastream.jks")
            storePassword = signingCredential("storePassword", "NOVASTREAM_STORE_PASSWORD")
            keyAlias = signingCredential("keyAlias", "NOVASTREAM_KEY_ALIAS")
            keyPassword = signingCredential("keyPassword", "NOVASTREAM_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // Minify stays off until it can be smoke-tested on a device: R8 shrinking of
            // reflection/JNI paths (Gson models, Media3, jlibtorrent) can fail only at runtime.
            // proguard-rules.pro is already prepared, so enabling this is a one-line change.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "META-INF/*.kotlin_module"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose 1.7.x — the first release with SharedTransitionLayout / Modifier.sharedElement, which
    // powers the poster -> detail morph (see roadmap §2e).
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.3.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    testImplementation("junit:junit:4.13.2")

    // In-app torrent streaming engine (jlibtorrent) so Real 18+ / P2P Stremio streams play directly.
    implementation("com.github.TorrentStream:TorrentStream-Android:2.7.0")
}
