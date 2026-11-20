package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.MealEntity
import com.example.data.model.MealFoodCrossRef
import com.example.data.model.MealWithFoods
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MealDao {
    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    abstract fun getAllMealsWithFoods(): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    abstract suspend fun getAllMealsWithFoodsOnce(): List<MealWithFoods>

    @Transaction
    @Query("SELECT * FROM meals WHERE timestamp >= :startEpoch AND timestamp < :endEpoch ORDER BY timestamp ASC")
    abstract fun getMealsBetween(startEpoch: Long, endEpoch: Long): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    abstract suspend fun getMealWithFoodsById(id: Long): MealWithFoods?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMeal(meal: MealEntity): Long

    @Update
    abstract suspend fun updateMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMealFoodCrossRefs(crossRefs: List<MealFoodCrossRef>)

    @Query("DELETE FROM meal_food_cross_ref WHERE mealId = :mealId")
    abstract suspend fun deleteCrossRefsForMeal(mealId: Long)

    @Query("DELETE FROM meals WHERE id = :id")
    abstract suspend fun deleteMealById(id: Long)

    @Query("SELECT COUNT(*) FROM meals")
    abstract fun getMealCount(): Flow<Int>

    @Query("SELECT MIN(timestamp) FROM meals")
    abstract suspend fun getFirstMealTimestamp(): Long?

    /** Inserts a meal and its food selection atomically. */
    @Transaction
    open suspend fun insertMealWithFoods(meal: MealEntity, foodIds: List<Long>): Long {
        val mealId = insertMeal(meal)
        replaceFoods(mealId, foodIds)
        return mealId
    }

    /** Updates a meal and replaces its food selection atomically. */
    @Transaction
    open suspend fun updateMealWithFoods(meal: MealEntity, foodIds: List<Long>) {
        updateMeal(meal)
        deleteCrossRefsForMeal(meal.id)
        replaceFoods(meal.id, foodIds)
    }

    /** Deletes a meal and its food selection atomically. */
    @Transaction
    open suspend fun deleteMealWithFoods(mealId: Long) {
        deleteCrossRefsForMeal(mealId)
        deleteMealById(mealId)
    }

    private suspend fun replaceFoods(mealId: Long, foodIds: List<Long>) {
        val distinctFoodIds = foodIds.distinct()
        if (distinctFoodIds.isNotEmpty()) {
            insertMealFoodCrossRefs(
                distinctFoodIds.map { foodId -> MealFoodCrossRef(mealId = mealId, foodId = foodId) }
            )
        }
    }
}
