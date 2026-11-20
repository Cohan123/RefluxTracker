package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Guards the database migration policy:
 *
 *  - A schema change must be accompanied by a real Migration. The database
 *    builder intentionally does **not** use `fallbackToDestructiveMigration`,
 *    because that would silently delete every meal and symptom on an unknown
 *    version.
 *  - Opening a database with an unknown version therefore has to fail loudly
 *    and must leave the existing file (and its data) untouched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationPolicyTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val dbFile: File
        get() = context.getDatabasePath("refluxtrack_database")

    @After
    fun tearDown() {
        AppDatabase.closeForTest()
        deleteDbFiles()
    }

    @Test
    fun `unknown database version fails loudly instead of wiping data`() {
        AppDatabase.closeForTest()
        dbFile.parentFile?.mkdirs()
        deleteDbFiles()

        // Simulate an existing database written by a (future or foreign) schema
        // version that this build does not know how to migrate.
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { raw ->
            raw.execSQL(
                "CREATE TABLE marker (id INTEGER PRIMARY KEY AUTOINCREMENT, value TEXT NOT NULL)"
            )
            raw.execSQL("INSERT INTO marker (value) VALUES ('keep-me')")
            raw.version = 99
        }

        // Room opens the database lazily on the first access, so trigger it.
        val failure = runCatching {
            AppDatabase.getInstance(context).openHelper.writableDatabase
        }.exceptionOrNull()

        assertNotNull(
            "An unknown database version must fail instead of being recreated destructively",
            failure
        )

        // The failed open must not have touched the existing data.
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            raw.rawQuery("SELECT value FROM marker", null).use { cursor ->
                assertTrue("Marker row is missing - the database was wiped", cursor.moveToFirst())
                assertEquals("keep-me", cursor.getString(0))
            }
        }
    }

    private fun deleteDbFiles() {
        listOf("", "-shm", "-wal").forEach { suffix ->
            File(dbFile.path + suffix).delete()
        }
    }
}
