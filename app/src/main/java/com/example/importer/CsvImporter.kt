package com.example.importer

import com.example.data.model.ImportedEntry
import com.example.data.model.MealTypes
import com.example.data.model.SymptomTypes
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parser for RefluxTracker backups.
 *
 * Supported inputs:
 *  - Legacy semicolon backups (`TYP;DATUM;UHRZEIT;TIMESTAMP_MS;...`), including
 *    the HTML wrapper produced by Android's Bluetooth share sheet.
 *  - The app's current comma-separated export (`# MAHLZEITEN` / `# SYMPTOME`).
 *
 * The parser is deliberately free of Android dependencies so it can be covered
 * by fast JVM unit tests. It only translates rows into [ImportedEntry] values;
 * resolving the referenced catalog entries is done by the repository.
 */
object CsvImporter {

    data class ParsedCsv(
        val entries: List<ImportedEntry>,
        val skippedRows: Int,
        val warnings: List<String>
    )

    fun parse(rawText: String, timeZone: TimeZone = TimeZone.getDefault()): ParsedCsv {
        val text = normalizeInput(rawText)
        if (text.isBlank()) {
            throw IllegalArgumentException("Die Datei ist leer.")
        }

        val headerLine = text.lineSequence()
            .map { it.trim().removePrefix("\uFEFF") }
            .firstOrNull { line ->
                val upper = line.uppercase(Locale.GERMAN)
                upper.contains("MAHLZEIT_TYP") ||
                    upper.contains("MAHLZEITENTYP") ||
                    upper.contains("SYMPTOMARTEN")
            }
            ?: throw IllegalArgumentException(
                "Kein gültiges RefluxTracker-Format gefunden. " +
                    "Erwartet wird eine CSV- oder HTML-Sicherung mit einer Kopfzeile."
            )

        val delimiter = if (headerLine.count { it == ';' } > headerLine.count { it == ',' }) ';' else ','
        val rows = tokenize(text, delimiter)

        val entries = mutableListOf<ImportedEntry>()
        val warnings = mutableListOf<String>()
        var skippedRows = 0
        var columns: Map<String, Int> = emptyMap()
        var legacyFormat = false
        var mode: EntryMode? = null
        var dataRow = 0

        for (row in rows) {
            val fields = row.fields
            if (fields.all { it.isBlank() }) continue

            val first = fields.firstOrNull()?.trim().orEmpty()
            if (first.startsWith("#")) {
                val marker = first.uppercase(Locale.GERMAN)
                if (marker.contains("MAHLZEIT")) mode = EntryMode.MEAL
                if (marker.contains("SYMPTOM")) mode = EntryMode.SYMPTOM
                continue
            }

            val headerMap = fields.mapIndexedNotNull { index, value ->
                logicalColumn(value)?.let { it to index }
            }.toMap()

            when {
                "TYP" in headerMap && "TIMESTAMP" in headerMap -> {
                    columns = headerMap
                    legacyFormat = true
                    mode = null
                    continue
                }

                "MEAL_TYPE" in headerMap && "FOODS" in headerMap -> {
                    columns = headerMap
                    legacyFormat = false
                    mode = EntryMode.MEAL
                    continue
                }

                "SYMPTOMS" in headerMap && "INTENSITY" in headerMap -> {
                    columns = headerMap
                    legacyFormat = false
                    mode = EntryMode.SYMPTOM
                    continue
                }
            }

            // Preamble such as the file name line emitted by some share targets.
            if (columns.isEmpty()) continue

            dataRow++
            val type = if (legacyFormat) {
                first.uppercase(Locale.GERMAN)
            } else {
                mode?.name
            }

            fun value(column: String): String =
                columns[column]?.let { fields.getOrNull(it)?.trim().orEmpty() }.orEmpty()

            when (type) {
                "MEAL", "MAHLZEIT" -> {
                    val timestamp = resolveTimestamp(
                        epochRaw = value("TIMESTAMP"),
                        dateRaw = value("DATE"),
                        timeRaw = value("TIME"),
                        timeZone = timeZone
                    )
                    if (timestamp == null) {
                        skippedRows++
                        warnings += "Datensatz $dataRow: kein gültiger Zeitpunkt, übersprungen."
                        continue
                    }

                    val mealType = mapMealType(value("MEAL_TYPE"))
                    if (mealType.isBlank()) {
                        warnings += "Datensatz $dataRow: Mahlzeitentyp fehlt, „Sonstiges“ verwendet."
                    }
                    entries += ImportedEntry.Meal(
                        timestamp = timestamp,
                        mealType = mealType.ifBlank { MealTypes.SONSTIGES },
                        portion = mapPortion(value("PORTION")),
                        foods = splitList(value("FOODS")),
                        notes = value("NOTES")
                    )
                }

                "SYMPTOM" -> {
                    val timestamp = resolveTimestamp(
                        epochRaw = value("TIMESTAMP"),
                        dateRaw = value("DATE"),
                        timeRaw = value("TIME"),
                        timeZone = timeZone
                    )
                    if (timestamp == null) {
                        skippedRows++
                        warnings += "Datensatz $dataRow: kein gültiger Zeitpunkt, übersprungen."
                        continue
                    }

                    val types = splitList(value("SYMPTOMS"))
                        .map(::mapSymptomType)
                        .distinct()
                    if (types.isEmpty()) {
                        skippedRows++
                        warnings += "Datensatz $dataRow: keine Symptomart, übersprungen."
                        continue
                    }

                    val rawIntensity = value("INTENSITY")
                    val parsedIntensity = rawIntensity.toIntOrNull()
                    if (parsedIntensity == null) {
                        skippedRows++
                        warnings += "Datensatz $dataRow: Intensität fehlt oder ist ungültig, übersprungen."
                        continue
                    }
                    val intensity = parsedIntensity.coerceIn(0, 10)
                    if (intensity != parsedIntensity) {
                        warnings += "Datensatz $dataRow: Intensität $parsedIntensity auf $intensity begrenzt."
                    }

                    val rawDuration = value("DURATION")
                    val duration = rawDuration.toIntOrNull()?.takeIf { it >= 0 }
                    if (rawDuration.isNotBlank() && duration == null) {
                        warnings += "Datensatz $dataRow: ungültige Dauer „$rawDuration“, leer übernommen."
                    }

                    entries += ImportedEntry.Symptom(
                        timestamp = timestamp,
                        symptomTypes = types,
                        intensity = intensity,
                        durationMinutes = duration,
                        activities = splitList(value("ACTIVITIES")),
                        remedies = splitList(value("REMEDIES")),
                        notes = value("NOTES")
                    )
                }

                else -> {
                    skippedRows++
                    warnings += "Datensatz $dataRow: unbekannter Typ „${first.take(30)}“, übersprungen."
                }
            }
        }

        if (entries.isEmpty() && skippedRows > 0) {
            throw IllegalArgumentException(
                "Keine gültigen Datensätze gefunden. " +
                    (warnings.firstOrNull()?.let { "Erster Hinweis: $it" } ?: "")
            )
        }

        return ParsedCsv(entries = entries, skippedRows = skippedRows, warnings = warnings)
    }

    // ---------------------------------------------------------------------
    // Input normalisation
    // ---------------------------------------------------------------------

    /**
     * Android Bluetooth shares often wrap the CSV in a tiny HTML page with
     * `<a href="tel:...">` links and `<br>` separators. Detect and unwrap it;
     * plain CSV/UTF-8 input is returned unchanged.
     */
    private fun normalizeInput(raw: String): String {
        val text = raw.removePrefix("\uFEFF")
        val probe = text.take(4096).lowercase(Locale.GERMAN)
        val looksLikeHtml = probe.contains("<html") ||
            probe.contains("<body") ||
            probe.contains("<br") ||
            probe.contains("<table")

        if (!looksLikeHtml) return text

        var unwrapped = text
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|tr|li|h[1-6])>"), "\n")
            .replace(Regex("(?s)<[^>]+>"), "")
        unwrapped = unescapeHtml(unwrapped)
        return unwrapped
    }

    private fun unescapeHtml(value: String): String {
        var text = value
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
        text = Regex("&#(x?)([0-9a-fA-F]+);").replace(text) { match ->
            val hex = match.groupValues[1].isNotEmpty()
            val code = match.groupValues[2].toIntOrNull(if (hex) 16 else 10)
            if (code != null && code in 0..0x10FFFF) {
                String(Character.toChars(code))
            } else {
                match.value
            }
        }
        // Last, so escaped entities are not decoded twice.
        return text.replace("&amp;", "&")
    }

    // ---------------------------------------------------------------------
    // CSV tokenizer
    // ---------------------------------------------------------------------

    private data class CsvRow(val fields: List<String>, val lineNumber: Int)

    private fun tokenize(text: String, delimiter: Char): List<CsvRow> {
        val rows = mutableListOf<CsvRow>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var line = 1
        var rowLine = 1
        var index = 0

        fun finishField() {
            fields += field.toString()
            field.setLength(0)
        }

        fun finishRow() {
            finishField()
            rows += CsvRow(fields, rowLine)
            fields = mutableListOf()
            rowLine = line + 1
        }

        while (index < text.length) {
            val ch = text[index]
            if (inQuotes) {
                when {
                    ch == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                        field.append('"')
                        index += 2
                    }

                    ch == '"' -> {
                        inQuotes = false
                        index++
                    }

                    else -> {
                        if (ch == '\n') line++
                        field.append(ch)
                        index++
                    }
                }
            } else {
                when (ch) {
                    '"' -> {
                        // Quotes only open a field at its start; elsewhere they
                        // are kept as literal text for lenient parsing.
                        if (field.isEmpty()) inQuotes = true else field.append(ch)
                        index++
                    }

                    delimiter -> {
                        finishField()
                        index++
                    }

                    '\n' -> {
                        finishRow()
                        line++
                        index++
                    }

                    '\r' -> {
                        finishRow()
                        line++
                        if (index + 1 < text.length && text[index + 1] == '\n') {
                            index += 2
                        } else {
                            index++
                        }
                    }

                    else -> {
                        field.append(ch)
                        index++
                    }
                }
            }
        }

        if (inQuotes) {
            throw IllegalArgumentException(
                "Die CSV-Datei enthält ein nicht geschlossenes Anführungszeichen."
            )
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            finishRow()
        }
        return rows
    }

    // ---------------------------------------------------------------------
    // Headers and value mapping
    // ---------------------------------------------------------------------

    private fun logicalColumn(raw: String): String? = when (normalize(raw)) {
        "typ" -> "TYP"
        "datum" -> "DATE"
        "uhrzeit", "zeit" -> "TIME"
        "timestamp_ms", "timestamp" -> "TIMESTAMP"
        "mahlzeit_typ", "mahlzeitentyp" -> "MEAL_TYPE"
        "portionsgroesse", "portion" -> "PORTION"
        "lebensmittel" -> "FOODS"
        "symptome", "symptomarten", "symptom" -> "SYMPTOMS"
        "intensitaet", "intensity" -> "INTENSITY"
        "dauer_min", "dauermin", "dauer" -> "DURATION"
        "taetigkeiten", "taetigkeit" -> "ACTIVITIES"
        "massnahmen", "massnahme" -> "REMEDIES"
        "notizen", "notiz", "notes" -> "NOTES"
        else -> null
    }

    private fun splitList(raw: String): List<String> {
        return raw.split('|', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun mapMealType(raw: String): String = when (key(raw)) {
        "fruehstueck", "fruestueck", "breakfast" -> MealTypes.FRUEHSTUECK
        "mittagessen", "mittag", "lunch" -> MealTypes.MITTAGESSEN
        "abendessen", "abend", "dinner" -> MealTypes.ABENDESSEN
        "snack", "zwischenmahlzeit" -> MealTypes.SNACK
        "getraenk", "getraenke", "drink" -> MealTypes.GETRAENK
        "sonstiges", "sonstige", "other" -> MealTypes.SONSTIGES
        else -> raw.trim()
    }

    private fun mapPortion(raw: String): String = when (key(raw)) {
        "klein", "small" -> "Klein"
        "mittel", "medium", "normal" -> "Normal"
        "gross", "large" -> "Groß"
        else -> raw.trim()
    }

    /**
     * Older backups used longer symptom names (for example
     * „Druckgefühl / Druck in der Brust“). Map the known variants onto the
     * current catalogue while leaving genuinely unknown names untouched, so no
     * information is silently lost.
     */
    private fun mapSymptomType(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        return when (key(trimmed)) {
            "sodbrennen", "sodbrennen / reflux", "reflux" -> SymptomTypes.SODBRENNEN
            "saeurerueckfluss", "saeureruckfluss", "saeure reflux" -> SymptomTypes.SAEURERUECKFLUSS
            "aufstossen", "saures aufstossen" -> SymptomTypes.AUFSTOSSEN
            "raeuspern", "raeuespern" -> SymptomTypes.RAEUSPERN
            "husten", "reizhusten" -> SymptomTypes.HUSTEN
            "halsbrennen", "brennen im rachen" -> SymptomTypes.HALSBRENNEN
            "druckgefuehl",
            "druckgefuehl / druck in der brust",
            "druck in der brust",
            "magendruck",
            "magendruck / voellegefuehl",
            "voellegefuehl" -> SymptomTypes.DRUCKGEFUEHL
            "uebelkeit" -> SymptomTypes.UEBELKEIT
            "blaehungen", "blaehungen / voellegefuehl" -> SymptomTypes.BLAEHUNGEN
            "schlafprobleme" -> SymptomTypes.SCHLAFPROBLEME
            else -> trimmed
        }
    }

    private fun normalize(value: String): String = key(value).replace(Regex("\\s+"), " ")

    private fun key(value: String): String = value.trim()
        .lowercase(Locale.GERMAN)
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")

    // ---------------------------------------------------------------------
    // Date/time parsing
    // ---------------------------------------------------------------------

    private fun resolveTimestamp(
        epochRaw: String,
        dateRaw: String,
        timeRaw: String,
        timeZone: TimeZone
    ): Long? {
        parseEpochMillis(epochRaw)?.let { return it }
        return parseDateTime(dateRaw, timeRaw, timeZone)
    }

    private fun parseEpochMillis(raw: String): Long? {
        val value = raw.trim().toLongOrNull() ?: return null
        return when {
            value >= 100_000_000_000L -> value
            // Tolerate old backups that stored seconds instead of milliseconds.
            value >= 1_000_000_000L -> value * 1_000L
            else -> null
        }
    }

    private fun parseDateTime(dateRaw: String, timeRaw: String, timeZone: TimeZone): Long? {
        val date = dateRaw.trim()
        if (date.isEmpty()) return null
        val time = timeRaw.trim().ifEmpty { "00:00" }
        val input = "$date $time"

        for (datePattern in DATE_PATTERNS) {
            for (timePattern in TIME_PATTERNS) {
                val format = SimpleDateFormat("$datePattern $timePattern", Locale.GERMANY).apply {
                    this.timeZone = timeZone
                    isLenient = false
                }
                try {
                    format.parse(input)?.let { return it.time }
                } catch (_: ParseException) {
                    // Try the next pattern.
                }
            }
        }
        return null
    }

    private enum class EntryMode { MEAL, SYMPTOM }

    private val DATE_PATTERNS = listOf(
        "yyyy-MM-dd",
        "dd.MM.yyyy",
        "d.M.yyyy",
        "yyyy/MM/dd",
        "dd.MM.yy"
    )

    private val TIME_PATTERNS = listOf(
        "HH:mm:ss",
        "H:mm:ss",
        "HH:mm",
        "H:mm"
    )
}
