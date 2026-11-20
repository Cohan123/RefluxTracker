package com.example.export

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.SymptomEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * End-to-end behaviour spec for [RefluxDataExport].
 *
 * Verifies that:
 *  - All meals currently in the DB end up in the CSV
 *  - All symptoms currently in the DB end up in the CSV
 *  - The output file is written into the configured cache subdirectory
 *
 * The actual sharing intent and UI is verified by manual / instrumented test;
 * here we only assert the data layer contract.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RefluxDataExportTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        val dbFile = context.getDatabasePath("test-reflux.db")
        File(dbFile.parentFile, "test-reflux.db").delete()
        File(dbFile.parentFile, "test-reflux.db-shm").delete()
        File(dbFile.parentFile, "test-reflux.db-wal").delete()
        File(context.filesDir, "dbkey.prefs").delete()
        AppDatabase.closeForTest()
    }

    @After
    fun tearDown() {
        AppDatabase.closeForTest()
        val dbFile = context.getDatabasePath("test-reflux.db")
        File(dbFile.parentFile, "test-reflux.db").delete()
        File(dbFile.parentFile, "test-reflux.db-shm").delete()
        File(dbFile.parentFile, "test-reflux.db-wal").delete()
        // wipe any exported files from this run
        File(context.cacheDir, "exports").deleteRecursively()
    }

    @Test
    fun `export writes a file containing all stored meals and symptoms`() = runBlocking {
        val db = AppDatabase.getInstance(context)
        db.foodDao().insertFood(FoodEntity(name = "Kaffee", category = "Getränke"))
        val foodKaffee = db.foodDao().getFoodByName("Kaffee")!!
        // Use the repository path:
        val repository = com.example.data.repository.RefluxRepository.create(db)
        repository.ensureDefaultCatalogs()
        repository.addMeal(
            timestamp = 1700000000000L,
            mealType = "Frühstück",
            portion = "Klein",
            notes = "schnell getrunken",
            foodIds = listOf(foodKaffee.id)
        )
        val sitzen = db.activityDao().getActivityByName("Sitzen")!!.id
        val wasser = db.remedyDao().getRemedyByName("Wasser")!!.id
        repository.addSymptom(
            timestamp = 1700003600000L, // ~1h später
            symptomType = "Sodbrennen",
            intensity = 6,
            durationMinutes = 30,
            notes = "",
            symptomTypes = listOf("Sodbrennen", "Aufstoßen"),
            activityIds = listOf(sitzen),
            remedyIds = listOf(wasser)
        )

        val outFile = RefluxDataExport.exportToCacheFile(context, repository)

        assertTrue("export file must exist", outFile.exists())
        assertEquals("must be in cache/exports/", "exports", outFile.parentFile?.name)
        val csv = outFile.readText(Charsets.UTF_8)
        assertTrue("CSV must list the meal", csv.contains("Frühstück"))
        assertTrue("CSV must list Kaffee", csv.contains("Kaffee"))
        assertTrue("CSV must list the symptom", csv.contains("Sodbrennen"))
        assertTrue("CSV must list all selected symptom types", csv.contains("Aufstoßen"))
        assertTrue("CSV must list the activity", csv.contains("Sitzen"))
        assertTrue("CSV must list the remedy", csv.contains("Wasser"))
    }
}
