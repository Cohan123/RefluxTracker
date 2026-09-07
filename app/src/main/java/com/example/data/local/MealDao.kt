package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.MealEntity
import com.example.data.model.MealFoodCrossRef
import com.example.data.model.MealWithFoods
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun getAllMealsWithFoods(): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM meals WHERE timestamp >= :startEpoch AND timestamp < :endEpoch ORDER BY timestamp ASC")
    fun getMealsBetween(startEpoch: Long, endEpoch: Long): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealWithFoodsById(id: Long): MealWithFoods?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealFoodCrossRefs(crossRefs: List<MealFoodCrossRef>)

    @Query("DELETE FROM meal_food_cross_ref WHERE mealId = :mealId")
    suspend fun deleteCrossRefsForMeal(mealId: Long)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Query("SELECT COUNT(*) FROM meals")
    fun getMealCount(): Flow<Int>

    @Query("SELECT MIN(timestamp) FROM meals")
    suspend fun getFirstMealTimestamp(): Long?
}
