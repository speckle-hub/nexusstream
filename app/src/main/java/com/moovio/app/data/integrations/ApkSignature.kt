package com.moovio.app.data.integrations

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * APK signature verification for the in-app updater.
 *
 * The updater downloads an APK from a GitHub release and hands it to the system installer. Without
 * a signature check, a hijacked release account (or a user-supplied "update repo") could deliver an
 * APK signed with *any* key — and Android would happily install it as an update of this app,
 * because the user would have to confirm the install. Android's own installer refuses an APK whose
 * signer differs from the installed one, but that check happens after the user has already been
 * shown an install prompt for a foreign package.
 *
 * So the updater verifies the signature *before* handing the file over: the archive must be signed
 * by the same key (or a key in the same signing lineage) as the currently installed app, and it
 * must carry the same package name.
 */

/** SHA-256 fingerprint of a signing certificate, as lowercase hex. */
fun certificateFingerprint(certificate: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(certificate).joinToString("") { "%02x".format(it) }

/**
 * True when every certificate in [candidate] is trusted, i.e. present in [trusted].
 *
 * An empty candidate set is never trusted — an APK with no signature must not be installed.
 */
fun isTrustedSignature(candidate: Set<String>, trusted: Set<String>): Boolean =
    candidate.isNotEmpty() && trusted.containsAll(candidate)

/** Signing certificates of an installed package: the current signer plus its signing history. */
private fun installedFingerprints(context: Context): Set<String> {
    val pm = context.packageManager
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signing = info.signingInfo ?: return emptySet()
            val current = signing.apkContentsSigners.orEmpty().map { certificateFingerprint(it.toByteArray()) }
            val history = signing.signingCertificateHistory.orEmpty().map { certificateFingerprint(it.toByteArray()) }
            (current + history).toSet()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures.orEmpty().map { certificateFingerprint(it.toByteArray()) }.toSet()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        emptySet()
    }
}

/**
 * Verifies that [file] is an update of the currently installed app: same package name, signed by
 * a key the installed app trusts.
 *
 * @throws SecurityException when the archive is not a trustworthy update of this app.
 */
fun verifyApkSignature(context: Context, file: File) {
    val pm = context.packageManager
    val info = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
        }
    } catch (_: Exception) {
        null
    } ?: throw SecurityException("Update APK could not be read")

    val archivePackage = info.packageName
    if (archivePackage != context.packageName) {
        throw SecurityException("Update APK is a different app ($archivePackage)")
    }

    val candidate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signing = info.signingInfo ?: throw SecurityException("Update APK is not signed")
        signing.apkContentsSigners.orEmpty().map { certificateFingerprint(it.toByteArray()) }.toSet()
    } else {
        @Suppress("DEPRECATION")
        info.signatures.orEmpty().map { certificateFingerprint(it.toByteArray()) }.toSet()
    }

    val trusted = installedFingerprints(context)
    if (!isTrustedSignature(candidate, trusted)) {
        throw SecurityException("Update APK is signed by an untrusted key")
    }
}
