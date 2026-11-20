package com.example.analysis

import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomTypes
import com.example.data.model.SymptomTypesJson
import kotlin.math.max
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

/**
 * Per-symptom-category analysis window. Different symptoms react on different
 * timescales — reflux is immediate (15 min – 3 h), cough is delayed (1 h – 8 h),
 * sleep problems are overnight (1 h – 14 h).
 *
 * `GENERAL` is the legacy 30 min – 4 h window kept for backwards compatibility.
 */
enum class SymptomCategory(val label: String) {
    IMMEDIATE("Sofort-Reaktion"),
    DELAYED("Verzögerte Reaktion"),
    OVERNIGHT("Nächtliche Reaktion"),
    GENERAL("Allgemein");

    fun window(): Window = when (this) {
        IMMEDIATE -> Window(startMinutes = 15, endMinutes = 180)
        DELAYED -> Window(startMinutes = 60, endMinutes = 480)
        OVERNIGHT -> Window(startMinutes = 60, endMinutes = 840)
        GENERAL -> Window(startMinutes = 30, endMinutes = 240)
    }

    data class Window(val startMinutes: Long, val endMinutes: Long) {
        val startMillis: Long get() = startMinutes * 60 * 1000L
        val endMillis: Long get() = endMinutes * 60 * 1000L
    }

    companion object {
        fun forSymptomType(symptomType: String): SymptomCategory = when (symptomType) {
            SymptomTypes.SODBRENNEN,
            SymptomTypes.SAEURERUECKFLUSS,
            SymptomTypes.AUFSTOSSEN -> IMMEDIATE

            SymptomTypes.HUSTEN,
            SymptomTypes.HALSBRENNEN,
            SymptomTypes.RAEUSPERN -> DELAYED

            SymptomTypes.SCHLAFPROBLEME,
            SymptomTypes.BLAEHUNGEN -> OVERNIGHT

            else -> GENERAL
        }
    }
}

data class FoodTriggerAnalysis(
    val foodId: Long,
    val foodName: String,
    /**
     * The symptom type this row is specific to, or null in legacy
     * single-window mode where all symptoms of a meal are aggregated.
     */
    val symptomType: String? = null,
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
    /**
     * If true, the engine emits one row per (food, symptomType) pair and
     * applies per-category analysis windows. If false, the engine uses a
     * single 30 min – 4 h window and aggregates all symptoms per food,
     * matching the legacy behaviour.
     */
    val useSymptomCategoryWindows: Boolean = true,
    val windowStartMinutes: Long = 30L,
    val windowEndMinutes: Long = 240L
)

object TriggerAnalysisEngine {

    fun analyze(
        meals: List<MealWithFoods>,
        symptoms: List<SymptomEntity>,
        config: AnalysisConfig = AnalysisConfig()
    ): List<FoodTriggerAnalysis> {
        if (meals.isEmpty()) return emptyList()

        val baselineIntensity = if (symptoms.isNotEmpty()) {
            symptoms.map { it.intensity }.average()
        } else {
            0.0
        }

        // Group symptoms by every selected type once, not once per food. The
        // same grouping provides the type-specific baseline used below.
        val symptomsByType: Map<String, List<SymptomEntity>> =
            if (config.useSymptomCategoryWindows) {
                symptoms
                    .flatMap { symptom ->
                        SymptomTypesJson
                            .decodeOrPrimary(symptom.symptomTypesJson, symptom.symptomType)
                            .distinct()
                            .map { type -> type to symptom }
                    }
                    .groupBy({ it.first }, { it.second })
            } else {
                emptyMap()
            }

        val baselineByType: Map<String, Double> = symptomsByType.mapValues { (_, list) ->
            list.map { it.intensity }.average()
        }

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

            if (config.useSymptomCategoryWindows) {
                // Group symptoms by every selected type so that secondary
                // selections (multi-select) participate in the analysis too.
                for ((symptomType, symptomsOfType) in symptomsByType) {
                    val category = SymptomCategory.forSymptomType(symptomType)
                    val window = category.window()
                    val row = analysePair(
                        foodId = foodId,
                        foodName = foodName,
                        symptomType = symptomType,
                        consumptionCount = consumptionCount,
                        mealList = mealList,
                        symptomsOfType = symptomsOfType,
                        window = window,
                        baselineIntensity = baselineByType[symptomType] ?: baselineIntensity
                    )
                    results.add(row)
                }
            } else {
                // Legacy mode: aggregate all symptoms per food, single window.
                val window = SymptomCategory.Window(
                    startMinutes = config.windowStartMinutes,
                    endMinutes = config.windowEndMinutes
                )
                val row = analysePair(
                    foodId = foodId,
                    foodName = foodName,
                    symptomType = null,
                    consumptionCount = consumptionCount,
                    mealList = mealList,
                    symptomsOfType = symptoms,
                    window = window,
                    baselineIntensity = baselineIntensity
                )
                results.add(row)
            }
        }

        return results
            // A (food, symptom type) pair without a single symptom in its
            // window is not evidence of anything. With per-type analysis this
            // would otherwise produce a large, misleading cross-product of
            // zero-event rows (one for every food × every logged symptom type).
            .filter { it.symptomEventsCount > 0 }
            .sortedWith(
                compareByDescending<FoodTriggerAnalysis> { it.score }
                    .thenByDescending { it.consumptionCount }
            )
    }

    private fun analysePair(
        foodId: Long,
        foodName: String,
        symptomType: String?,
        consumptionCount: Int,
        mealList: List<MealWithFoods>,
        symptomsOfType: List<SymptomEntity>,
        window: SymptomCategory.Window,
        baselineIntensity: Double
    ): FoodTriggerAnalysis {
        var symptomEventsCount = 0
        val intensitiesAfter = mutableListOf<Int>()
        val delaysMinutes = mutableListOf<Long>()

        for (mealWithFoods in mealList) {
            val mealTime = mealWithFoods.meal.timestamp
            val minTime = mealTime + window.startMillis
            val maxTime = mealTime + window.endMillis

            val matching = symptomsOfType.filter { s ->
                s.timestamp in minTime..maxTime
            }

            if (matching.isNotEmpty()) {
                symptomEventsCount++
                val episodeAvg = matching.map { it.intensity }.average().roundToInt()
                intensitiesAfter.add(episodeAvg)
                val first = matching.minByOrNull { it.timestamp }
                if (first != null) {
                    val delayMs = first.timestamp - mealTime
                    delaysMinutes.add(max(0L, delayMs / (60 * 1000L)))
                }
            }
        }

        val avgIntensityAfter = if (intensitiesAfter.isNotEmpty()) intensitiesAfter.average() else 0.0
        val avgDelay = if (delaysMinutes.isNotEmpty()) delaysMinutes.average() else 0.0
        val dataQuality = DataQuality.fromCount(consumptionCount)

        val symptomProbability = symptomEventsCount.toDouble() / consumptionCount.toDouble()
        val intensityFactor = (avgIntensityAfter / 10.0).coerceIn(0.0, 1.0)
        val elevationOverBaseline = if (baselineIntensity > 0) {
            ((avgIntensityAfter - baselineIntensity) / 10.0).coerceIn(-0.5, 0.5)
        } else {
            0.0
        }

        // Same weights as before, but with a more conservative confidence curve
        // for low observation counts — single-incident triggers must not look
        // like "probable correlations".
        val baseRawScore = (symptomProbability * 55.0) + (intensityFactor * 35.0) + (elevationOverBaseline * 10.0)
        val confidenceFactor = when (consumptionCount) {
            0 -> 0.0
            1 -> 0.25
            2 -> 0.45
            3 -> 0.70
            4 -> 0.82
            5 -> 0.90
            else -> 1.0
        }

        val finalScore = (baseRawScore * confidenceFactor).roundToInt().coerceIn(0, 100)

        val assessment = when {
            dataQuality == DataQuality.INSUFFICIENT -> CorrelationAssessment.INSUFFICIENT_DATA
            finalScore >= 60 && dataQuality >= DataQuality.LOW -> CorrelationAssessment.PROBABLE
            finalScore >= 35 && dataQuality >= DataQuality.LOW -> CorrelationAssessment.POSSIBLE
            else -> CorrelationAssessment.UNREMARKABLE
        }

        return FoodTriggerAnalysis(
            foodId = foodId,
            foodName = foodName,
            symptomType = symptomType,
            consumptionCount = consumptionCount,
            symptomEventsCount = symptomEventsCount,
            averageSymptomIntensityAfter = avgIntensityAfter,
            baselineSymptomIntensity = baselineIntensity,
            averageDelayMinutes = avgDelay,
            score = finalScore,
            dataQuality = dataQuality,
            assessment = assessment
        )
    }
}
