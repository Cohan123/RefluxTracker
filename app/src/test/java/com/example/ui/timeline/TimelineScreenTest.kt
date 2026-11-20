package com.example.ui.timeline

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
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
 * Verifies that the timeline exposes all selected symptom types plus the
 * activity/remedy details, and that tapping a row actually expands it
 * (the expansion state must be Compose snapshot state).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class TimelineScreenTest {

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
    fun `symptom row expands to show all types and catalog details`() {
        runBlocking {
            repository.ensureDefaultCatalogs()
            val sitzen = db.activityDao().getActivityByName("Sitzen")!!.id
            val wasser = db.remedyDao().getRemedyByName("Wasser")!!.id
            repository.addSymptom(
                timestamp = System.currentTimeMillis(),
                symptomType = "Sodbrennen",
                intensity = 5,
                durationMinutes = 30,
                notes = "nach dem Essen",
                symptomTypes = listOf("Sodbrennen", "Aufstoßen"),
                activityIds = listOf(sitzen),
                remedyIds = listOf(wasser)
            )
        }

        composeRule.setContent {
            TimelineScreen(
                viewModel = viewModel,
                onNavigateToAddMeal = {},
                onNavigateToAddSymptom = {},
                onNavigateToEditMeal = {},
                onNavigateToEditSymptom = {}
            )
        }

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Sodbrennen, Aufstoßen")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        // Collapsed by default.
        composeRule.onAllNodesWithText("Intensität: 5/10").assertCountEquals(0)

        composeRule.onNodeWithTag("timeline_row_toggle").performClick()

        composeRule.onNodeWithText("Intensität: 5/10").assertExists()
        composeRule.onNodeWithText("Dauer: 30 Minuten").assertExists()
        composeRule.onNodeWithText("Tätigkeiten: Sitzen").assertExists()
        composeRule.onNodeWithText("Maßnahmen: Wasser").assertExists()
        composeRule.onNodeWithText("Notiz: nach dem Essen").assertExists()

        // The always-visible add actions are present even when the day is not empty.
        composeRule.onNodeWithTag("timeline_add_meal_button").assertExists()
        composeRule.onNodeWithTag("timeline_add_symptom_button").assertExists()
    }
}
