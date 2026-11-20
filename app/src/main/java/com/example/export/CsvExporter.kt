package com.example.export

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds a UTF-8 CSV string containing all meals and symptoms for export.
 *
 * Format:
 *  - Comma-separated, German locale for dates and decimal numbers
 *  - Lines delimited by `\n`
 *  - Section headers are comment-prefixed (`# MAHLZEITEN` / `# SYMPTOME`) so a
 *    spreadsheet app ignores them but a human reading the raw file sees structure.
 *    The actual tabular CSV header follows the section marker on the next line.
 *  - Fields containing commas, quotes or newlines are quoted with `"`; embedded
 *    quotes are doubled (`""`) per RFC 4180.
 *  - The output ends with a single trailing newline so appending is safe.
 */
object CsvExporter {

    data class MealRow(
        val timestampMillis: Long,
        val mealType: String,
        val portion: String,
        val foodNames: List<String>,
        val notes: String
    )

    data class SymptomRow(
        val timestampMillis: Long,
        val symptomType: String,
        val symptomTypes: List<String> = listOf(symptomType),
        val intensity: Int,
        val durationMinutes: Int?,
        val notes: String,
        val activities: List<String> = emptyList(),
        val remedies: List<String> = emptyList()
    )

    private val MEAL_HEADER = "Datum,Zeit,Mahlzeitentyp,Portion,Lebensmittel,Notiz"
    private val SYMPTOM_HEADER =
        "Datum,Zeit,Symptom,Symptomarten,Intensitaet,DauerMin,Taetigkeiten,Massnahmen,Notiz"

    fun buildCsv(
        meals: List<MealRow>,
        symptoms: List<SymptomRow>,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.GERMAN
    ): String {
        val dateFmt = SimpleDateFormat("dd.MM.yyyy", locale).apply { this.timeZone = timeZone }
        val timeFmt = SimpleDateFormat("HH:mm", locale).apply { this.timeZone = timeZone }
        val sb = StringBuilder()
        sb.append("# MAHLZEITEN\n")
        sb.append(MEAL_HEADER).append('\n')
        for (m in meals) {
            val d = Date(m.timestampMillis)
            sb.append(dateFmt.format(d)).append(',')
            sb.append(timeFmt.format(d)).append(',')
            sb.append(quote(m.mealType)).append(',')
            sb.append(quote(m.portion)).append(',')
            sb.append(quote(m.foodNames.joinToString("; "))).append(',')
            sb.append(quote(m.notes)).append('\n')
        }
        sb.append('\n')
        sb.append("# SYMPTOME\n")
        sb.append(SYMPTOM_HEADER).append('\n')
        for (s in symptoms) {
            val d = Date(s.timestampMillis)
            val types = s.symptomTypes.ifEmpty { listOf(s.symptomType) }
            sb.append(dateFmt.format(d)).append(',')
            sb.append(timeFmt.format(d)).append(',')
            sb.append(quote(s.symptomType)).append(',')
            sb.append(quote(types.joinToString("; "))).append(',')
            sb.append(s.intensity.toString()).append(',')
            sb.append(s.durationMinutes?.toString() ?: "").append(',')
            sb.append(quote(s.activities.joinToString("; "))).append(',')
            sb.append(quote(s.remedies.joinToString("; "))).append(',')
            sb.append(quote(s.notes)).append('\n')
        }
        return sb.toString()
    }

    private fun quote(s: String): String {
        if (s.isEmpty()) return ""
        val needs = s.contains(',') || s.contains('"') || s.contains('\n') || s.contains('\r')
        if (!needs) return s
        return "\"" + s.replace("\"", "\"\"") + "\""
    }
}
