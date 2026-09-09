package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealFoodCrossRef
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RefluxDao {

    // Foods
    @Query("SELECT * FROM foods ORDER BY name ASC")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getFoodById(id: Long): FoodEntity?

    @Query("SELECT * FROM foods WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getFoodByName(name: String): FoodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    // Meals
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealById(mealId: Long): MealEntity?

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithFoodsById(mealId: Long): MealWithFoods?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealFoodCrossRef(crossRef: MealFoodCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealFoodCrossRefs(crossRefs: List<MealFoodCrossRef>)

    @Transaction
    suspend fun insertMealWithFoods(meal: MealEntity, foodIds: List<Long>): Long {
        val mealId = insertMeal(meal)
        val refs = foodIds.map { MealFoodCrossRef(mealId = mealId, foodId = it) }
        insertMealFoodCrossRefs(refs)
        return mealId
    }

    @Transaction
    suspend fun updateMealWithFoods(meal: MealEntity, foodIds: List<Long>) {
        updateMeal(meal)
        deleteMealCrossRefs(meal.id)
        val refs = foodIds.map { MealFoodCrossRef(mealId = meal.id, foodId = it) }
        insertMealFoodCrossRefs(refs)
    }

    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun getAllMealsWithFoods(): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM meals WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getMealsSince(since: Long): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM meals WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getMealsBetweenSync(start: Long, end: Long): List<MealWithFoods>

    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp ASC")
    suspend fun getAllMealsSync(): List<MealWithFoods>

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)

    @Query("DELETE FROM meal_food_cross_ref WHERE mealId = :mealId")
    suspend fun deleteMealCrossRefs(mealId: Long)

    @Transaction
    suspend fun deleteMeal(mealId: Long) {
        deleteMealCrossRefs(mealId)
        deleteMealById(mealId)
    }

    // Symptoms
    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    fun getAllSymptoms(): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSymptomsSince(since: Long): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms ORDER BY timestamp ASC")
    suspend fun getAllSymptomsSync(): List<SymptomEntity>

    @Query("SELECT * FROM symptoms WHERE id = :id")
    suspend fun getSymptomById(id: Long): SymptomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomEntity): Long

    @Update
    suspend fun updateSymptom(symptom: SymptomEntity)

    @Query("DELETE FROM symptoms WHERE id = :id")
    suspend fun deleteSymptomById(id: Long)
}
