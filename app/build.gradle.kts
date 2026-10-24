import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Release signing material is never read from a tracked file and never hard-coded here.
 *
 * It comes from the untracked, git-ignored `local.properties` at the repo root:
 *
 *     moovio.storeFile=C:/secure/moovio-release.jks
 *     moovio.storePassword=…
 *     moovio.keyAlias=moovio
 *     moovio.keyPassword=…
 *
 * or from the `MOOVIO_STORE_FILE` / `MOOVIO_STORE_PASSWORD` / `MOOVIO_KEY_ALIAS` /
 * `MOOVIO_KEY_PASSWORD` environment variables (the file wins when both are set).
 *
 * The key itself deliberately lives outside the repository: the previous release key was
 * committed once and is still recoverable from git history, so it must never be trusted again.
 */
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingCredential(propKey: String, envKey: String): String? =
    (localProps.getProperty(propKey) ?: System.getenv(envKey))?.trim()?.takeIf { it.isNotEmpty() }

android {
    namespace = "com.moovio.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moovio.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 23
        versionName = "1.10.4"
        vectorDrawables { useSupportLibrary = true }
        // TMDB key lives in the gitignored local.properties (or the TMDB_API_KEY env var), never in source.
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
            val store = signingCredential("moovio.storeFile", "MOOVIO_STORE_FILE")
            if (store != null) storeFile = file(store)
            storePassword = signingCredential("moovio.storePassword", "MOOVIO_STORE_PASSWORD")
            keyAlias = signingCredential("moovio.keyAlias", "MOOVIO_KEY_ALIAS")
            keyPassword = signingCredential("moovio.keyPassword", "MOOVIO_KEY_PASSWORD")
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
        freeCompilerArgs += listOf("-Xjvm-default=all", "-Xskip-metadata-version-check")
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
    // Phase 37 Anime4K: GLSL frame-processor effects for the player.
    implementation("androidx.media3:media3-effect:1.3.1")
    // Phase 30 Chromecast: route picker + Cast session delivery for the player.
    implementation("androidx.media3:media3-cast:1.3.1")
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    testImplementation("junit:junit:4.13.2")

    // In-app torrent streaming engine (jlibtorrent) so Real 18+ / P2P Stremio streams play directly.
    implementation("com.github.TorrentStream:TorrentStream-Android:2.7.0")

    // CloudStream provider runtime: lets .cs3 extensions load in-process (see data/cloudstream).
    implementation("com.github.recloudstream.cloudstream:library:4.7.0")
    // The library declares Jackson as a *runtime* dependency, but the provider-settings shim needs
    // the very same mapper on the compile classpath (it is the codec upstream providers read with).
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    // === Aniyomi/Mihon extension runtime (§2ba, NEXT PHASE — toolchain migration required) ===
    // These libraries cannot be compiled with AGP 8.5.0 / compileSdk 36:
    //  - com.github.mihonapp:extensions-lib:1.6.0 → requires compileSdk 37+, AGP ≥ 8.9
    //  - com.github.komikku-app:aniyomi-extensions-lib:13 → duplicate classes with the above
    // Both cause `checkReleaseDuplicateClasses` and `checkReleaseAarMetadata` failures.
    // Do not add them back until the toolchain migration (AGP ≥ 8.9, compileSdk 36/37,
    // Kotlin 2.1–2.4, OkHttp 5) is complete. Extension APKs are still downloaded, stored,
    // enabled/disabled and updated via MangaExtensionRepository — they simply cannot execute yet.
}

/**
 * Fail fast — and with an actionable message — when a release task runs without signing material.
 *
 * AGP's own error ("SigningConfig 'release' is missing required property storePassword") does not
 * say where the credentials are supposed to come from, which made a release build fail late and
 * cryptically. This check runs only when a release packaging task is actually in the graph, so
 * `assembleDebug` and the unit tests stay unaffected.
 */
gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { it.name.endsWith("Release") && it.name.startsWith("package") }
    if (releaseRequested) {
        val release = android.signingConfigs.getByName("release")
        val missing = buildList {
            if (release.storeFile == null || !release.storeFile!!.exists()) add("moovio.storeFile")
            if (release.storePassword == null) add("moovio.storePassword")
            if (release.keyAlias == null) add("moovio.keyAlias")
            if (release.keyPassword == null) add("moovio.keyPassword")
        }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing release signing material: ${missing.joinToString()}. " +
                    "Add them to the untracked local.properties at the repo root " +
                    "(moovio.storeFile / moovio.storePassword / moovio.keyAlias / moovio.keyPassword) " +
                    "or export MOOVIO_STORE_FILE / MOOVIO_STORE_PASSWORD / MOOVIO_KEY_ALIAS / " +
                    "MOOVIO_KEY_PASSWORD.",
            )
        }
    }
}
