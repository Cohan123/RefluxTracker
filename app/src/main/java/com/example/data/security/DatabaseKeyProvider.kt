package com.example.data.security

import android.util.Base64
import java.io.File
import java.security.SecureRandom

/**
 * Manages the 32-byte symmetric key used to encrypt the local Room database
 * via SQLCipher.
 *
 * Threat model:
 *  - Device is lost / stolen, attacker has physical or root access.
 *  - Goal: the on-disk SQLite file must be unreadable without the key.
 *  - The key is not stored in cleartext, but it is only obfuscated: the XOR
 *    pad is stored right next to the wrapped key. This is NOT equivalent to
 *    Keystore-backed wrapping (see the note below).
 *
 * Key flow:
 *  1. On first call, generate a random 32-byte DB key with [SecureRandom].
 *  2. Encode it as Base64.
 *  3. XOR it with a per-install random 32-byte pad, then Base64-encode again.
 *     (The pad is stored next to the wrapped key — this is obfuscation, not
 *     real cryptography. The real cryptographic boundary on modern Android is
 *     the Android Keystore; on older devices that don't support a Keystore-
 *     backed wrapping key, this obfuscation is the best local-only option.)
 *  4. Write the wrapped key + pad to [prefsFile] as two Base64 lines.
 *
 * NOTE: This implementation is intentionally local-only and self-contained
 * (no Android Keystore calls in unit-testable paths). On a real device, the
 * Android Keystore-backed wrapping is wired in via a future hardening PR.
 *
 * The public API:
 *  - [getOrCreateDatabaseKey] returns the 32 raw key bytes.
 */
object DatabaseKeyProvider {

    private const val KEY_LENGTH = 32

    fun getOrCreateDatabaseKey(prefsFile: File): ByteArray {
        val store = KeyStore.read(prefsFile)
        if (store != null) {
            return unwrap(store)
        }
        val rawKey = ByteArray(KEY_LENGTH).also { SecureRandom().nextBytes(it) }
        val pad = ByteArray(KEY_LENGTH).also { SecureRandom().nextBytes(it) }
        val wrapped = ByteArray(KEY_LENGTH) { i -> (rawKey[i].toInt() xor pad[i].toInt()).toByte() }
        KeyStore.write(prefsFile, wrapped = wrapped, pad = pad)
        return rawKey
    }

    private fun unwrap(store: KeyStore): ByteArray {
        val out = ByteArray(KEY_LENGTH)
        for (i in 0 until KEY_LENGTH) {
            out[i] = (store.wrapped[i].toInt() xor store.pad[i].toInt()).toByte()
        }
        return out
    }

    /** Minimal on-disk representation: two Base64 lines: wrapped, then pad. */
    internal data class KeyStore(val wrapped: ByteArray, val pad: ByteArray) {
        companion object {
            private const val VERSION_PREFIX = "RTKEYv1"

            fun read(file: File): KeyStore? {
                if (!file.exists()) return null
                val lines = file.readLines().filter { it.isNotBlank() }
                if (lines.size != 3) return null
                if (lines[0] != VERSION_PREFIX) return null
                return try {
                    KeyStore(
                        wrapped = Base64.decode(lines[1], Base64.NO_WRAP),
                        pad = Base64.decode(lines[2], Base64.NO_WRAP)
                    )
                } catch (_: IllegalArgumentException) {
                    null
                }
            }

            fun write(file: File, wrapped: ByteArray, pad: ByteArray) {
                file.parentFile?.mkdirs()
                file.writeText(
                    buildString {
                        append(VERSION_PREFIX); append('\n')
                        append(Base64.encodeToString(wrapped, Base64.NO_WRAP)); append('\n')
                        append(Base64.encodeToString(pad, Base64.NO_WRAP)); append('\n')
                    }
                )
            }
        }
    }
}
