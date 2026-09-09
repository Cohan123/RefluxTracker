package com.example.analysis

import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealType
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerAnalysisEngineTest {

    @Test
    fun `test coffee trigger correlation example`() {
        val coffee = FoodEntity(id = 1L, name = "Kaffee", category = "Getränke")
        val bread = FoodEntity(id = 2L, name = "Brot", category = "Milch & Getreide")

        val meals = mutableListOf<MealWithFoods>()
        val symptoms = mutableListOf<SymptomEntity>()

        val baseTime = 1700000000000L
        val hourMs = 3600 * 1000L

        // Coffee consumed 12 times
        // 9 of these times followed by heartburn (Sodbrennen) within 2 hours
        for (i in 0 until 12) {
            val mealTime = baseTime + (i * 24 * hourMs) // 1 coffee per day
            val meal = MealEntity(
                id = (i + 1).toLong(),
                timestamp = mealTime,
                mealType = MealType.FRUEHSTUECK
            )
            meals.add(MealWithFoods(meal = meal, foods = listOf(coffee, bread)))

            if (i < 9) {
                // Heartburn occurs 75 minutes after coffee (inside 30min - 4h window)
                val symptomTime = mealTime + (75 * 60 * 1000L)
                symptoms.add(
                    SymptomEntity(
                        id = (i + 1).toLong(),
                        timestamp = symptomTime,
                        symptomType = SymptomType.SODBRENNEN,
                        intensity = 7
                    )
                )
            }
        }

        val results = TriggerAnalysisEngine.analyze(meals, symptoms)
        val coffeeAnalysis = results.find { it.foodName == "Kaffee" }

        assertTrue("Coffee analysis should be found", coffeeAnalysis != null)
        coffeeAnalysis?.let {
            assertEquals(12, it.consumptionCount)
            assertEquals(9, it.symptomEventsCount)
            assertEquals(DataQuality.GOOD, it.dataQuality)
            assertTrue("Score should be high for 9/12 correlation", it.score >= 60)
            assertEquals(CorrelationAssessment.PROBABLE, it.assessment)
        }
    }

    @Test
    fun `test insufficient data when observations are under 3`() {
        val apple = FoodEntity(id = 3L, name = "Apfel", category = "Obst")
        val mealTime = 1700000000000L
        val meals = listOf(
            MealWithFoods(
                meal = MealEntity(id = 1L, timestamp = mealTime, mealType = MealType.SNACK),
                foods = listOf(apple)
            )
        )
        val symptoms = listOf(
            SymptomEntity(
                id = 1L,
                timestamp = mealTime + (45 * 60 * 1000L),
                symptomType = SymptomType.SODBRENNEN,
                intensity = 5
            )
        )

        val results = TriggerAnalysisEngine.analyze(meals, symptoms)
        val appleAnalysis = results.find { it.foodName == "Apfel" }

        assertTrue(appleAnalysis != null)
        appleAnalysis?.let {
            assertEquals(1, it.consumptionCount)
            assertEquals(DataQuality.INSUFFICIENT, it.dataQuality)
            assertEquals(CorrelationAssessment.INSUFFICIENT_DATA, it.assessment)
        }
    }
}
