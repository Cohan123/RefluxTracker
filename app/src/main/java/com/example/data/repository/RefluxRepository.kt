package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.FoodDao
import com.example.data.local.InitialFoodData
import com.example.data.local.MealDao
import com.example.data.local.SymptomDao
import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealFoodCrossRef
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed class TimelineEntry(open val timestamp: Long) {
    data class Meal(val mealWithFoods: MealWithFoods) : TimelineEntry(mealWithFoods.meal.timestamp)
    data class Symptom(val symptom: SymptomEntity) : TimelineEntry(symptom.timestamp)
}

class RefluxRepository(
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val symptomDao: SymptomDao
) {
    val allFoods: Flow<List<FoodEntity>> = foodDao.getAllFoods()
    val allMealsWithFoods: Flow<List<MealWithFoods>> = mealDao.getAllMealsWithFoods()
    val allSymptoms: Flow<List<SymptomEntity>> = symptomDao.getAllSymptoms()

    fun searchFoods(query: String): Flow<List<FoodEntity>> {
        return if (query.isBlank()) {
            foodDao.getAllFoods()
        } else {
            foodDao.searchFoods(query)
        }
    }

    suspend fun insertCustomFood(name: String, category: String = "Eigene Lebensmittel"): Long {
        return withContext(Dispatchers.IO) {
            val existing = foodDao.getFoodByName(name.trim())
            if (existing != null) {
                existing.id
            } else {
                foodDao.insertFood(
                    FoodEntity(
                        name = name.trim(),
                        category = category,
                        iconName = "restaurant",
                        isCustom = true
                    )
                )
            }
        }
    }

    suspend fun ensureDefaultFoods() {
        withContext(Dispatchers.IO) {
            if (foodDao.getFoodCount() == 0) {
                foodDao.insertAllFoods(InitialFoodData.PREPOPULATED_FOODS)
            }
        }
    }

    suspend fun addMeal(
        timestamp: Long,
        mealType: String,
        portion: String,
        notes: String,
        foodIds: List<Long>
    ): Long {
        return withContext(Dispatchers.IO) {
            val meal = MealEntity(
                timestamp = timestamp,
                mealType = mealType,
                portion = portion,
                notes = notes
            )
            val mealId = mealDao.insertMeal(meal)
            if (foodIds.isNotEmpty()) {
                val crossRefs = foodIds.map { foodId ->
                    MealFoodCrossRef(mealId = mealId, foodId = foodId)
                }
                mealDao.insertMealFoodCrossRefs(crossRefs)
            }
            mealId
        }
    }

    suspend fun deleteMeal(mealId: Long) {
        withContext(Dispatchers.IO) {
            mealDao.deleteCrossRefsForMeal(mealId)
            mealDao.deleteMealById(mealId)
        }
    }

    suspend fun addSymptom(
        timestamp: Long,
        symptomType: String,
        intensity: Int,
        durationMinutes: Int?,
        notes: String
    ): Long {
        return withContext(Dispatchers.IO) {
            val symptom = SymptomEntity(
                timestamp = timestamp,
                symptomType = symptomType,
                intensity = intensity,
                durationMinutes = durationMinutes,
                notes = notes
            )
            symptomDao.insertSymptom(symptom)
        }
    }

    suspend fun deleteSymptom(symptomId: Long) {
        withContext(Dispatchers.IO) {
            symptomDao.deleteSymptomById(symptomId)
        }
    }

    fun getDayTimeline(startEpoch: Long, endEpoch: Long): Flow<List<TimelineEntry>> {
        val mealsFlow = mealDao.getMealsBetween(startEpoch, endEpoch)
        val symptomsFlow = symptomDao.getSymptomsBetween(startEpoch, endEpoch)

        return combine(mealsFlow, symptomsFlow) { meals, symptoms ->
            val entries = mutableListOf<TimelineEntry>()
            entries.addAll(meals.map { TimelineEntry.Meal(it) })
            entries.addAll(symptoms.map { TimelineEntry.Symptom(it) })
            entries.sortedBy { it.timestamp }
        }
    }

    suspend fun getEarliestLogTimestamp(): Long? {
        return withContext(Dispatchers.IO) {
            val mealTs = mealDao.getFirstMealTimestamp()
            val sympTs = symptomDao.getFirstSymptomTimestamp()
            when {
                mealTs == null -> sympTs
                sympTs == null -> mealTs
                else -> minOf(mealTs, sympTs)
            }
        }
    }

    companion object {
        fun create(database: AppDatabase): RefluxRepository {
            return RefluxRepository(
                foodDao = database.foodDao(),
                mealDao = database.mealDao(),
                symptomDao = database.symptomDao()
            )
        }
    }
}
