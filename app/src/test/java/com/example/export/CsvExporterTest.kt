package com.example.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour spec for [CsvExporter].
 *
 * Output contract:
 *  - UTF-8 encoded
 *  - German decimal separator (,), German date format (dd.MM.yyyy), 24h time
 *  - Header row first, then rows
 *  - Multi-line notes and embedded commas are quoted and doubled-quoted per RFC 4180
 *  - Empty / missing fields are rendered as the empty string (between two commas)
 *  - One blank line between meal rows and symptom rows, clearly separated by
 *    section headers ("# MAHLZEITEN" / "# SYMPTOME") in comment-prefixed lines
 *    so spreadsheet apps can still parse the table
 */
class CsvExporterTest {

    @Test
    fun `export with no data produces only headers and section markers`() {
        val csv = CsvExporter.buildCsv(
            meals = emptyList(),
            symptoms = emptyList()
        )
        // Section markers and headers, nothing else.
        assertTrue("must contain MAHLZEITEN section", csv.contains("# MAHLZEITEN"))
        assertTrue("must contain SYMPTOME section", csv.contains("# SYMPTOME"))
        assertTrue("must contain meal header", csv.contains("Datum,Zeit,Mahlzeitentyp,Portion,Lebensmittel,Notiz"))
        assertTrue(
            "must contain symptom header",
            csv.contains(
                "Datum,Zeit,Symptom,Symptomarten,Intensitaet,DauerMin,Taetigkeiten,Massnahmen,Notiz"
            )
        )
    }

    @Test
    fun `meal row encodes German date and time and joined food list`() {
        val meals = listOf(
            CsvExporter.MealRow(
                timestampMillis = 1700000000000L, // 2023-11-14 22:13:20 UTC, exporter uses local TZ
                mealType = "Mittagessen",
                portion = "Normal",
                foodNames = listOf("Spaghetti", "Tomatensoße"),
                notes = ""
            )
        )
        val csv = CsvExporter.buildCsv(meals = meals, symptoms = emptyList())
        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val mealRow = lines.first { it.contains("Mittagessen") }
        assertTrue(
            "meal row must contain both food names joined by '; ', got: $mealRow",
            mealRow.contains("Spaghetti; Tomatensoße")
        )
        assertTrue("meal row must contain meal type", mealRow.contains("Mittagessen"))
        assertTrue("meal row must contain portion", mealRow.contains("Normal"))
    }

    @Test
    fun `notes containing comma and newline are quoted and escaped per RFC 4180`() {
        val meals = listOf(
            CsvExporter.MealRow(
                timestampMillis = 1700000000000L,
                mealType = "Snack",
                portion = "Klein",
                foodNames = listOf("Schokolade"),
                notes = "mit Kaffee, danach,\nBauchschmerzen"
            )
        )
        val csv = CsvExporter.buildCsv(meals = meals, symptoms = emptyList())
        // The notes cell must be quoted and contain "" for each " and the newline preserved.
        assertTrue(
            "notes must be quoted: " + csv,
            csv.contains("\"mit Kaffee, danach,\nBauchschmerzen\"")
        )
    }

    @Test
    fun `symptom row encodes intensity and optional duration`() {
        val symptoms = listOf(
            CsvExporter.SymptomRow(
                timestampMillis = 1700000000000L,
                symptomType = "Sodbrennen",
                intensity = 7,
                durationMinutes = 45,
                notes = ""
            ),
            CsvExporter.SymptomRow(
                timestampMillis = 1700000004000L,
                symptomType = "Husten",
                intensity = 3,
                durationMinutes = null,
                notes = "trocken"
            )
        )
        val csv = CsvExporter.buildCsv(meals = emptyList(), symptoms = symptoms)
        val lines = csv.lines().filter { it.isNotBlank() }
        val sod = lines.first { it.contains("Sodbrennen") }
        val hu = lines.first { it.contains("Husten") }
        assertTrue("sodbrennen row must contain intensity 7", sod.contains(",7,"))
        assertTrue("sodbrennen row must contain duration 45", sod.contains(",45,"))
        // For Husten, durationMinutes is null → empty field between two commas.
        // Expected: ",3,,,,trocken" (intensity=3, then empty duration,
        // empty activities, empty remedies, then notes)
        assertTrue(
            "husten row must have empty duration field, got: $hu",
            hu.contains(",3,,,,trocken")
        )
    }

    @Test
    fun `symptom row lists all selected types and catalog details`() {
        val symptoms = listOf(
            CsvExporter.SymptomRow(
                timestampMillis = 1700000000000L,
                symptomType = "Sodbrennen",
                symptomTypes = listOf("Sodbrennen", "Aufstoßen"),
                intensity = 5,
                durationMinutes = 15,
                notes = "",
                activities = listOf("Sitzen", "Liegen"),
                remedies = listOf("Wasser")
            )
        )
        val csv = CsvExporter.buildCsv(meals = emptyList(), symptoms = symptoms)
        val row = csv.lines().first { it.contains("Sodbrennen") }
        assertTrue(
            "row must contain all symptom types, got: $row",
            row.contains("Sodbrennen; Aufstoßen")
        )
        assertTrue("row must contain activities, got: $row", row.contains("Sitzen; Liegen"))
        assertTrue("row must contain remedies, got: $row", row.contains("Wasser"))
    }

    @Test
    fun `output ends with a newline so file append is safe`() {
        val csv = CsvExporter.buildCsv(meals = emptyList(), symptoms = emptyList())
        assertEquals("CSV must end with a newline", '\n', csv.last())
    }
}
