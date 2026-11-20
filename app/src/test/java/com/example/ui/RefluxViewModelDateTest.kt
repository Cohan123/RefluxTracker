package com.example.ui

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.model.MealTypes
import com.example.data.repository.RefluxRepository
import com.example.data.repository.TimelineEntry
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A retroactive meal must switch the day view to the saved date without
 * moving any existing entry.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class RefluxViewModelDateTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: RefluxViewModel
    private var foodId: Long = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<Application>()
            db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            val repository = RefluxRepository.create(db)
            foodId = repository.insertCustomFood("Testessen")
            viewModel = RefluxViewModel(app, repository)
        }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `saving a meal for a past date selects that day immediately`() = runBlocking {
        val pastTimestamp = DateTimeUtils.getStartAndEndOfDay(-3).first + 12 * 60 * 60 * 1000L

        viewModel.addMeal(
            timestamp = pastTimestamp,
            mealType = MealTypes.MITTAGESSEN,
            portion = "Normal",
            notes = "nachgetragen",
            foodIds = listOf(foodId)
        )

        withTimeout(5_000) {
            while (viewModel.selectedDayOffset.value != -3) delay(10)
        }

        val timeline = withTimeout(5_000) {
            viewModel.timelineForSelectedDay.first { entries ->
                entries.any { it is TimelineEntry.Meal }
            }
        }
        val meal = timeline.filterIsInstance<TimelineEntry.Meal>().single()
        assertEquals(pastTimestamp, meal.mealWithFoods.meal.timestamp)
        assertEquals(listOf("Testessen"), meal.mealWithFoods.foods.map { it.name })
    }

    @Test
    fun `editing the date follows the new day and leaves other meals alone`() = runBlocking {
        val repository = RefluxRepository.create(db)
        val yesterday = DateTimeUtils.getStartAndEndOfDay(-1).first + 10 * 60 * 60 * 1000L
        val existingId = repository.addMeal(
            timestamp = yesterday,
            mealType = MealTypes.FRUEHSTUECK,
            portion = "Normal",
            notes = "bleibt",
            foodIds = listOf(foodId)
        )

        viewModel.addMeal(
            timestamp = System.currentTimeMillis(),
            mealType = MealTypes.SNACK,
            portion = "Klein",
            notes = "verschieben",
            foodIds = listOf(foodId)
        )
        withTimeout(5_000) {
            repository.allMealsWithFoods.first { it.size == 2 }
        }

        viewModel.updateMeal(
            mealId = existingId,
            timestamp = DateTimeUtils.getStartAndEndOfDay(-2).first + 9 * 60 * 60 * 1000L,
            mealType = MealTypes.FRUEHSTUECK,
            portion = "Groß",
            notes = "verschoben",
            foodIds = listOf(foodId)
        )

        withTimeout(5_000) {
            while (viewModel.selectedDayOffset.value != -2) delay(10)
        }

        // The snack from today still exists unchanged and is not moved.
        val allMeals = repository.allMealsWithFoods.first()
        assertEquals(2, allMeals.size)
        val moved = allMeals.single { it.meal.id == existingId }
        assertTrue(DateTimeUtils.localDayOffset(moved.meal.timestamp) == -2)
        assertTrue(allMeals.any { DateTimeUtils.localDayOffset(it.meal.timestamp) == 0 })
    }
}
