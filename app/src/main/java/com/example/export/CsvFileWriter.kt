package com.example.export

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a UTF-8 CSV string into the app's cache directory under
 * `cacheDir/exports/refluxtracker-export-yyyyMMdd-HHmmss-<nanos>.csv`.
 *
 * The parent directory `exports` matches the FileProvider config in the
 * manifest (`cache-path="exports"`), so the returned [File] can be wrapped
 * in a `content://` URI and shared via Intent without exposing internal
 * storage.
 */
object CsvFileWriter {

    fun write(context: Context, csvContent: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
        val file = File(dir, "refluxtracker-export-$dateStamp-${System.nanoTime()}.csv")
        file.writeText(csvContent, Charsets.UTF_8)
        return file
    }
}
