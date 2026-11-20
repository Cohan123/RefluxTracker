package com.example.importer

import android.content.Context
import android.net.Uri
import com.example.data.repository.RefluxRepository
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class CsvImportSummary(
    val mealsImported: Int,
    val symptomsImported: Int,
    val duplicatesSkipped: Int,
    val skippedRows: Int,
    val warnings: List<String> = emptyList()
) {
    val totalImported: Int
        get() = mealsImported + symptomsImported
}

/**
 * Android-facing entry point for importing a backup selected through the
 * Storage Access Framework. Parsing lives in [CsvImporter]; writing to the
 * database is done atomically by [RefluxRepository.importEntries].
 */
object RefluxDataImport {

    private const val MAX_FILE_BYTES = 20 * 1024 * 1024

    suspend fun importFromUri(
        context: Context,
        repository: RefluxRepository,
        uri: Uri
    ): CsvImportSummary {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalArgumentException("Die ausgewählte Datei konnte nicht geöffnet werden.")

        require(bytes.size <= MAX_FILE_BYTES) {
            "Die Datei ist zu groß (maximal 20 MB)."
        }
        return importFromText(decode(bytes), repository)
    }

    suspend fun importFromText(
        text: String,
        repository: RefluxRepository
    ): CsvImportSummary {
        val parsed = CsvImporter.parse(text)
        val written = repository.importEntries(parsed.entries)
        return CsvImportSummary(
            mealsImported = written.mealsImported,
            symptomsImported = written.symptomsImported,
            duplicatesSkipped = written.duplicatesSkipped,
            skippedRows = parsed.skippedRows,
            warnings = parsed.warnings
        )
    }

    /**
     * Backups are normally UTF-8. Older Windows exports may use Windows-1252;
     * fall back to it only when the bytes are not valid UTF-8.
     */
    private fun decode(bytes: ByteArray): String {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1252"))
        }
    }
}
