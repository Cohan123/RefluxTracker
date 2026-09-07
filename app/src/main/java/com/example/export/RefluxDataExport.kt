package com.example.export

import android.content.Context
import com.example.data.repository.RefluxRepository
import kotlinx.coroutines.flow.first

/**
 * High-level entry point for exporting all locally-stored reflux data as a
 * single CSV file in the app's cache directory.
 *
 * Returns the [java.io.File] that was written. The caller (UI layer) is
 * responsible for wrapping it in a `content://` URI via FileProvider and
 * launching the system share sheet.
 *
 * The actual DB read is delegated to the [RefluxRepository] passed in, so
 * this class stays trivially unit-testable (no AppDatabase singleton).
 */
object RefluxDataExport {

    suspend fun exportToCacheFile(
        context: Context,
        repository: RefluxRepository
    ): java.io.File {
        val meals = repository.allMealsWithFoods.first()
        val symptoms = repository.allSymptoms.first()
        val csv = CsvExporter.buildCsv(
            meals = meals.map { m ->
                CsvExporter.MealRow(
                    timestampMillis = m.meal.timestamp,
                    mealType = m.meal.mealType,
                    portion = m.meal.portion,
                    foodNames = m.foods.map { it.name },
                    notes = m.meal.notes
                )
            },
            symptoms = symptoms.map { s ->
                CsvExporter.SymptomRow(
                    timestampMillis = s.timestamp,
                    symptomType = s.symptomType,
                    intensity = s.intensity,
                    durationMinutes = s.durationMinutes,
                    notes = s.notes
                )
            }
        )
        return CsvFileWriter.write(context, csv)
    }
}
