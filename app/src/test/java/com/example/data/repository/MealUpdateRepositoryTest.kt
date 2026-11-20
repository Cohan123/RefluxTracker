package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.FoodEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Data-layer behaviour for editing meals: the food selection must be replaced
 * atomically and `createdAt` must stay untouched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MealUpdateRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: RefluxRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RefluxRepository.create(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `updateMeal replaces foods and preserves createdAt`() = runBlocking {
        val breadId = db.foodDao().insertFood(FoodEntity(name = "Brot", category = "Getreide"))
        val cheeseId = db.foodDao().insertFood(FoodEntity(name = "Käse", category = "Milch"))

        val mealId = repository.addMeal(
            timestamp = 1_000L,
            mealType = "Frühstück",
            portion = "Klein",
            notes = "alt",
            foodIds = listOf(breadId)
        )
        val createdAt = db.mealDao().getMealWithFoodsById(mealId)!!.meal.createdAt

        val updated = repository.updateMeal(
            mealId = mealId,
            timestamp = 42_000L,
            mealType = "Mittagessen",
            portion = "Groß",
            notes = "neu",
            foodIds = listOf(cheeseId)
        )

        assertTrue("The existing meal must be updated", updated)
        val details = db.mealDao().getMealWithFoodsById(mealId)!!
        assertEquals(createdAt, details.meal.createdAt)
        assertEquals(42_000L, details.meal.timestamp)
        assertEquals("Mittagessen", details.meal.mealType)
        assertEquals("Groß", details.meal.portion)
        assertEquals("neu", details.meal.notes)
        assertEquals(listOf("Käse"), details.foods.map { it.name })
        assertEquals(
            "Updating must not create a second meal",
            1,
            db.mealDao().getAllMealsWithFoods().first().size
        )
    }

    @Test
    fun `updateMeal returns false for a missing meal`() = runBlocking {
        val updated = repository.updateMeal(
            mealId = 999L,
            timestamp = 1_000L,
            mealType = "Snack",
            portion = "Normal",
            notes = "",
            foodIds = emptyList()
        )
        assertFalse(updated)
    }

    @Test
    fun `getMealDetails returns the stored food selection`() = runBlocking {
        val appleId = db.foodDao().insertFood(FoodEntity(name = "Apfel", category = "Obst"))
        val mealId = repository.addMeal(
            timestamp = 1_000L,
            mealType = "Snack",
            portion = "Normal",
            notes = "",
            foodIds = listOf(appleId)
        )

        val details = repository.getMealDetails(mealId)

        assertEquals(mealId, details?.meal?.id)
        assertEquals(listOf("Apfel"), details?.foods?.map { it.name })
    }
}
