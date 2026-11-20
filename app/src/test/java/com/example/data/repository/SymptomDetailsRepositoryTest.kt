package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.InitialCatalogData
import com.example.data.model.SymptomTypesJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Data-layer behaviour for the multi-select feature: seeded catalogues,
 * atomic symptom + activity/remedy storage and junction cleanup on delete.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SymptomDetailsRepositoryTest {

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
    fun `default catalogs are seeded once and stay idempotent`() = runBlocking {
        repository.ensureDefaultCatalogs()
        repository.ensureDefaultCatalogs()

        assertEquals(
            InitialCatalogData.ACTIVITIES.size,
            db.activityDao().getActivityCount()
        )
        assertEquals(
            InitialCatalogData.REMEDIES.size,
            db.remedyDao().getRemedyCount()
        )
    }

    @Test
    fun `addSymptom stores all selected types, activities and remedies`() = runBlocking {
        repository.ensureDefaultCatalogs()
        val activityId = db.activityDao().getActivityByName("Sitzen")!!.id
        val remedyId = repository.insertCustomRemedy("Eiswürfel")

        val symptomId = repository.addSymptom(
            timestamp = 1_000L,
            symptomType = "Sodbrennen",
            intensity = 7,
            durationMinutes = 30,
            notes = "nach dem Essen",
            symptomTypes = listOf("Sodbrennen", "Aufstoßen"),
            activityIds = listOf(activityId),
            remedyIds = listOf(remedyId)
        )

        val details = repository.getSymptomDetails(symptomId)
        assertNotNull(details)
        assertEquals("Sodbrennen", details!!.symptom.symptomType)
        assertEquals(
            listOf("Sodbrennen", "Aufstoßen"),
            SymptomTypesJson.decode(details.symptom.symptomTypesJson)
        )
        assertEquals(listOf("Sitzen"), details.activities.map { it.name })
        assertEquals(listOf("Eiswürfel"), details.remedies.map { it.name })
        assertTrue("Custom remedy must be flagged", details.remedies.single().isCustom)
    }

    @Test
    fun `single-type addSymptom keeps the legacy call contract`() = runBlocking {
        val symptomId = repository.addSymptom(
            timestamp = 1_000L,
            symptomType = "Husten",
            intensity = 3,
            durationMinutes = null,
            notes = ""
        )

        val details = repository.getSymptomDetails(symptomId)!!
        assertEquals(listOf("Husten"), SymptomTypesJson.decode(details.symptom.symptomTypesJson))
        assertTrue(details.activities.isEmpty())
        assertTrue(details.remedies.isEmpty())
    }

    @Test
    fun `addSymptom trims the primary symptom type`() = runBlocking {
        val symptomId = repository.addSymptom(
            timestamp = 1_000L,
            symptomType = "  Sodbrennen  ",
            intensity = 5,
            durationMinutes = null,
            notes = ""
        )

        val details = repository.getSymptomDetails(symptomId)!!
        assertEquals("Sodbrennen", details.symptom.symptomType)
        assertEquals(
            listOf("Sodbrennen"),
            SymptomTypesJson.decode(details.symptom.symptomTypesJson)
        )
    }

    @Test
    fun `addSymptom rejects an entry without any symptom type`() = runBlocking {
        val failure = runCatching {
            repository.addSymptom(
                timestamp = 1_000L,
                symptomType = "   ",
                intensity = 3,
                durationMinutes = null,
                notes = ""
            )
        }.exceptionOrNull()

        assertTrue(
            "A symptom without a type must be rejected",
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `day timeline carries activity and remedy details`() = runBlocking {
        repository.ensureDefaultCatalogs()
        val activityId = db.activityDao().getActivityByName("Sitzen")!!.id
        val remedyId = repository.insertCustomRemedy("Eiswürfel")

        repository.addSymptom(
            timestamp = 5_000L,
            symptomType = "Sodbrennen",
            intensity = 4,
            durationMinutes = 20,
            notes = "test",
            symptomTypes = listOf("Sodbrennen", "Aufstoßen"),
            activityIds = listOf(activityId),
            remedyIds = listOf(remedyId)
        )

        val timeline = repository.getDayTimeline(0L, 10_000L).first()
        assertEquals(1, timeline.size)
        val entry = timeline.single() as TimelineEntry.Symptom
        assertEquals(
            listOf("Sodbrennen", "Aufstoßen"),
            SymptomTypesJson.decodeOrPrimary(
                entry.symptom.symptomTypesJson,
                entry.symptom.symptomType
            )
        )
        assertEquals(listOf("Sitzen"), entry.activities.map { it.name })
        assertEquals(listOf("Eiswürfel"), entry.remedies.map { it.name })
    }

    @Test
    fun `updateSymptom replaces details and preserves createdAt`() = runBlocking {
        repository.ensureDefaultCatalogs()
        val sitzen = db.activityDao().getActivityByName("Sitzen")!!.id
        val liegen = db.activityDao().getActivityByName("Liegen")!!.id
        val wasser = db.remedyDao().getRemedyByName("Wasser")!!.id

        val symptomId = repository.addSymptom(
            timestamp = 5_000L,
            symptomType = "Sodbrennen",
            intensity = 4,
            durationMinutes = 20,
            notes = "alt",
            symptomTypes = listOf("Sodbrennen", "Aufstoßen"),
            activityIds = listOf(sitzen),
            remedyIds = listOf(wasser)
        )
        val createdAt = repository.getSymptomDetails(symptomId)!!.symptom.createdAt

        val updated = repository.updateSymptom(
            symptomId = symptomId,
            timestamp = 42_000L,
            symptomType = "Husten",
            intensity = 9,
            durationMinutes = 60,
            notes = "neu",
            symptomTypes = listOf("Husten", "Räuspern"),
            activityIds = listOf(liegen),
            remedyIds = emptyList()
        )

        assertTrue("The existing symptom must be updated", updated)
        val details = repository.getSymptomDetails(symptomId)!!
        assertEquals(createdAt, details.symptom.createdAt)
        assertEquals(42_000L, details.symptom.timestamp)
        assertEquals("Husten", details.symptom.symptomType)
        assertEquals(9, details.symptom.intensity)
        assertEquals(60, details.symptom.durationMinutes)
        assertEquals("neu", details.symptom.notes)
        assertEquals(
            listOf("Husten", "Räuspern"),
            SymptomTypesJson.decode(details.symptom.symptomTypesJson)
        )
        assertEquals(listOf("Liegen"), details.activities.map { it.name })
        assertTrue(details.remedies.isEmpty())
        assertEquals(
            "Updating must not create a second symptom",
            1,
            db.symptomDao().getAllSymptoms().first().size
        )
    }

    @Test
    fun `updateSymptom returns false for a missing symptom`() = runBlocking {
        val updated = repository.updateSymptom(
            symptomId = 999L,
            timestamp = 1_000L,
            symptomType = "Husten",
            intensity = 3,
            durationMinutes = null,
            notes = ""
        )
        assertFalse(updated)
    }

    @Test
    fun `custom catalog entries reuse normalized names instead of duplicating`() = runBlocking {
        val foodId = repository.insertCustomFood("Käsetest")
        val foodIdAgain = repository.insertCustomFood("kaesetest")
        assertEquals(foodId, foodIdAgain)
        assertEquals(
            1,
            db.foodDao().getFoods().count { it.name == "Käsetest" }
        )

        val activityId = repository.insertCustomActivity("Spazieren")
        val activityIdAgain = repository.insertCustomActivity("spazieren")
        assertEquals(activityId, activityIdAgain)
        assertEquals(
            1,
            db.activityDao().getActivities().count {
                it.name.equals("Spazieren", ignoreCase = true)
            }
        )

        val remedyId = repository.insertCustomRemedy("Eiswürfel")
        val remedyIdAgain = repository.insertCustomRemedy("eiswuerfel")
        assertEquals(remedyId, remedyIdAgain)
        assertEquals(
            1,
            db.remedyDao().getRemedies().count {
                it.name.equals("Eiswürfel", ignoreCase = true)
            }
        )
    }

    @Test
    fun `custom food rejects a blank name`() = runBlocking {
        val failure = runCatching {
            repository.insertCustomFood("   ")
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `deleteSymptom removes junction rows`() = runBlocking {
        repository.ensureDefaultCatalogs()
        val activityId = db.activityDao().getActivityByName("Liegen")!!.id
        val symptomId = repository.addSymptom(
            timestamp = 1_000L,
            symptomType = "Sodbrennen",
            intensity = 5,
            durationMinutes = null,
            notes = "",
            activityIds = listOf(activityId)
        )
        assertNotNull(repository.getSymptomDetails(symptomId))

        repository.deleteSymptom(symptomId)

        assertNull(repository.getSymptomDetails(symptomId))
        db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM symptom_activity_cross_ref"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }
}
