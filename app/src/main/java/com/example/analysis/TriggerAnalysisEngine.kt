package com.example.analysis

import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class DataQuality(val label: String, val minObservations: Int) {
    INSUFFICIENT("Noch zu wenige Daten", 0),
    LOW("Geringe Datenbasis", 3),
    SUFFICIENT("Ausreichende Datenbasis", 6),
    GOOD("Gute Datenbasis", 11);

    companion object {
        fun fromCount(count: Int): DataQuality = when {
            count < 3 -> INSUFFICIENT
            count in 3..5 -> LOW
            count in 6..10 -> SUFFICIENT
            else -> GOOD
        }
    }
}

enum class CorrelationAssessment(val label: String) {
    PROBABLE("Wahrscheinlicher Zusammenhang"),
    POSSIBLE("Möglicher Zusammenhang"),
    UNREMARKABLE("Keine auffälligen Zusammenhänge"),
    INSUFFICIENT_DATA("Noch zu wenige Daten")
}

data class FoodTriggerAnalysis(
    val foodId: Long,
    val foodName: String,
    val consumptionCount: Int,
    val symptomEventsCount: Int,
    val averageSymptomIntensityAfter: Double,
    val baselineSymptomIntensity: Double,
    val averageDelayMinutes: Double,
    val score: Int, // 0..100
    val dataQuality: DataQuality,
    val assessment: CorrelationAssessment
)

data class AnalysisConfig(
    val windowStartMinutes: Long = 30L, // 30 min after meal
    val windowEndMinutes: Long = 240L    // 4 hours (240 min) after meal
)

object TriggerAnalysisEngine {

    fun analyze(
        meals: List<MealWithFoods>,
        symptoms: List<SymptomEntity>,
        config: AnalysisConfig = AnalysisConfig()
    ): List<FoodTriggerAnalysis> {
        if (meals.isEmpty()) return emptyList()

        // Overall baseline symptom intensity
        val baselineIntensity = if (symptoms.isNotEmpty()) {
            symptoms.map { it.intensity }.average()
        } else {
            0.0
        }

        val windowStartMs = config.windowStartMinutes * 60 * 1000L
        val windowEndMs = config.windowEndMinutes * 60 * 1000L

        // Group meals by distinct food
        val foodMealsMap = mutableMapOf<Long, Pair<String, MutableList<MealWithFoods>>>()

        for (mealWithFoods in meals) {
            for (food in mealWithFoods.foods) {
                val pair = foodMealsMap.getOrPut(food.id) {
                    Pair(food.name, mutableListOf())
                }
                pair.second.add(mealWithFoods)
            }
        }

        val results = mutableListOf<FoodTriggerAnalysis>()

        for ((foodId, pair) in foodMealsMap) {
            val (foodName, mealList) = pair
            val consumptionCount = mealList.size
            if (consumptionCount == 0) continue

            var symptomEventsCount = 0
            val intensitiesAfter = mutableListOf<Int>()
            val delaysMinutes = mutableListOf<Long>()

            for (mealWithFoods in mealList) {
                val mealTime = mealWithFoods.meal.timestamp
                val minTime = mealTime + windowStartMs
                val maxTime = mealTime + windowEndMs

                // Symptoms occurring within the analysis window
                val matchingSymptoms = symptoms.filter { s ->
                    s.timestamp in minTime..maxTime
                }

                if (matchingSymptoms.isNotEmpty()) {
                    symptomEventsCount++
                    // Take average intensity during this episode
                    val episodeAvgIntensity = matchingSymptoms.map { it.intensity }.average().roundToInt()
                    intensitiesAfter.add(episodeAvgIntensity)

                    // Delay from meal to the first symptom
                    val firstSymptom = matchingSymptoms.minByOrNull { it.timestamp }
                    if (firstSymptom != null) {
                        val delayMs = firstSymptom.timestamp - mealTime
                        delaysMinutes.add(max(0L, delayMs / (60 * 1000L)))
                    }
                }
            }

            val avgIntensityAfter = if (intensitiesAfter.isNotEmpty()) {
                intensitiesAfter.average()
            } else {
                0.0
            }

            val avgDelay = if (delaysMinutes.isNotEmpty()) {
                delaysMinutes.average()
            } else {
                0.0
            }

            val dataQuality = DataQuality.fromCount(consumptionCount)

            // Calculate deterministic Trigger Score (0..100)
            val symptomProbability = symptomEventsCount.toDouble() / consumptionCount.toDouble()
            val intensityFactor = (avgIntensityAfter / 10.0).coerceIn(0.0, 1.0)
            val elevationOverBaseline = if (baselineIntensity > 0) {
                ((avgIntensityAfter - baselineIntensity) / 10.0).coerceIn(-0.5, 0.5)
            } else {
                0.0
            }

            // Weighted composite:
            // 55% probability of symptoms
            // 35% symptom intensity
            // 10% elevation over personal baseline
            val baseRawScore = (symptomProbability * 55.0) + (intensityFactor * 35.0) + (elevationOverBaseline * 10.0)

            // Confidence dampening for smaller sample sizes
            val confidenceFactor = when (consumptionCount) {
                0 -> 0.0
                1 -> 0.35
                2 -> 0.55
                3 -> 0.75
                4 -> 0.85
                5 -> 0.92
                else -> 1.0
            }

            val finalScore = (baseRawScore * confidenceFactor).roundToInt().coerceIn(0, 100)

            val assessment = when {
                dataQuality == DataQuality.INSUFFICIENT -> CorrelationAssessment.INSUFFICIENT_DATA
                finalScore >= 60 && dataQuality >= DataQuality.LOW -> CorrelationAssessment.PROBABLE
                finalScore >= 35 && dataQuality >= DataQuality.LOW -> CorrelationAssessment.POSSIBLE
                else -> CorrelationAssessment.UNREMARKABLE
            }

            results.add(
                FoodTriggerAnalysis(
                    foodId = foodId,
                    foodName = foodName,
                    consumptionCount = consumptionCount,
                    symptomEventsCount = symptomEventsCount,
                    averageSymptomIntensityAfter = avgIntensityAfter,
                    baselineSymptomIntensity = baselineIntensity,
                    averageDelayMinutes = avgDelay,
                    score = finalScore,
                    dataQuality = dataQuality,
                    assessment = assessment
                )
            )
        }

        // Sort by score descending, then consumptionCount descending
        return results.sortedWith(
            compareByDescending<FoodTriggerAnalysis> { it.score }
                .thenByDescending { it.consumptionCount }
        )
    }
}
