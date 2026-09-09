package com.example.util

import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealType
import com.example.data.model.MealWithFoods
import com.example.data.model.PortionSize
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomType
import com.example.data.repository.RefluxRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvBackupManager {

    private const val HEADER = "TYP;DATUM;UHRZEIT;TIMESTAMP_MS;MAHLZEIT_TYP;PORTIONSGROESSE;LEBENSMITTEL;SYMPTOME;INTENSITAET;DAUER_MIN;TAETIGKEITEN;MASSNAHMEN;NOTIZEN"
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.GERMAN)
    private val dateTimeParser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMAN)

    data class ValidationResult(
        val isValid: Boolean,
        val rowCount: Int = 0,
        val estimatedMeals: Int = 0,
        val estimatedSymptoms: Int = 0,
        val errorMessage: String? = null
    )

    data class ImportResult(
        val mealsImported: Int = 0,
        val symptomsImported: Int = 0,
        val duplicatesSkipped: Int = 0,
        val errors: List<String> = emptyList()
    ) {
        val totalImported: Int get() = mealsImported + symptomsImported
        val hasErrors: Boolean get() = errors.isNotEmpty()
    }

    /**
     * Erzeugt eine vollständige, standardkonforme CSV-Sicherung aller Mahlzeiten und Symptome.
     */
    fun exportToCsv(
        meals: List<MealWithFoods>,
        symptoms: List<SymptomEntity>
    ): String {
        val sb = StringBuilder()
        sb.append(HEADER).append("\r\n")

        // Mahlzeiten exportieren
        meals.sortedBy { it.meal.timestamp }.forEach { mwf ->
            val m = mwf.meal
            val dateStr = dateFormatter.format(Date(m.timestamp))
            val timeStr = timeFormatter.format(Date(m.timestamp))
            val foodNames = mwf.foods.joinToString("|") { it.name }

            val row = listOf(
                "MAHLZEIT",
                dateStr,
                timeStr,
                m.timestamp.toString(),
                m.mealType.name,
                m.portionSize.name,
                foodNames,
                "", // Keine Symptome
                "", // Keine Intensität
                "", // Keine Dauer
                "", // Keine Tätigkeiten
                "", // Keine Maßnahmen
                m.notes
            )
            sb.append(formatRow(row)).append("\r\n")
        }

        // Symptome exportieren
        symptoms.sortedBy { it.timestamp }.forEach { s ->
            val dateStr = dateFormatter.format(Date(s.timestamp))
            val timeStr = timeFormatter.format(Date(s.timestamp))
            val symptomsStr = s.getSymptomList().joinToString("|")
            val activitiesStr = s.getActivityList().joinToString("|")
            val remediesStr = s.getRemedyList().joinToString("|")

            val row = listOf(
                "SYMPTOM",
                dateStr,
                timeStr,
                s.timestamp.toString(),
                "", // Kein Mahlzeitentyp
                "", // Keine Portionsgröße
                "", // Keine Lebensmittel
                symptomsStr,
                s.intensity.toString(),
                s.durationMinutes.toString(),
                activitiesStr,
                remediesStr,
                s.notes
            )
            sb.append(formatRow(row)).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Prüft, ob eine CSV-Datei dem erwarteten Format entspricht.
     */
    fun validateCsv(csvContent: String): ValidationResult {
        if (csvContent.isBlank()) {
            return ValidationResult(isValid = false, errorMessage = "Die Datei ist leer.")
        }

        val rows = parseCsvRows(csvContent)
        if (rows.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "Keine lesbaren Datenzeilen gefunden.")
        }

        val firstRow = rows.first()
        val isHeader = firstRow.any { col ->
            col.contains("TYP", ignoreCase = true) ||
            col.contains("TIMESTAMP", ignoreCase = true) ||
            col.contains("MAHLZEIT", ignoreCase = true)
        }

        if (!isHeader) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Die Datei enthält keine gültige RefluxTrack-Kopfzeile (Header fehlt)."
            )
        }

        val dataRows = rows.drop(1)
        var mealCount = 0
        var symptomCount = 0

        for (row in dataRows) {
            if (row.isEmpty()) continue
            val type = row.getOrNull(0)?.trim()?.uppercase(Locale.ROOT)
            when (type) {
                "MAHLZEIT" -> mealCount++
                "SYMPTOM" -> symptomCount++
            }
        }

        if (mealCount == 0 && symptomCount == 0) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Es wurden keine Mahlzeiten- oder Symptomeinträge in der Datei gefunden."
            )
        }

        return ValidationResult(
            isValid = true,
            rowCount = dataRows.size,
            estimatedMeals = mealCount,
            estimatedSymptoms = symptomCount
        )
    }

    /**
     * Importiert Mahlzeiten und Symptome verlustfrei aus dem CSV-Format in das Repository.
     */
    suspend fun importCsv(
        csvContent: String,
        repository: RefluxRepository,
        skipDuplicates: Boolean = true
    ): ImportResult {
        val rows = parseCsvRows(csvContent)
        if (rows.isEmpty()) {
            return ImportResult(errors = listOf("Keine Datenzeilen gefunden."))
        }

        // Header überspringen, falls vorhanden
        val dataRows = if (rows.first().any { it.contains("TYP", ignoreCase = true) }) {
            rows.drop(1)
        } else {
            rows
        }

        val existingMeals = repository.getAllMealsSync()
        val existingSymptoms = repository.getAllSymptomsSync()

        var importedMeals = 0
        var importedSymptoms = 0
        var skippedDuplicates = 0
        val errors = mutableListOf<String>()

        for ((index, row) in dataRows.withIndex()) {
            val lineNum = index + 2
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            val type = row.getOrNull(0)?.trim()?.uppercase(Locale.ROOT) ?: ""
            if (type != "MAHLZEIT" && type != "SYMPTOM") {
                errors.add("Zeile $lineNum: Unbekannter Typ '$type' (erwartet: MAHLZEIT oder SYMPTOM).")
                continue
            }

            try {
                val datumStr = row.getOrNull(1)?.trim() ?: ""
                val uhrzeitStr = row.getOrNull(2)?.trim() ?: ""
                val timestampMsStr = row.getOrNull(3)?.trim() ?: ""

                // Timestamp ermitteln
                val timestamp = when {
                    timestampMsStr.isNotBlank() && timestampMsStr.toLongOrNull() != null -> {
                        timestampMsStr.toLong()
                    }
                    datumStr.isNotBlank() && uhrzeitStr.isNotBlank() -> {
                        dateTimeParser.parse("$datumStr $uhrzeitStr")?.time ?: System.currentTimeMillis()
                    }
                    datumStr.isNotBlank() -> {
                        dateFormatter.parse(datumStr)?.time ?: System.currentTimeMillis()
                    }
                    else -> System.currentTimeMillis()
                }

                if (type == "MAHLZEIT") {
                    val mealTypeStr = row.getOrNull(4)?.trim() ?: MealType.MITTAGESSEN.name
                    val mealType = try {
                        MealType.valueOf(mealTypeStr.uppercase(Locale.ROOT))
                    } catch (_: Exception) {
                        MealType.values().find { it.displayName.equals(mealTypeStr, ignoreCase = true) }
                            ?: MealType.MITTAGESSEN
                    }

                    val portionStr = row.getOrNull(5)?.trim() ?: PortionSize.MITTEL.name
                    val portion = try {
                        PortionSize.valueOf(portionStr.uppercase(Locale.ROOT))
                    } catch (_: Exception) {
                        PortionSize.values().find { it.displayName.equals(portionStr, ignoreCase = true) }
                            ?: PortionSize.MITTEL
                    }

                    val foodsRaw = row.getOrNull(6)?.trim() ?: ""
                    val notes = row.getOrNull(12)?.trim() ?: ""

                    // Duplikatsprüfung: Gleicher Timestamp (innerhalb 2 Sek.) und gleicher Typ
                    if (skipDuplicates) {
                        val isDuplicate = existingMeals.any {
                            Math.abs(it.meal.timestamp - timestamp) < 2000L && it.meal.mealType == mealType
                        }
                        if (isDuplicate) {
                            skippedDuplicates++
                            continue
                        }
                    }

                    // Lebensmittel ermitteln oder anlegen
                    val foodNames = foodsRaw.split("|", ";", ",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    val foodIds = mutableListOf<Long>()
                    for (fName in foodNames) {
                        val existing = repository.getFoodByName(fName)
                        val id = existing?.id ?: repository.insertFood(
                            FoodEntity(name = fName, category = "Allgemein")
                        )
                        foodIds.add(id)
                    }

                    val meal = MealEntity(
                        timestamp = timestamp,
                        mealType = mealType,
                        portionSize = portion,
                        notes = notes
                    )
                    repository.addMeal(meal, foodIds)
                    importedMeals++

                } else if (type == "SYMPTOM") {
                    val symptomsRaw = row.getOrNull(7)?.trim() ?: ""
                    val intensityStr = row.getOrNull(8)?.trim() ?: "5"
                    val durationStr = row.getOrNull(9)?.trim() ?: "30"
                    val activitiesRaw = row.getOrNull(10)?.trim() ?: ""
                    val remediesRaw = row.getOrNull(11)?.trim() ?: ""
                    val notes = row.getOrNull(12)?.trim() ?: ""

                    val intensity = intensityStr.toIntOrNull()?.coerceIn(1, 10) ?: 5
                    val duration = durationStr.toIntOrNull()?.coerceAtLeast(5) ?: 30

                    // Symptome parsen
                    val symptomList = symptomsRaw.split("|", ";", ",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val symptomsClean = if (symptomList.isNotEmpty()) symptomList else listOf("Sodbrennen")

                    val primarySymptomType = symptomList.firstOrNull()?.let { name ->
                        SymptomType.values().find {
                            it.name.equals(name, ignoreCase = true) ||
                            it.displayName.equals(name, ignoreCase = true)
                        }
                    } ?: SymptomType.SODBRENNEN

                    val activitiesList = activitiesRaw.split("|", ";", ",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    val remediesList = remediesRaw.split("|", ";", ",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (skipDuplicates) {
                        val isDuplicate = existingSymptoms.any {
                            Math.abs(it.timestamp - timestamp) < 2000L
                        }
                        if (isDuplicate) {
                            skippedDuplicates++
                            continue
                        }
                    }

                    val symptom = SymptomEntity(
                        timestamp = timestamp,
                        symptomType = primarySymptomType,
                        intensity = intensity,
                        durationMinutes = duration,
                        notes = notes,
                        symptoms = symptomsClean.joinToString(", "),
                        activities = activitiesList.joinToString(", "),
                        remedies = remediesList.joinToString(", ")
                    )
                    repository.addSymptom(symptom)
                    importedSymptoms++
                }
            } catch (e: Exception) {
                errors.add("Zeile $lineNum: Fehler beim Import (${e.localizedMessage ?: "Ungültiges Format"}).")
            }
        }

        return ImportResult(
            mealsImported = importedMeals,
            symptomsImported = importedSymptoms,
            duplicatesSkipped = skippedDuplicates,
            errors = errors
        )
    }

    private fun formatRow(cols: List<String>): String {
        return cols.joinToString(";") { col ->
            escapeCsvField(col)
        }
    }

    private fun escapeCsvField(value: String): String {
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    /**
     * Standard RFC 4180 CSV-Parser zur sicheren Zerlegung mit Anführungszeichen und Semicolons.
     */
    fun parseCsvRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        val currentRow = mutableListOf<String>()
        var inQuotes = false
        var i = 0

        // Bestimme das Trennzeichen anhand der Kopfzeile: Semicolon bevorzugt
        val delimiter = if (csv.lineSequence().firstOrNull()?.contains(";") == true) ';' else ','

        while (i < csv.length) {
            val c = csv[i]

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                    }
                    delimiter -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < csv.length && csv[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }
}
