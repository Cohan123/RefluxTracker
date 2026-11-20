package com.example.ui.meal

import android.app.Application
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.model.FoodEntity
import com.example.data.repository.RefluxRepository
import com.example.ui.RefluxViewModel
import kotlinx.coroutines.flow.first
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
 * Verifies that editing a meal loads its stored values and that saving
 * replaces the meal type, portion and food selection without creating a
 * second row or touching `createdAt`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class AddMealScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: RefluxRepository
    private lateinit var viewModel: RefluxViewModel

    private var breadId: Long = 0

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RefluxRepository.create(db)
        // Insert before constructing the ViewModel so its default-food seeding
        // sees a non-empty catalogue and keeps this test deterministic.
        runBlocking {
            breadId = db.foodDao().insertFood(FoodEntity(name = "Testbrot", category = "Test"))
            db.foodDao().insertFood(FoodEntity(name = "Testkäse", category = "Test"))
        }
        viewModel = RefluxViewModel(app, repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `editing loads existing meal and saves changes`() {
        val mealId = runBlocking {
            repository.addMeal(
                timestamp = 5_000L,
                mealType = "Frühstück",
                portion = "Klein",
                notes = "alt",
                foodIds = listOf(breadId)
            )
        }
        val createdAt = runBlocking {
            db.mealDao().getMealWithFoodsById(mealId)!!.meal.createdAt
        }

        var navigatedBack = false
        composeRule.setContent {
            AddMealScreen(
                viewModel = viewModel,
                onNavigateBack = { navigatedBack = true },
                mealId = mealId
            )
        }

        // The form is hidden behind the loading state until the meal is loaded.
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("meal_notes_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mahlzeit bearbeiten").assertExists()
        composeRule.onNodeWithText("Frühstück").assertIsSelected()
        composeRule.onNodeWithText("Klein").assertIsSelected()

        composeRule.onNodeWithText("Mittagessen").performClick()
        composeRule.onNodeWithText("Groß").performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Testkäse").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Testkäse").performClick()
        composeRule.onAllNodesWithText("Testkäse").assertAll(isSelected())

        composeRule.onNodeWithTag("add_meal_form")
            .performScrollToNode(hasTestTag("save_meal_button"))
        composeRule.onNodeWithTag("save_meal_button").performClick()

        composeRule.waitUntil(10_000) {
            runBlocking {
                db.mealDao().getMealWithFoodsById(mealId)?.meal?.mealType == "Mittagessen"
            }
        }
        composeRule.waitForIdle()
        assertTrue("Saving should navigate back", navigatedBack)

        val details = runBlocking { db.mealDao().getMealWithFoodsById(mealId)!! }
        assertEquals(1, runBlocking { db.mealDao().getAllMealsWithFoods().first().size })
        assertEquals(createdAt, details.meal.createdAt)
        assertEquals(5_000L, details.meal.timestamp)
        assertEquals("Mittagessen", details.meal.mealType)
        assertEquals("Groß", details.meal.portion)
        assertEquals("alt", details.meal.notes)
        assertEquals(
            setOf("Testbrot", "Testkäse"),
            details.foods.map { it.name }.toSet()
        )
    }
}
