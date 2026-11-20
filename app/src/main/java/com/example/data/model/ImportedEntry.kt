package com.example.data.model

/**
 * One record read from an imported CSV/HTML backup.
 *
 * The importer deliberately speaks in names instead of database ids: the
 * repository resolves (and, when necessary, creates) the referenced foods,
 * activities and remedies inside a single transaction.
 */
sealed interface ImportedEntry {

    data class Meal(
        val timestamp: Long,
        val mealType: String,
        val portion: String,
        val foods: List<String>,
        val notes: String
    ) : ImportedEntry

    data class Symptom(
        val timestamp: Long,
        val symptomTypes: List<String>,
        val intensity: Int,
        val durationMinutes: Int?,
        val activities: List<String>,
        val remedies: List<String>,
        val notes: String
    ) : ImportedEntry
}
