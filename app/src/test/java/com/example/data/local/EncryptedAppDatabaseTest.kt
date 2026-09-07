package com.example.data.local

import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FoodEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Behaviour spec for SQLCipher-backed [AppDatabase].
 *
 * Methods are run in name-ascending order so the database lifecycle is
 * deterministic across tests (open → close → reopen). Without this,
 * Robolectric's single-threaded SQLite shadow occasionally surfaces
 * SQLITE_BUSY when the onCreate seed coroutine from the previous test
 * is still draining when the next test reopens.
 */
@RunWith(RobolectricTestRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Config(sdk = [34])
class EncryptedAppDatabaseTest {

    private lateinit var dbFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        dbFile = context.getDatabasePath("test-reflux.db")
        dbFile.parentFile?.mkdirs()
        File(dbFile.parentFile, "test-reflux.db").delete()
        File(dbFile.parentFile, "test-reflux.db-shm").delete()
        File(dbFile.parentFile, "test-reflux.db-wal").delete()
        // Wipe any stored key from previous test runs so we get a clean slate.
        File(context.filesDir, "dbkey.prefs").delete()
    }

    @After
    fun tearDown() {
        AppDatabase.closeForTest()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        File(context.getDatabasePath("test-reflux.db").parentFile, "test-reflux.db").delete()
        File(context.getDatabasePath("test-reflux.db").parentFile, "test-reflux.db-shm").delete()
        File(context.getDatabasePath("test-reflux.db").parentFile, "test-reflux.db-wal").delete()
    }

    @Test
    fun `database opens, accepts writes, and reads back across simulated process restart`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // First "process": open, insert, close.
        val foodId = AppDatabase.getInstance(context).foodDao().insertFood(
            FoodEntity(name = "Testbrot", category = "Milch & Getreide")
        )
        assertNotNull(foodId)
        assertEquals(true, foodId > 0L)
        // Drain pending onCreate seed jobs on the IO dispatcher so they finish
        // before we close the DB; otherwise the close races with the seed
        // coroutine and the next reopen sees a "re-open an already-closed
        // object" exception from the still-running seed.
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(50)
        }
        AppDatabase.closeForTest()

        // Second "process": reopen, the data must still be there.
        val foods = AppDatabase.getInstance(context).foodDao().getAllFoods().first()
        val found = foods.any { it.name == "Testbrot" }
        assertEquals("Encrypted DB must persist data across reopen", true, found)
    }

    @Test
    fun `app database reports sqlcipher encryption is active when library is loaded`() {
        // If the SQLCipher native library loads successfully in this test process,
        // AppDatabase.attachSqlcipher must have attached a non-null openHelperFactory
        // (verified indirectly: the DB file on disk is NOT a valid SQLite header
        // until the key is applied).
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppDatabase.getInstance(context) // forces DB creation
        AppDatabase.closeForTest()

        val dbFile = context.getDatabasePath("test-reflux.db")
        // A freshly-created SQLCipher-encrypted DB has the SQLCipher magic header
        // ("SQLite format 3\0" but with bytes scrambled by encryption — actually
        // SQLCipher 4.x writes the same header, but the first 16 bytes of page 1
        // differ from plaintext). For a robust check, we just verify that
        // opening the file with a WRONG key produces an error.
        val wrongKey = ByteArray(32) { 0x42 }
        val opened = try {
            AppDatabase.openWithRawKeyForTest(context, dbFile.absolutePath, wrongKey)
            true
        } catch (_: Throwable) {
            false
        }
        // If SQLCipher is active, opening with a wrong key throws.
        // If SQLCipher is not active, opening succeeds with any key.
        // We assert: opening with a wrong key MUST fail, proving encryption is on.
        assertEquals(
            "Opening the DB with the wrong key must fail — proves SQLCipher is active",
            false, opened
        )
    }
}
