package com.moovio.desktop.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-HMAC-SHA256 hashing for the NSFW PIN. The PIN itself is never stored, only a salted
 * hash; a short numeric PIN is inherently brute-forceable, so the gate is a privacy screen,
 * not disk encryption (the data directory can be protected with BitLocker/EFS by the user).
 */
object PinHasher {
    private const val ITERATIONS = 150_000
    private const val KEY_LENGTH_BITS = 256

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return Base64.getEncoder().encodeToString(factory.generateSecret(spec).encoded)
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(
            hash(pin, salt).toByteArray(),
            expectedHash.toByteArray(),
        )

    fun isValidPin(pin: String): Boolean = Regex("""^\d{4,8}$""").matches(pin)
}
