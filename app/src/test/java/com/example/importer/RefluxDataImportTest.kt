package com.example.importer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.SymptomTypesJson
import com.example.data.repository.RefluxRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end data-layer test for importing a backup: parsing, creating missing
 * catalogue entries, storing multi-select details, and idempotent re-imports.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RefluxDataImportTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: RefluxRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RefluxRepository.create(db)
        runBlocking {
            repository.ensureDefaultFoods()
            repository.ensureDefaultCatalogs()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `imports meals and symptoms and creates missing catalogue entries`() = runBlocking {
        val summary = RefluxDataImport.importFromText(LEGACY_CSV, repository)

        assertEquals(1, summary.mealsImported)
        assertEquals(2, summary.symptomsImported)
        assertEquals(0, summary.duplicatesSkipped)
        assertEquals(0, summary.skippedRows)

        val meals = repository.allMealsWithFoods.first()
        assertEquals(1, meals.size)
        assertEquals(1788841833921L, meals.single().meal.timestamp)
        assertEquals("Frühstück", meals.single().meal.mealType)
        assertEquals("Normal", meals.single().meal.portion)
        assertEquals(
            setOf("Kaffee", "Marmeladenbrot"),
            meals.single().foods.map { it.name }.toSet()
        )

        val marmeladenbrot = db.foodDao().getFoodByName("Marmeladenbrot")
        assertNotNull(marmeladenbrot)
        assertTrue(marmeladenbrot!!.isCustom)
        assertEquals("Import", marmeladenbrot.category)

        val symptoms = repository.allSymptomsWithDetails.first()
            .sortedBy { it.symptom.timestamp }
        assertEquals(2, symptoms.size)

        val first = symptoms[0]
        assertEquals("Sodbrennen", first.symptom.symptomType)
        assertEquals(
            listOf("Sodbrennen", "Druckgefühl"),
            SymptomTypesJson.decodeOrPrimary(
                first.symptom.symptomTypesJson,
                first.symptom.symptomType
            )
        )
        assertEquals(6, first.symptom.intensity)
        assertEquals(30, first.symptom.durationMinutes)
        assertEquals(
            setOf("Liegen", "Autofahren"),
            first.activities.map { it.name }.toSet()
        )
        assertEquals(listOf("Wasser getrunken"), first.remedies.map { it.name })
        assertNotNull(db.activityDao().getActivityByName("Autofahren"))
        assertNotNull(db.remedyDao().getRemedyByName("Wasser getrunken"))

        val second = symptoms[1]
        assertEquals(listOf("Husten"), SymptomTypesJson.decode(second.symptom.symptomTypesJson))
        assertEquals(setOf("Sitzen", "Stress / Hektik"), second.activities.map { it.name }.toSet())
        assertNotNull(db.activityDao().getActivityByName("Stress / Hektik"))
    }

    @Test
    fun `re-importing the same backup skips duplicates`() = runBlocking {
        RefluxDataImport.importFromText(LEGACY_CSV, repository)
        val second = RefluxDataImport.importFromText(LEGACY_CSV, repository)

        assertEquals(0, second.mealsImported)
        assertEquals(0, second.symptomsImported)
        assertEquals(3, second.duplicatesSkipped)
        assertEquals(1, repository.allMealsWithFoods.first().size)
        assertEquals(2, repository.allSymptomsWithDetails.first().size)
    }

    @Test
    fun `recognises a CSV export of an entry stored with seconds`() = runBlocking {
        val coffee = db.foodDao().getFoodByName("Kaffee")!!
        // 06:30:45 on the same day; the exported CSV only contains 06:30.
        repository.addMeal(
            timestamp = 1788841800000L + 45_000L,
            mealType = "Frühstück",
            portion = "Normal",
            notes = "",
            foodIds = listOf(coffee.id)
        )

        val exportCsv = """
            TYP;DATUM;UHRZEIT;TIMESTAMP_MS;MAHLZEIT_TYP;PORTIONSGROESSE;LEBENSMITTEL;SYMPTOME;INTENSITAET;DAUER_MIN;TAETIGKEITEN;MASSNAHMEN;NOTIZEN
            MAHLZEIT;2026-09-08;06:30;1788841800000;FRUEHSTUECK;MITTEL;Kaffee;;;;;;
        """.trimIndent()

        val summary = RefluxDataImport.importFromText(exportCsv, repository)

        assertEquals(0, summary.mealsImported)
        assertEquals(1, summary.duplicatesSkipped)
        assertEquals(1, repository.allMealsWithFoods.first().size)
    }

    private companion object {
        val LEGACY_CSV = """
            TYP;DATUM;UHRZEIT;TIMESTAMP_MS;MAHLZEIT_TYP;PORTIONSGROESSE;LEBENSMITTEL;SYMPTOME;INTENSITAET;DAUER_MIN;TAETIGKEITEN;MASSNAHMEN;NOTIZEN
            MAHLZEIT;2026-09-08;06:30;1788841833921;FRUEHSTUECK;MITTEL;Kaffee|Marmeladenbrot;;;;;;
            SYMPTOM;2026-09-08;07:00;1788843600000;;;;Sodbrennen|Druckgefühl / Druck in der Brust;6;30;Liegen|Autofahren;Wasser getrunken;
            SYMPTOM;2026-09-09;08:00;1788933600000;;;;Reizhusten;3;;Sitzen|Stress / Hektik;;
        """.trimIndent()
    }
}
