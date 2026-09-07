package com.example.export

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Behaviour spec for [CsvFileWriter].
 *
 * Contract:
 *  - Writes UTF-8 CSV into the cache directory under a unique filename
 *  - Returns a File pointing at the written file (caller wires it into a
 *    FileProvider for sharing via Intent)
 *  - Filename encodes the export date so multiple exports don't collide in
 *    the user's "Downloads" / "Recent" view
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CsvFileWriterTest {

    @Test
    fun `write returns a file under the cache directory`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = CsvFileWriter.write(context, csvContent = "# HEADER\nfoo,bar\n")
        assertNotNull(file)
        assertTrue("file must exist after write", file.exists())
        assertTrue("file must be inside the cache dir", file.absolutePath.startsWith(context.cacheDir.absolutePath))
    }

    @Test
    fun `write produces a file whose contents match the given CSV string exactly`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val content = "# MAHLZEITEN\nDatum,Zeit\n14.11.2023,23:13\n"
        val file = CsvFileWriter.write(context, csvContent = content)
        val onDisk = file.readText(Charsets.UTF_8)
        assertEquals("UTF-8 round-trip must be exact", content, onDisk)
    }

    @Test
    fun `write produces a filename that encodes the export date so two exports don't collide silently`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val f1 = CsvFileWriter.write(context, csvContent = "a\n")
        val f2 = CsvFileWriter.write(context, csvContent = "b\n")
        // Different filenames → no silent overwrite.
        assertTrue("Two writes must produce different filenames", f1.name != f2.name)
        // Both filenames must carry a date component (yyyyMMdd).
        val datePattern = Regex("""\d{8}""")
        assertTrue("filename must contain a date: ${f1.name}", datePattern.containsMatchIn(f1.name))
        assertTrue("filename must contain a date: ${f2.name}", datePattern.containsMatchIn(f2.name))
    }

    @Test
    fun `write produces a file in a stable subdirectory so it can be exposed via FileProvider`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = CsvFileWriter.write(context, csvContent = "x\n")
        // The FileProvider in the manifest is wired to files-path "exports" or
        // external-cache-path "exports" — either way, the parent dir must be
        // named "exports" so the path matches the FileProvider config.
        assertEquals(
            "parent directory must be 'exports' to match the FileProvider config",
            "exports", file.parentFile?.name
        )
    }
}
