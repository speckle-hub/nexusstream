package com.moovio.app.data.integrations

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * On-disk format of an encrypted backup.
 *
 * ```
 * [ MAGIC ][ IV 12 bytes ][ AES-256-GCM ciphertext + 16 byte tag ]
 * ```
 *
 * The magic prefix is what lets an import tell an encrypted export apart from a legacy plaintext
 * `.json` without guessing from the file extension — a `.json` a user renamed, or an encrypted file
 * they saved with the old name, both resolve correctly.
 *
 * Everything here is plain JCE, so the format itself is testable on the JVM; only the key lives in
 * the Android KeyStore (see [AndroidBackupKey]).
 */
object BackupFormat {

    /** Marker at the head of every encrypted export. ASCII so it is readable in a hex dump. */
    private const val MAGIC_TEXT = "MOOVIO-BACKUP-1"

    val MAGIC: ByteArray = MAGIC_TEXT.toByteArray(Charsets.US_ASCII)

    /** GCM nonce length. 12 bytes is the size AES-GCM is specified for and the only one with no perf caveat. */
    const val IV_BYTES = 12

    /** Authentication tag length. 128 bits, the maximum GCM offers. */
    const val TAG_BITS = 128

    /** Smallest possible valid container: magic + IV + a 16-byte tag over zero plaintext. */
    private const val MIN_BYTES = 16 + IV_BYTES + TAG_BITS / 8

    /** True when [bytes] carries the encrypted container header. */
    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < MIN_BYTES) return false
        for (i in MAGIC.indices) if (bytes[i] != MAGIC[i]) return false
        return true
    }
}

/**
 * AES-256-GCM wrapper around a backup payload.
 *
 * A fresh random IV per export is mandatory for GCM: reusing an IV under the same key breaks the
 * cipher completely (the keystream repeats and the tag can be forged), and the key here is reused
 * for every export the user makes. The IV is stored in the clear next to the ciphertext, which is
 * standard practice — it is not secret, only unique.
 *
 * [keyProvider] is injected rather than read from the KeyStore directly so tests can drive the real
 * encrypt/decrypt path with a JVM-generated key.
 */
class BackupCipher(private val keyProvider: () -> SecretKey) {

    private val random = SecureRandom()

    /** Encrypts [plain] into a self-describing container (magic + IV + ciphertext). */
    fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(BackupFormat.IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider(), GCMParameterSpec(BackupFormat.TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plain)
        return BackupFormat.MAGIC + iv + ciphertext
    }

    /**
     * Decrypts a container produced by [encrypt].
     *
     * A wrong key, a truncated file or a single flipped byte all surface as
     * [javax.crypto.AEADBadTagException] from `doFinal` — GCM authenticates the whole payload, so a
     * tampered backup is rejected rather than half-restored.
     */
    fun decrypt(blob: ByteArray): ByteArray {
        require(BackupFormat.isEncrypted(blob)) { "Not an encrypted Moovio backup" }
        val ivStart = BackupFormat.MAGIC.size
        val iv = blob.copyOfRange(ivStart, ivStart + BackupFormat.IV_BYTES)
        val ciphertext = blob.copyOfRange(ivStart + BackupFormat.IV_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(BackupFormat.TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/**
 * The backup key, generated once per install and kept in the Android KeyStore.
 *
 * The key material never leaves the keystore: the OS hands back only a [SecretKey] handle, so a
 * backup file copied off the device cannot be decrypted on another device, and root-level access to
 * the app's files still does not yield the key on hardware with a TEE/StrongBox.
 *
 * This is a deliberate trade-off: encrypted backups are **not portable between devices**. Restoring
 * on a new phone needs a fresh export made there, or the legacy plaintext file the user still has —
 * which is exactly why [BackupManager] still understands plaintext with an explicit confirmation.
 */
object AndroidBackupKey {

    private const val ALIAS = "moovio_backup_aes"

    /** Returns the existing key, creating a 256-bit AES-GCM key on first use. */
    fun get(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // No user-authentication gate: a backup written to SAF has to be readable when the
                // user is restoring, not only while a biometric prompt is open.
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    private const val PROVIDER = "AndroidKeyStore"
}
