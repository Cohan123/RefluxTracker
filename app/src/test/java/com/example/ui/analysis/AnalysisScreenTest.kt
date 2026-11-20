package com.example.ui.analysis

import android.app.Application
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.model.MealTypes
import com.example.data.model.SymptomTypes
import com.example.data.repository.RefluxRepository
import com.example.ui.RefluxViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Verifies that the fixed-window analytics service is actually wired into the
 * analysis screen, that its metrics are rendered, and that the 7/14/30/365
 * window selector recomputes the summary.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class AnalysisScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: RefluxRepository
    private lateinit var viewModel: RefluxViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RefluxRepository.create(db)
        viewModel = RefluxViewModel(app, repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `fixed window analytics are visible and switchable`() {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L

        runBlocking {
            repository.ensureDefaultFoods()
            repository.ensureDefaultCatalogs()

            val coffeeId = db.foodDao().getFoodByName("Kaffee")!!.id
            val sittingId = db.activityDao().getActivityByName("Sitzen")!!.id
            val lyingId = db.activityDao().getActivityByName("Liegen")!!.id
            val waterId = db.remedyDao().getRemedyByName("Wasser")!!.id

            repository.addMeal(
                timestamp = now - 2 * hour,
                mealType = MealTypes.FRUEHSTUECK,
                portion = "Groß",
                notes = "",
                foodIds = listOf(coffeeId)
            )
            repository.addSymptom(
                timestamp = now - hour,
                symptomType = SymptomTypes.SODBRENNEN,
                intensity = 4,
                durationMinutes = 30,
                notes = "",
                activityIds = listOf(sittingId),
                remedyIds = listOf(waterId)
            )
            // Baseline without a measure; used by the effectiveness comparison.
            repository.addSymptom(
                timestamp = now - 3 * hour,
                symptomType = SymptomTypes.SODBRENNEN,
                intensity = 8,
                durationMinutes = 90,
                notes = "",
                activityIds = listOf(lyingId)
            )
        }

        composeRule.setContent {
            AnalysisScreen(viewModel = viewModel)
        }

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("7 Tage-Auswertung")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("analysis_screen")
            .performScrollToNode(hasText("Täglicher GERD-Burden (0–100)"))
        composeRule.onNodeWithText("Täglicher GERD-Burden (0–100)").assertExists()
        composeRule.onNodeWithTag("burden_trend_chart").assertExists()

        composeRule.onNodeWithTag("analysis_screen")
            .performScrollToNode(hasText("Symptome nach Tageszeit"))
        composeRule.onNodeWithTag("time_of_day_heatmap").assertExists()

        composeRule.onNodeWithTag("analysis_screen")
            .performScrollToNode(hasText("Symptom-Wahrscheinlichkeit mit und ohne Faktor"))
        composeRule.onNodeWithTag("trigger_matrix").assertExists()

        composeRule.onNodeWithTag("analysis_screen")
            .performScrollToNode(hasText("Kaffee"))
        composeRule.onNodeWithText("Top-3-Trigger (7 Tage)").assertExists()
        composeRule.onNodeWithText("Kaffee").assertExists()

        composeRule.onNodeWithTag("analysis_screen")
            .performScrollToNode(hasTestTag("analytics_window_30"))
        composeRule.onNodeWithTag("analytics_window_30").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("30 Tage-Auswertung")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("analysis_screen")
            .performScrollToNode(hasText("Top-3-Trigger (30 Tage)"))
        composeRule.onNodeWithText("Top-3-Trigger (30 Tage)").assertExists()

        composeRule.onNodeWithTag("analysis_screen")
            .performScrollToNode(hasText("Häufigste Tätigkeiten"))
        composeRule.onNodeWithText("Häufigste Tätigkeiten").assertExists()
        composeRule.onNodeWithText("Sitzen").assertExists()

        composeRule.onNodeWithTag("analysis_screen")
            .performScrollToNode(hasText("Wirksamste Maßnahmen"))
        composeRule.onNodeWithText("Wirksamste Maßnahmen").assertExists()
        composeRule.onNodeWithText("Wasser").assertExists()
    }
}
