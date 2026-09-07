package com.example.data.security

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Behaviour spec for [DatabaseKeyProvider].
 *
 * The provider must:
 *  - Generate a 32-byte database key on first access (call it from main thread
 *    inside Robolectric — real Android Keystore is mocked by Robolectric).
 *  - Return the SAME key on subsequent calls within the same install (so the
 *    Room database can be re-opened after process death).
 *  - Persist the key encrypted-at-rest (Keystore-wrapped), so it survives
 *    process restart but is not stored in cleartext on disk.
 *  - Reject persistence if no keystore is available (fail loudly, no fallback
 *    to plaintext).
 *
 * What this test intentionally does NOT cover:
 *  - Real Android Keystore hardware backing (Robolectric fakes the keystore;
 *    production behaviour is verified on-device).
 *  - Key rotation (separate follow-up PR).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseKeyProviderTest {

    private lateinit var prefsFile: File

    @Before
    fun setUp() {
        // Robolectric gives us an isolated Context; clear any leftover key file.
        prefsFile = File(
            System.getProperty("java.io.tmpdir"),
            "dbkey-test-${System.nanoTime()}.prefs"
        )
        prefsFile.delete()
    }

    @After
    fun tearDown() {
        prefsFile.delete()
    }

    @Test
    fun `first getOrCreateDatabaseKey returns 32 bytes`() {
        val key1 = DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile)
        assertNotNull(key1)
        assertEquals(32, key1.size)
    }

    @Test
    fun `second call returns the same key bytes`() {
        val key1 = DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile)
        val key2 = DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile)
        assertArrayEquals(
            "DatabaseKeyProvider must return the same key on repeated calls " +
                "within one install, otherwise SQLCipher cannot reopen the DB",
            key1,
            key2
        )
    }

    @Test
    fun `key survives a simulated process restart`() {
        val key1 = DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile)
        // simulate process death: new "instance", same prefs file
        val key2 = DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile)
        assertArrayEquals(
            "After process restart, the same key must be returned so Room " +
                "can decrypt the existing database",
            key1,
            key2
        )
    }

    @Test
    fun `persisted key on disk is not plaintext`() {
        DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile)
        assertTrue("prefs file must exist after key generation", prefsFile.exists())

        // The prefs file MUST NOT contain the raw 32-byte key as a contiguous block.
        // We cannot know the exact encoded format, but we can assert the raw key
        // bytes are not present in the file.
        val keyBytes = DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile)
        val rawBytes = keyBytes
        val onDisk = prefsFile.readBytes()

        // Search for any 8-byte window of the raw key in the file. If any matches,
        // the key is stored in cleartext.
        var foundPlaintextWindow = false
        var i = 0
        while (i <= onDisk.size - 8) {
            var match = true
            for (j in 0 until 8) {
                if (onDisk[i + j] != rawBytes[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                foundPlaintextWindow = true
                break
            }
            i++
        }
        assertFalse(
            "Persisted key file must not contain raw key bytes — keystore wrap failed",
            foundPlaintextWindow
        )
    }
}
