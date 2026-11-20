package com.example.ui.symptom

import android.app.Application
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import com.example.data.model.SymptomTypesJson
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
 * Verifies that the multi-select chips are actually wired through to the
 * repository: selected symptom types, activities and remedies must end up in
 * the database.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class AddSymptomScreenTest {

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
    fun `saving stores selected symptom types, activity and remedy`() {
        var navigatedBack = false
        composeRule.setContent {
            AddSymptomScreen(
                viewModel = viewModel,
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Second symptom type in addition to the preselected "Sodbrennen".
        composeRule.onNodeWithText("Aufstoßen").performClick()
        composeRule.onNodeWithText("Aufstoßen").assertIsSelected()

        // Wait for the seed coroutine and scroll to the activity/remedy chips.
        composeRule.waitUntil(10_000) {
            runBlocking { db.activityDao().getActivityCount() > 0 }
        }
        composeRule.onNodeWithTag("add_symptom_form")
            .performScrollToNode(hasText("Sitzen"))
        composeRule.onNodeWithText("Sitzen").performClick()
        composeRule.onNodeWithText("Sitzen").assertIsSelected()

        composeRule.onNodeWithTag("add_symptom_form")
            .performScrollToNode(hasText("Wasser"))
        composeRule.onNodeWithText("Wasser").performClick()
        composeRule.onNodeWithText("Wasser").assertIsSelected()

        composeRule.onNodeWithTag("add_symptom_form")
            .performScrollToNode(hasTestTag("save_symptom_button"))
        composeRule.onNodeWithTag("save_symptom_button").performClick()

        // The repository write happens on Dispatchers.IO; poll the database
        // instead of only waiting for the navigation callback (which is posted
        // to the paused Robolectric main looper).
        composeRule.waitUntil(10_000) {
            runBlocking { db.symptomDao().getAllSymptoms().first().isNotEmpty() }
        }
        composeRule.waitForIdle()
        assertTrue("Saving should navigate back", navigatedBack)

        val symptoms = runBlocking { db.symptomDao().getAllSymptoms().first() }
        assertEquals(1, symptoms.size)
        val details = runBlocking {
            db.symptomDao().getSymptomWithDetails(symptoms.single().id)
        }
        assertTrue(details != null)
        assertEquals("Sodbrennen", details!!.symptom.symptomType)
        assertEquals(
            listOf("Sodbrennen", "Aufstoßen"),
            SymptomTypesJson.decode(details.symptom.symptomTypesJson)
        )
        assertEquals(listOf("Sitzen"), details.activities.map { it.name })
        assertEquals(listOf("Wasser"), details.remedies.map { it.name })
    }

    @Test
    fun `editing loads existing details and saves changes`() {
        val sitzen = runBlocking {
            repository.ensureDefaultCatalogs()
            db.activityDao().getActivityByName("Sitzen")!!.id
        }
        val symptomId = runBlocking {
            repository.addSymptom(
                timestamp = 5_000L,
                symptomType = "Sodbrennen",
                intensity = 4,
                durationMinutes = 20,
                notes = "alt",
                symptomTypes = listOf("Sodbrennen", "Aufstoßen"),
                activityIds = listOf(sitzen)
            )
        }
        val createdAt = runBlocking {
            db.symptomDao().getSymptomById(symptomId)!!.createdAt
        }

        var navigatedBack = false
        composeRule.setContent {
            AddSymptomScreen(
                viewModel = viewModel,
                onNavigateBack = { navigatedBack = true },
                symptomId = symptomId
            )
        }

        // The form is hidden behind the loading state until the repository
        // returns the stored symptom.
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Sitzen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Aufstoßen").assertIsSelected()
        composeRule.onNodeWithText("Sitzen").assertIsSelected()

        composeRule.onNodeWithTag("add_symptom_form")
            .performScrollToNode(hasText("Husten"))
        composeRule.onNodeWithText("Husten").performClick()
        composeRule.onNodeWithText("Husten").assertIsSelected()

        composeRule.onNodeWithTag("add_symptom_form")
            .performScrollToNode(hasTestTag("save_symptom_button"))
        composeRule.onNodeWithTag("save_symptom_button").performClick()

        composeRule.waitUntil(10_000) {
            runBlocking {
                val stored = db.symptomDao().getSymptomById(symptomId)
                stored != null && SymptomTypesJson
                    .decode(stored.symptomTypesJson)
                    .contains("Husten")
            }
        }
        composeRule.waitForIdle()
        assertTrue("Saving should navigate back", navigatedBack)

        val details = runBlocking { db.symptomDao().getSymptomWithDetails(symptomId) }
        assertEquals(1, runBlocking { db.symptomDao().getAllSymptoms().first().size })
        assertEquals(createdAt, details!!.symptom.createdAt)
        assertEquals(5_000L, details.symptom.timestamp)
        assertEquals("Sodbrennen", details.symptom.symptomType)
        assertEquals(
            listOf("Sodbrennen", "Aufstoßen", "Husten"),
            SymptomTypesJson.decode(details.symptom.symptomTypesJson)
        )
        assertEquals(listOf("Sitzen"), details.activities.map { it.name })
        assertTrue(details.remedies.isEmpty())
    }
}
