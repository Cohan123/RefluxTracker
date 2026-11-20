package com.example.importer

import com.example.data.model.ImportedEntry
import com.example.export.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Behaviour spec for the backup parser. Covers both the legacy semicolon
 * format (including the HTML wrapper used by Bluetooth shares) and the app's
 * current comma-separated export, so export -> import round-trips.
 */
class CsvImporterTest {

    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    @Test
    fun `parses legacy semicolon backup and maps catalog aliases`() {
        val csv = """
            TYP;DATUM;UHRZEIT;TIMESTAMP_MS;MAHLZEIT_TYP;PORTIONSGROESSE;LEBENSMITTEL;SYMPTOME;INTENSITAET;DAUER_MIN;TAETIGKEITEN;MASSNAHMEN;NOTIZEN
            MAHLZEIT;2026-09-08;06:30;1788841833921;FRUEHSTUECK;MITTEL;Kaffee|Marmeladenbrot;;;;;;
            SYMPTOM;2026-09-08;07:00;1788843600000;;;;Sodbrennen|Druckgefühl / Druck in der Brust;6;30;Liegen;Wasser getrunken;
            SYMPTOM;2026-09-09;08:00;1788933600000;;;;Reizhusten;3;;Sitzen;;
        """.trimIndent()

        val parsed = CsvImporter.parse(csv, berlin)

        assertEquals(3, parsed.entries.size)
        assertEquals(0, parsed.skippedRows)

        val meal = parsed.entries[0] as ImportedEntry.Meal
        assertEquals(1788841833921L, meal.timestamp)
        assertEquals("Frühstück", meal.mealType)
        assertEquals("Normal", meal.portion)
        assertEquals(listOf("Kaffee", "Marmeladenbrot"), meal.foods)
        assertEquals("", meal.notes)

        val symptom = parsed.entries[1] as ImportedEntry.Symptom
        assertEquals(listOf("Sodbrennen", "Druckgefühl"), symptom.symptomTypes)
        assertEquals(6, symptom.intensity)
        assertEquals(30, symptom.durationMinutes)
        assertEquals(listOf("Liegen"), symptom.activities)
        assertEquals(listOf("Wasser getrunken"), symptom.remedies)

        val cough = parsed.entries[2] as ImportedEntry.Symptom
        assertEquals(listOf("Husten"), cough.symptomTypes)
        assertNull(cough.durationMinutes)
    }

    @Test
    fun `unwraps the HTML page produced by Android Bluetooth sharing`() {
        val html = """
            <html><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/></head><body>
            <a href="http://RefluxTrack_Backup_1.csv">RefluxTrack_Backup_1.csv</a><br><br>
            TYP;DATUM;UHRZEIT;TIMESTAMP_MS;MAHLZEIT_TYP;PORTIONSGROESSE;LEBENSMITTEL;SYMPTOME;INTENSITAET;DAUER_MIN;TAETIGKEITEN;MASSNAHMEN;NOTIZEN<br>
            MAHLZEIT;<a href="tel:2026-09-08">2026-09-08</a>;06:30;<a href="tel:1788841833921">1788841833921</a>;GETRAENK;KLEIN;Kaffee;;;;;;<br>
            SYMPTOM;2026-09-08;07:00;1788843600000;;;;Sodbrennen;4;15;Liegen;;<br>
            </body></html>
        """.trimIndent()

        val parsed = CsvImporter.parse(html, berlin)

        assertEquals(2, parsed.entries.size)
        val meal = parsed.entries[0] as ImportedEntry.Meal
        assertEquals("Getränk", meal.mealType)
        assertEquals("Klein", meal.portion)
        assertEquals(listOf("Kaffee"), meal.foods)
        val symptom = parsed.entries[1] as ImportedEntry.Symptom
        assertEquals(listOf("Sodbrennen"), symptom.symptomTypes)
    }

    @Test
    fun `parses a current export and round-trips timestamp and details`() {
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
            .apply { timeZone = berlin }
            .parse("14.11.2023 22:13")!!
            .time
        val csv = CsvExporter.buildCsv(
            meals = listOf(
                CsvExporter.MealRow(
                    timestampMillis = timestamp,
                    mealType = "Abendessen",
                    portion = "Groß",
                    foodNames = listOf("Pasta mit Tomatensauce", "Käse"),
                    notes = "mit Kaffee, danach"
                )
            ),
            symptoms = listOf(
                CsvExporter.SymptomRow(
                    timestampMillis = timestamp + 3_600_000L,
                    symptomType = "Sodbrennen",
                    symptomTypes = listOf("Sodbrennen", "Aufstoßen"),
                    intensity = 7,
                    durationMinutes = 45,
                    notes = "zwei\nZeilen",
                    activities = listOf("Liegen"),
                    remedies = listOf("Wasser")
                )
            ),
            timeZone = berlin,
            locale = Locale.GERMANY
        )

        val parsed = CsvImporter.parse(csv, berlin)

        assertEquals(2, parsed.entries.size)
        val meal = parsed.entries[0] as ImportedEntry.Meal
        assertEquals(timestamp, meal.timestamp)
        assertEquals("Abendessen", meal.mealType)
        assertEquals("Groß", meal.portion)
        assertEquals(listOf("Pasta mit Tomatensauce", "Käse"), meal.foods)
        assertEquals("mit Kaffee, danach", meal.notes)

        val symptom = parsed.entries[1] as ImportedEntry.Symptom
        assertEquals(timestamp + 3_600_000L, symptom.timestamp)
        assertEquals(listOf("Sodbrennen", "Aufstoßen"), symptom.symptomTypes)
        assertEquals(7, symptom.intensity)
        assertEquals(45, symptom.durationMinutes)
        assertEquals("zwei\nZeilen", symptom.notes)
        assertEquals(listOf("Liegen"), symptom.activities)
        assertEquals(listOf("Wasser"), symptom.remedies)
    }

    @Test
    fun `handles quoted delimiters and multiline notes`() {
        val csv = """
            TYP;DATUM;UHRZEIT;TIMESTAMP_MS;MAHLZEIT_TYP;PORTIONSGROESSE;LEBENSMITTEL;SYMPTOME;INTENSITAET;DAUER_MIN;TAETIGKEITEN;MASSNAHMEN;NOTIZEN
            MAHLZEIT;2026-09-08;06:30;1788841833921;FRUEHSTUECK;MITTEL;"Brot; Toast";;;;;;"Notiz; mit
            Zeilenumbruch"
        """.trimIndent()

        val parsed = CsvImporter.parse(csv, berlin)

        val meal = parsed.entries.single() as ImportedEntry.Meal
        assertEquals(listOf("Brot", "Toast"), meal.foods)
        assertEquals("Notiz; mit\nZeilenumbruch", meal.notes)
    }

    @Test
    fun `skips malformed rows and reports warnings`() {
        val csv = """
            TYP;DATUM;UHRZEIT;TIMESTAMP_MS;MAHLZEIT_TYP;PORTIONSGROESSE;LEBENSMITTEL;SYMPTOME;INTENSITAET;DAUER_MIN;TAETIGKEITEN;MASSNAHMEN;NOTIZEN
            MAHLZEIT;2026-09-08;06:30;1788841833921;FRUEHSTUECK;MITTEL;Kaffee;;;;;;
            SYMPTOM;not-a-date;99:99;not-a-number;;;;Sodbrennen;6;30;Liegen;;
            UNBEKANNT;2026-09-08;08:00;1788847200000;SNACK;KLEIN;Apfel;;;;;;
        """.trimIndent()

        val parsed = CsvImporter.parse(csv, berlin)

        assertEquals(1, parsed.entries.size)
        assertEquals(2, parsed.skippedRows)
        assertTrue(parsed.warnings.any { it.contains("kein gültiger Zeitpunkt") })
        assertTrue(parsed.warnings.any { it.contains("unbekannter Typ") })
    }
}
