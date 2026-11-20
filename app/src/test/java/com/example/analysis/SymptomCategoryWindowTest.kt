package com.example.analysis

import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour spec for the symptom-type-specific analysis windows.
 *
 * The current engine uses a single 30 min - 4 h window for every symptom,
 * which misses delayed reactions (cough 8 h after chocolate) and overnight
 * reactions (sleep problems). PR5 introduces per-symptom-category windows
 * and a per-symptom-type analysis granularity.
 *
 * Each test below asserts a concrete scenario so the windows stay tuned.
 */
class SymptomCategoryWindowTest {

    @Test
    fun `category for known symptom types is mapped correctly`() {
        assertEquals(SymptomCategory.IMMEDIATE, SymptomCategory.forSymptomType(SymptomTypes.SODBRENNEN))
        assertEquals(SymptomCategory.IMMEDIATE, SymptomCategory.forSymptomType(SymptomTypes.SAEURERUECKFLUSS))
        assertEquals(SymptomCategory.IMMEDIATE, SymptomCategory.forSymptomType(SymptomTypes.AUFSTOSSEN))
        assertEquals(SymptomCategory.DELAYED, SymptomCategory.forSymptomType(SymptomTypes.HUSTEN))
        assertEquals(SymptomCategory.DELAYED, SymptomCategory.forSymptomType(SymptomTypes.HALSBRENNEN))
        assertEquals(SymptomCategory.DELAYED, SymptomCategory.forSymptomType(SymptomTypes.RAEUSPERN))
        assertEquals(SymptomCategory.OVERNIGHT, SymptomCategory.forSymptomType(SymptomTypes.SCHLAFPROBLEME))
        assertEquals(SymptomCategory.OVERNIGHT, SymptomCategory.forSymptomType(SymptomTypes.BLAEHUNGEN))
    }

    @Test
    fun `category for unknown symptom type defaults to GENERAL`() {
        assertEquals(SymptomCategory.GENERAL, SymptomCategory.forSymptomType("Unbekanntes Symptom"))
    }

    @Test
    fun `IMMEDIATE window is 15 minutes to 3 hours`() {
        val w = SymptomCategory.IMMEDIATE.window()
        assertEquals(15L, w.startMinutes)
        assertEquals(180L, w.endMinutes)
    }

    @Test
    fun `DELAYED window is 1 hour to 8 hours`() {
        val w = SymptomCategory.DELAYED.window()
        assertEquals(60L, w.startMinutes)
        assertEquals(480L, w.endMinutes)
    }

    @Test
    fun `OVERNIGHT window is 1 hour to 14 hours`() {
        val w = SymptomCategory.OVERNIGHT.window()
        assertEquals(60L, w.startMinutes)
        assertEquals(840L, w.endMinutes)
    }

    @Test
    fun `GENERAL window matches the legacy 30 min - 4 h window for backwards compatibility`() {
        val w = SymptomCategory.GENERAL.window()
        assertEquals(30L, w.startMinutes)
        assertEquals(240L, w.endMinutes)
    }

    @Test
    fun `analyze produces one FoodTriggerAnalysis per food-symptom-type pair not per food`() {
        // Setup: 5 coffees, all followed by Sodbrennen within the IMMEDIATE window;
        // 3 of those 5 also show Schlafprobleme in the OVERNIGHT window.
        val coffee = com.example.data.model.FoodEntity(id = 1L, name = "Kaffee", category = "Getränke")
        val meals = mutableListOf<MealWithFoods>()
        val symptoms = mutableListOf<SymptomEntity>()
        val hour = 3600 * 1000L
        val baseTime = 1700000000000L
        for (i in 0 until 5) {
            val mealTime = baseTime + i * 24 * hour
            meals.add(
                MealWithFoods(
                    meal = com.example.data.model.MealEntity(
                        id = (i + 1).toLong(),
                        timestamp = mealTime,
                        mealType = "Frühstück"
                    ),
                    foods = listOf(coffee)
                )
            )
            // Sodbrennen 30 min später — IMMEDIATE window
            symptoms.add(
                SymptomEntity(
                    id = (i + 1).toLong(),
                    timestamp = mealTime + 30 * 60 * 1000L,
                    symptomType = SymptomTypes.SODBRENNEN,
                    intensity = 6
                )
            )
            // Schlafprobleme 6 h später — OVERNIGHT window
            if (i < 3) {
                symptoms.add(
                    SymptomEntity(
                        id = (i + 100).toLong(),
                        timestamp = mealTime + 6 * hour,
                        symptomType = SymptomTypes.SCHLAFPROBLEME,
                        intensity = 4
                    )
                )
            }
        }
        val results = TriggerAnalysisEngine.analyze(meals, symptoms)
        // Two pairs: (Kaffee, Sodbrennen) and (Kaffee, Schlafprobleme)
        assertEquals(
            "Each (food, symptomType) pair should produce its own analysis row",
            2, results.size
        )
        val sodbrennen = results.find { it.symptomType == SymptomTypes.SODBRENNEN }
        val sleep = results.find { it.symptomType == SymptomTypes.SCHLAFPROBLEME }
        assertNotNull("Sodbrennen row must exist", sodbrennen)
        assertNotNull("Schlafprobleme row must exist", sleep)
        assertEquals(5, sodbrennen!!.consumptionCount)
        assertEquals(5, sodbrennen.symptomEventsCount)
        assertEquals(5, sleep!!.consumptionCount)
        assertEquals(3, sleep.symptomEventsCount)
    }

    @Test
    fun `pairs without a symptom inside their window are omitted`() {
        val coffee = com.example.data.model.FoodEntity(id = 1L, name = "Kaffee", category = "Getränke")
        val mealTime = 1700000000000L
        val hour = 3600 * 1000L
        val meals = listOf(
            MealWithFoods(
                meal = com.example.data.model.MealEntity(
                    id = 1L, timestamp = mealTime, mealType = "Frühstück"
                ),
                foods = listOf(coffee)
            )
        )
        val symptoms = listOf(
            // Before the meal: outside the IMMEDIATE window.
            SymptomEntity(
                id = 1L,
                timestamp = mealTime - hour,
                symptomType = SymptomTypes.SODBRENNEN,
                intensity = 6
            ),
            // 2 h after: inside the DELAYED window for cough.
            SymptomEntity(
                id = 2L,
                timestamp = mealTime + 2 * hour,
                symptomType = SymptomTypes.HUSTEN,
                intensity = 5
            )
        )

        val results = TriggerAnalysisEngine.analyze(meals, symptoms)

        assertEquals(1, results.size)
        assertEquals(SymptomTypes.HUSTEN, results.single().symptomType)
        assertEquals(1, results.single().symptomEventsCount)
    }

    @Test
    fun `baseline intensity is specific to the analysed symptom type`() {
        val coffee = com.example.data.model.FoodEntity(id = 1L, name = "Kaffee", category = "Getränke")
        val mealTime = 1700000000000L
        val halfHour = 30 * 60 * 1000L
        val hour = 3600 * 1000L
        val meals = listOf(
            MealWithFoods(
                meal = com.example.data.model.MealEntity(
                    id = 1L, timestamp = mealTime, mealType = "Frühstück"
                ),
                foods = listOf(coffee)
            )
        )
        val symptoms = listOf(
            SymptomEntity(
                id = 1L,
                timestamp = mealTime + halfHour,
                symptomType = SymptomTypes.SODBRENNEN,
                intensity = 8
            ),
            SymptomEntity(
                id = 2L,
                timestamp = mealTime + 2 * hour,
                symptomType = SymptomTypes.HUSTEN,
                intensity = 3
            )
        )

        val results = TriggerAnalysisEngine.analyze(meals, symptoms)

        val heartburn = results.single { it.symptomType == SymptomTypes.SODBRENNEN }
        val cough = results.single { it.symptomType == SymptomTypes.HUSTEN }
        assertEquals(8.0, heartburn.baselineSymptomIntensity, 0.0001)
        assertEquals(3.0, cough.baselineSymptomIntensity, 0.0001)
        // The rows must not share the global average of (8 + 3) / 2 = 5.5.
        assertNotEquals(5.5, cough.baselineSymptomIntensity, 0.0001)
    }

    @Test
    fun `legacy single-window behaviour is preserved when per-category analysis is disabled`() {
        // Old AnalysisConfig has windowStartMinutes=30, windowEndMinutes=240.
        // For backwards compatibility the engine still accepts this and uses
        // GENERAL for everything. A symptom at 5 hours must NOT be counted.
        val coffee = com.example.data.model.FoodEntity(id = 1L, name = "Kaffee", category = "Getränke")
        val mealTime = 1700000000000L
        val hour = 3600 * 1000L
        val meals = listOf(
            MealWithFoods(
                meal = com.example.data.model.MealEntity(
                    id = 1L, timestamp = mealTime, mealType = "Frühstück"
                ),
                foods = listOf(coffee)
            )
        )
        val symptoms = listOf(
            // Inside legacy 4h window
            SymptomEntity(
                id = 1L,
                timestamp = mealTime + 30 * 60 * 1000L,
                symptomType = SymptomTypes.SODBRENNEN,
                intensity = 7
            ),
            // Outside legacy 4h window — must not be counted for Sodbrennen
            SymptomEntity(
                id = 2L,
                timestamp = mealTime + 5 * hour,
                symptomType = SymptomTypes.SODBRENNEN,
                intensity = 8
            )
        )
        val results = TriggerAnalysisEngine.analyze(
            meals, symptoms,
            config = AnalysisConfig(useSymptomCategoryWindows = false)
        )
        // Single-window mode aggregates all symptoms regardless of type into one row.
        assertEquals(1, results.size)
        assertEquals(1, results[0].symptomEventsCount)
        assertNull(
            "In single-window mode the row has no per-type breakdown",
            results[0].symptomType
        )
    }

    @Test
    fun `confidence factor is more conservative at low observation counts`() {
        // With one observation, the score must not exceed 35 (was 55 before).
        // This prevents single-incident false positives from dominating the dashboard.
        val coffee = com.example.data.model.FoodEntity(id = 1L, name = "Kaffee", category = "Getränke")
        val mealTime = 1700000000000L
        val meals = listOf(
            MealWithFoods(
                meal = com.example.data.model.MealEntity(
                    id = 1L, timestamp = mealTime, mealType = "Frühstück"
                ),
                foods = listOf(coffee)
            )
        )
        val symptoms = listOf(
            SymptomEntity(
                id = 1L,
                timestamp = mealTime + 60 * 60 * 1000L,
                symptomType = SymptomTypes.SODBRENNEN,
                intensity = 10
            )
        )
        val results = TriggerAnalysisEngine.analyze(meals, symptoms)
        assertTrue(
            "Score with 1 observation must be conservatively capped — got ${results[0].score}",
            results[0].score <= 35
        )
        // And must be flagged as insufficient data.
        assertEquals(DataQuality.INSUFFICIENT, results[0].dataQuality)
        assertEquals(CorrelationAssessment.INSUFFICIENT_DATA, results[0].assessment)
    }

    @Test
    fun `cough 6 hours after chocolate is now detected via DELAYED window`() {
        // Without per-category windows this would have been missed.
        val chocolate = com.example.data.model.FoodEntity(id = 1L, name = "Schokolade", category = "Süßes")
        val mealTime = 1700000000000L
        val hour = 3600 * 1000L
        val meals = listOf(
            MealWithFoods(
                meal = com.example.data.model.MealEntity(
                    id = 1L, timestamp = mealTime, mealType = "Snack"
                ),
                foods = listOf(chocolate)
            )
        )
        val symptoms = listOf(
            SymptomEntity(
                id = 1L,
                timestamp = mealTime + 6 * hour,
                symptomType = SymptomTypes.HUSTEN,
                intensity = 5
            )
        )
        val results = TriggerAnalysisEngine.analyze(meals, symptoms)
        val cough = results.find { it.symptomType == SymptomTypes.HUSTEN }
        assertNotNull("Cough after 6 h must be detected via DELAYED window", cough)
        assertEquals(1, cough!!.symptomEventsCount)
    }
}
