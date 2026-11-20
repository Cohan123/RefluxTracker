package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActivityEntity
import com.example.data.model.RemedyEntity
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomTypesJson
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * End-to-end test for [MIGRATION_1_2]: builds a real schema-v1 database with
 * the exact DDL Room generated for version 1, then opens it through
 * [AppDatabase] so Room runs the migration and validates the resulting schema.
 *
 * Robolectric cannot load SQLCipher's native library, so this test exercises
 * the plaintext path (the schema/migration logic is identical).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration1To2Test {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val dbFile: File
        get() = context.getDatabasePath("refluxtrack_database")

    @After
    fun tearDown() {
        AppDatabase.closeForTest()
        deleteDbFiles()
    }

    @Test
    fun `migration 1 to 2 keeps data, backfills symptom types and creates catalog tables`() = runBlocking {
        AppDatabase.closeForTest()
        dbFile.parentFile?.mkdirs()
        deleteDbFiles()

        val trickyType = "Test \"quoted\" \\ backslash"
        createVersion1Database(trickyType)

        // Opening through Room runs MIGRATION_1_2 and validates the schema.
        AppDatabase.getInstance(context).openHelper.writableDatabase

        val db = AppDatabase.getInstance(context)

        // Existing rows survived.
        val symptom = db.symptomDao().getSymptomById(1)
        assertNotNull("Symptom row was lost during migration", symptom)
        assertEquals(trickyType, symptom!!.symptomType)
        assertEquals(
            "symptomTypesJson must contain the escaped legacy type",
            listOf(trickyType),
            SymptomTypesJson.decode(symptom.symptomTypesJson)
        )

        // The whole legacy table set is still readable.
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            raw.rawQuery("SELECT COUNT(*) FROM meals", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Meal row was lost during migration", 1, cursor.getInt(0))
            }
            raw.rawQuery("PRAGMA user_version", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(3, cursor.getInt(0))
            }
        }

        // New tables are usable through the DAOs.
        val activityId = db.activityDao().insertActivity(ActivityEntity(name = "Sitzen"))
        val remedyId = db.remedyDao().insertRemedy(RemedyEntity(name = "Wasser"))
        val newSymptomId = db.symptomDao().insertSymptomWithDetails(
            symptom = SymptomEntity(
                timestamp = 2_000L,
                symptomType = "Sodbrennen",
                intensity = 6,
                symptomTypesJson = SymptomTypesJson.encode(listOf("Sodbrennen", "Aufstoßen"))
            ),
            activityIds = listOf(activityId),
            remedyIds = listOf(remedyId)
        )

        val details = db.symptomDao().getSymptomWithDetails(newSymptomId)
        assertNotNull(details)
        assertEquals(listOf("Sitzen"), details!!.activities.map { it.name })
        assertEquals(listOf("Wasser"), details.remedies.map { it.name })
        assertEquals(
            listOf("Sodbrennen", "Aufstoßen"),
            SymptomTypesJson.decode(details.symptom.symptomTypesJson)
        )

        // Deleting the symptom removes the junction rows.
        db.symptomDao().deleteSymptomWithDetails(newSymptomId)
        assertEquals(null, db.symptomDao().getSymptomWithDetails(newSymptomId))
    }

    private fun createVersion1Database(trickySymptomType: String) {
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { raw ->
            // Exact DDL from app/schemas/.../1.json.
            raw.execSQL(
                "CREATE TABLE IF NOT EXISTS `foods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)"
            )
            raw.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_foods_name` ON `foods` (`name`)"
            )
            raw.execSQL(
                "CREATE TABLE IF NOT EXISTS `meals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `mealType` TEXT NOT NULL, `portion` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            raw.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_meals_timestamp` ON `meals` (`timestamp`)"
            )
            raw.execSQL(
                "CREATE TABLE IF NOT EXISTS `meal_food_cross_ref` (`mealId` INTEGER NOT NULL, `foodId` INTEGER NOT NULL, PRIMARY KEY(`mealId`, `foodId`))"
            )
            raw.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_meal_food_cross_ref_mealId` ON `meal_food_cross_ref` (`mealId`)"
            )
            raw.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_meal_food_cross_ref_foodId` ON `meal_food_cross_ref` (`foodId`)"
            )
            raw.execSQL(
                "CREATE TABLE IF NOT EXISTS `symptoms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `symptomType` TEXT NOT NULL, `intensity` INTEGER NOT NULL, `durationMinutes` INTEGER, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            raw.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_symptoms_timestamp` ON `symptoms` (`timestamp`)"
            )
            raw.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
            )
            raw.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '1014c31afa107587ef7346f0edc32352')"
            )

            val meal = ContentValues().apply {
                put("timestamp", 1_000L)
                put("mealType", "Mittagessen")
                put("portion", "Normal")
                put("notes", "legacy meal")
                put("createdAt", 1_000L)
            }
            raw.insertOrThrow("meals", null, meal)

            val symptom = ContentValues().apply {
                put("timestamp", 1_500L)
                put("symptomType", trickySymptomType)
                put("intensity", 4)
                putNull("durationMinutes")
                put("notes", "legacy symptom")
                put("createdAt", 1_500L)
            }
            raw.insertOrThrow("symptoms", null, symptom)

            raw.version = 1
        }
    }

    private fun deleteDbFiles() {
        listOf("", "-shm", "-wal", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).delete()
        }
    }
}
