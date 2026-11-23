package com.example.ui.habit

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.model.MealTypes
import com.example.data.repository.RefluxRepository
import com.example.ui.RefluxViewModel
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Regression tests for the habits management screen.
 *
 * The detector can produce two habits with the same food set at different
 * times of day. Those must remain two distinct, stable entries instead of
 * crashing the lazy list with duplicate keys. Deleting an entry has to persist
 * a local override and refresh the UI.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class HabitsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: RefluxRepository
    private lateinit var viewModel: RefluxViewModel
    private var breadId: Long = 0
    private var cheeseId: Long = 0

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RefluxRepository.create(db)
        runBlocking {
            breadId = repository.insertCustomFood("Brot")
            cheeseId = repository.insertCustomFood("Käse")
        }
        viewModel = RefluxViewModel(app, repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `same food set at different times stays two usable habits`(): Unit = runBlocking {
        // Breakfast at 06:00 on three days, "breakfast" at 20:00 on three days.
        addHabitMeals(dayOffsets = listOf(-6, -5, -4), hour = 6)
        addHabitMeals(dayOffsets = listOf(-3, -2, -1), hour = 20)

        composeRule.setContent {
            HabitsScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("habit_card").fetchSemanticsNodes().size == 2
        }
        composeRule.onAllNodesWithTag("habit_card").assertCountEquals(2)
        composeRule.onNodeWithTag("habits_screen").assertExists()
    }

    @Test
    fun `deleting a habit persists the override and removes the card`() = runBlocking {
        addHabitMeals(dayOffsets = listOf(-3, -2, -1), hour = 8)

        composeRule.setContent {
            HabitsScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("habit_card").fetchSemanticsNodes().size == 1
        }

        composeRule.onAllNodesWithTag("habit_delete")[0].performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("habit_delete_confirm").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("habit_delete_confirm").performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("habit_card").fetchSemanticsNodes().isEmpty()
        }

        val overrides = repository.getHabitOverrides()
        assertEquals(1, overrides.size)
        assertTrue(overrides.single().isDeleted)
    }

    @Test
    fun `deleting one of two same-food habits keeps the other one`() = runBlocking {
        addHabitMeals(dayOffsets = listOf(-6, -5, -4), hour = 6)
        addHabitMeals(dayOffsets = listOf(-3, -2, -1), hour = 20)

        composeRule.setContent {
            HabitsScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("habit_card").fetchSemanticsNodes().size == 2
        }

        composeRule.onAllNodesWithTag("habit_delete")[0].performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("habit_delete_confirm").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("habit_delete_confirm").performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("habit_card").fetchSemanticsNodes().size == 1
        }
        composeRule.onAllNodesWithTag("habit_card").assertCountEquals(1)

        val overrides = repository.getHabitOverrides()
        assertEquals(1, overrides.size)
        assertTrue(overrides.single().isDeleted)
        // The remaining habit still has a usable, non-blank signature.
        assertTrue(overrides.single().signature.contains("::t"))
    }

    private suspend fun addHabitMeals(dayOffsets: List<Int>, hour: Int) {
        for (offset in dayOffsets) {
            repository.addMeal(
                timestamp = timestamp(offset, hour),
                mealType = MealTypes.FRUEHSTUECK,
                portion = "Normal",
                notes = "",
                foodIds = listOf(breadId, cheeseId)
            )
        }
    }

    private fun timestamp(dayOffset: Int, hour: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
