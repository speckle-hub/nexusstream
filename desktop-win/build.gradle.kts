@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import java.io.File
import java.net.URI
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "com.moovio"
version = "0.1.1"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")

    implementation("net.java.dev.jna:jna:5.15.0")
    implementation("net.java.dev.jna:jna-platform:5.15.0")

    testImplementation(kotlin("test"))
    testImplementation(compose.uiTest)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

/** Fetches a prebuilt Windows x64 libmpv into native/mpv (not committed; see README). */
val libmpvVersion = "20261021-git-e471496c56"
val libmpvUrl =
    "https://github.com/shinchiro/mpv-winbuild-cmake/releases/download/20261021/mpv-dev-x86_64-$libmpvVersion.7z"

tasks.register("fetchLibmpv") {
    description = "Download a prebuilt libmpv-2.dll into native/mpv"
    group = "moovio"
    doLast {
        val outDir = layout.projectDirectory.dir("native/mpv").asFile
        val dll = File(outDir, "libmpv-2.dll")
        if (dll.exists()) {
            logger.lifecycle("libmpv-2.dll already present at ${dll.absolutePath}")
            return@doLast
        }
        outDir.mkdirs()
        val archive = File(outDir, "mpv-dev.7z")
        logger.lifecycle("Downloading $libmpvUrl")
        URI(libmpvUrl).toURL().openStream().use { input ->
            archive.outputStream().use { output -> input.copyTo(output) }
        }
        val extract = File(outDir, "extract").apply { mkdirs() }
        val process = ProcessBuilder("tar", "-xf", archive.absolutePath, "-C", extract.absolutePath)
            .redirectErrorStream(true)
            .start()
        val log = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        check(code == 0) { "tar failed to extract libmpv archive:\n$log" }
        val found = extract.walkTopDown().firstOrNull { it.name.equals("libmpv-2.dll", true) }
            ?: error("libmpv-2.dll not found in archive")
        found.copyTo(dll, overwrite = true)
        archive.delete()
        extract.deleteRecursively()
        logger.lifecycle("Installed ${dll.absolutePath} (${dll.length() / 1_048_576} MB)")
    }
}

/**
 * Portable Windows build: jpackage app-image + libmpv next to the launcher, zipped.
 * Installers (MSI/EXE) additionally need the WiX toolset installed on the build machine.
 */
// jpackage only bundles platform resources from a staging folder. Copy the DLL there so both
// the portable image and the MSI contain the player runtime.
val stageMpv by tasks.registering(Copy::class) {
    group = "moovio"
    description = "Stage libmpv-2.dll for jpackage app resources"
    val source = layout.projectDirectory.file("native/mpv/libmpv-2.dll")
    onlyIf { source.asFile.isFile }
    from(source)
    into(layout.projectDirectory.dir("packaging/windows"))
}

tasks.matching { it.name in listOf("createDistributable", "packageMsi", "packageExe", "prepareAppResources") }.configureEach {
    dependsOn(stageMpv)
}

val portableZip by tasks.registering(Zip::class) {
    group = "moovio"
    description = "Build a portable zipped Windows app image (includes libmpv when fetched)"
    dependsOn("createDistributable")
    archiveFileName.set("Moovio-${project.version}-windows-x64.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(layout.buildDirectory.dir("compose/binaries/main/app")) {
        into("Moovio")
    }
}

compose.desktop {
    application {
        mainClass = "com.moovio.desktop.MainKt"

        jvmArgs += listOf(
            "-Dfile.encoding=UTF-8",
            "-Dmoovio.version=${project.version}",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            appResourcesRootDir.set(layout.projectDirectory.dir("packaging"))
            packageName = "Moovio"
            packageVersion = "0.1.1"
            description = "Moovio — premium desktop streaming client"
            vendor = "Moovio"
            // JDK modules used indirectly (java.net.http, JNA, Skia/JCEF bindings).
            includeAllModules = true
            copyright = "© 2026 Moovio"
            windows {
                iconFile.set(project.file("packaging/moovio.ico"))
                menu = true
                shortcut = true
                perUserInstall = true
                dirChooser = true
                upgradeUuid = "b7c1d2e4-6a2f-4a6d-9c3e-5f7a8b9c0d11"
            }
        }
    }
}
