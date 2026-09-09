package com.example.data.repository

import com.example.data.local.RefluxDao
import com.example.data.model.FoodEntity
import com.example.data.model.FoodTriggerAnalysis
import com.example.data.model.MealEntity
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import com.example.data.model.TimelineItem
import com.example.data.model.TriggerLikelihood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit

class RefluxRepository(private val dao: RefluxDao) {

    val allFoods: Flow<List<FoodEntity>> = dao.getAllFoods()
    val allMealsWithFoods: Flow<List<MealWithFoods>> = dao.getAllMealsWithFoods()
    val allSymptoms: Flow<List<SymptomEntity>> = dao.getAllSymptoms()

    val timelineItems: Flow<List<TimelineItem>> = combine(allMealsWithFoods, allSymptoms) { meals, symptoms ->
        val list = mutableListOf<TimelineItem>()
        meals.forEach { list.add(TimelineItem.MealItem(it)) }
        symptoms.forEach { list.add(TimelineItem.SymptomItem(it)) }
        list.sortedByDescending { it.timestamp }
    }

    suspend fun insertFood(food: FoodEntity): Long = dao.insertFood(food)
    suspend fun getFoodByName(name: String): FoodEntity? = dao.getFoodByName(name)
    suspend fun deleteFood(food: FoodEntity) = dao.deleteFood(food)

    suspend fun getMealWithFoodsById(id: Long): MealWithFoods? = dao.getMealWithFoodsById(id)
    suspend fun getSymptomById(id: Long): SymptomEntity? = dao.getSymptomById(id)

    suspend fun addMeal(meal: MealEntity, foodIds: List<Long>): Long {
        return dao.insertMealWithFoods(meal, foodIds)
    }

    suspend fun updateMeal(meal: MealEntity, foodIds: List<Long>) {
        dao.updateMealWithFoods(meal, foodIds)
    }

    suspend fun deleteMeal(mealId: Long) = dao.deleteMeal(mealId)

    suspend fun addSymptom(symptom: SymptomEntity): Long = dao.insertSymptom(symptom)
    suspend fun updateSymptom(symptom: SymptomEntity) = dao.updateSymptom(symptom)
    suspend fun deleteSymptom(symptomId: Long) = dao.deleteSymptomById(symptomId)

    suspend fun getAllMealsSync(): List<MealWithFoods> = dao.getAllMealsSync()
    suspend fun getAllSymptomsSync(): List<SymptomEntity> = dao.getAllSymptomsSync()
    suspend fun insertFoods(foods: List<FoodEntity>) = dao.insertFoods(foods)

    /**
     * Berechnet die statistischen Korrelationen zwischen Lebensmitteln und Symptomen.
     * Ein Symptom wird als "zeitlich folgend" betrachtet, wenn es zwischen 15 Minuten
     * und maxWindowHours (Standard 4 Stunden) nach Beginn der Mahlzeit aufgetreten ist.
     */
    suspend fun analyzeTriggers(
        lookbackDays: Int = 30,
        minWindowMinutes: Long = 15,
        maxWindowMinutes: Long = 240
    ): List<FoodTriggerAnalysis> {
        val now = System.currentTimeMillis()
        val since = if (lookbackDays > 0) now - TimeUnit.DAYS.toMillis(lookbackDays.toLong()) else 0L

        val allMeals = dao.getAllMealsSync().filter { it.meal.timestamp >= since }
        val allSymptoms = dao.getAllSymptomsSync().filter { it.timestamp >= since }

        if (allMeals.isEmpty()) return emptyList()

        // Mapping: foodId -> Statistics
        class FoodStats(val food: FoodEntity) {
            var consumedCount = 0
            var triggeredCount = 0
            val delayList = mutableListOf<Long>()
            val intensityList = mutableListOf<Int>()
        }

        val foodStatsMap = mutableMapOf<Long, FoodStats>()

        // Jede Mahlzeit auswerten
        for (mealWithFoods in allMeals) {
            val mealTime = mealWithFoods.meal.timestamp
            val windowStart = mealTime + TimeUnit.MINUTES.toMillis(minWindowMinutes)
            val windowEnd = mealTime + TimeUnit.MINUTES.toMillis(maxWindowMinutes)

            // Symptome im Analysefenster nach dieser Mahlzeit finden
            val matchingSymptoms = allSymptoms.filter { symptom ->
                symptom.timestamp in windowStart..windowEnd
            }

            val hasSymptom = matchingSymptoms.isNotEmpty()

            for (food in mealWithFoods.foods) {
                val stats = foodStatsMap.getOrPut(food.id) { FoodStats(food) }
                stats.consumedCount++
                if (hasSymptom) {
                    stats.triggeredCount++
                    matchingSymptoms.forEach { s ->
                        val delay = (s.timestamp - mealTime) / (60 * 1000) // Minuten
                        stats.delayList.add(delay)
                        stats.intensityList.add(s.intensity)
                    }
                }
            }
        }

        return foodStatsMap.values.map { stats ->
            val percentage = if (stats.consumedCount > 0) {
                ((stats.triggeredCount.toDouble() / stats.consumedCount) * 100).toInt()
            } else 0

            val avgDelay = if (stats.delayList.isNotEmpty()) stats.delayList.average().toLong() else 0L
            val avgIntensity = if (stats.intensityList.isNotEmpty()) stats.intensityList.average() else 0.0

            val likelihood = when {
                stats.consumedCount < 3 -> TriggerLikelihood.UNBEKANNT
                percentage >= 65 -> TriggerLikelihood.HOCH
                percentage >= 35 -> TriggerLikelihood.MITTEL
                else -> TriggerLikelihood.GERING
            }

            FoodTriggerAnalysis(
                foodId = stats.food.id,
                foodName = stats.food.name,
                category = stats.food.category,
                timesConsumed = stats.consumedCount,
                timesTriggered = stats.triggeredCount,
                triggerPercentage = percentage,
                avgIntensity = avgIntensity,
                avgDelayMinutes = avgDelay,
                likelihood = likelihood
            )
        }.sortedWith(
            compareByDescending<FoodTriggerAnalysis> { it.triggerPercentage }
                .thenByDescending { it.timesConsumed }
        )
    }
}
