package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FoodCategories
import com.example.data.model.HabitOverrideEntity
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end test for [MIGRATION_2_3]: the schema-v2 database is built with the
 * exact DDL Room generated for version 2, then opened through [AppDatabase] so
 * Room runs the migration and validates the result.
 *
 * Robolectric cannot load SQLCipher's native library, so this test uses the
 * plaintext path; the migration SQL is identical.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration2To3Test {

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
    fun `migration 2 to 3 preserves data, adds categories and habit overrides`() = runBlocking {
        AppDatabase.closeForTest()
        dbFile.parentFile?.mkdirs()
        deleteDbFiles()
        createVersion2Database()

        // Opening through Room applies MIGRATION_2_3 and validates the schema.
        AppDatabase.getInstance(context).openHelper.writableDatabase
        val db = AppDatabase.getInstance(context)

        val coffee = db.foodDao().getFoodByName("Kaffee")
        assertNotNull("existing food was lost", coffee)
        assertEquals(
            FoodCategories.CAFFEINE,
            coffee!!.categories
        )

        val custom = db.foodDao().getFoodByName("Mein eigenes Essen")
        assertNotNull(custom)
        assertEquals("unassigned food must stay empty", "", custom!!.categories)

        // Unrelated existing rows survived untouched.
        val meal = db.mealDao().getMealWithFoodsById(1)
        assertNotNull(meal)
        assertEquals("Mittagessen", meal!!.meal.mealType)
        assertEquals(listOf("Kaffee"), meal.foods.map { it.name })

        val symptom = db.symptomDao().getSymptomById(1)
        assertNotNull(symptom)
        assertEquals("Sodbrennen", symptom!!.symptomType)

        // New DAOs/tables work after the migration.
        db.foodDao().updateCategories(custom.id, FoodCategories.encode(listOf(FoodCategories.FAT)))
        assertEquals(
            listOf(FoodCategories.FAT),
            FoodCategories.decode(db.foodDao().getFoodById(custom.id)!!.categories)
        )

        db.habitOverrideDao().upsert(
            HabitOverrideEntity(
                signature = "fruehstueck::brot|kaese",
                customName = "Standardfrühstück"
            )
        )
        assertEquals("Standardfrühstück", db.habitOverrideDao().getOverrides().single().customName)
        db.habitOverrideDao().delete("fruehstueck::brot|kaese")
        assertTrue(db.habitOverrideDao().getOverrides().isEmpty())

        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            raw.rawQuery("PRAGMA user_version", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(3, cursor.getInt(0))
            }
            raw.rawQuery("SELECT COUNT(*) FROM meals", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private fun createVersion2Database() {
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { raw ->
            exec(raw, "foods", "CREATE TABLE IF NOT EXISTS `%s` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)")
            raw.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_foods_name` ON `foods` (`name`)")

            exec(raw, "meals", "CREATE TABLE IF NOT EXISTS `%s` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `mealType` TEXT NOT NULL, `portion` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_meals_timestamp` ON `meals` (`timestamp`)")

            exec(raw, "meal_food_cross_ref", "CREATE TABLE IF NOT EXISTS `%s` (`mealId` INTEGER NOT NULL, `foodId` INTEGER NOT NULL, PRIMARY KEY(`mealId`, `foodId`))")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_food_cross_ref_mealId` ON `meal_food_cross_ref` (`mealId`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_food_cross_ref_foodId` ON `meal_food_cross_ref` (`foodId`)")

            exec(raw, "symptoms", "CREATE TABLE IF NOT EXISTS `%s` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `symptomType` TEXT NOT NULL, `intensity` INTEGER NOT NULL, `durationMinutes` INTEGER, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `symptomTypesJson` TEXT NOT NULL DEFAULT '[]')")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_symptoms_timestamp` ON `symptoms` (`timestamp`)")

            exec(raw, "activities", "CREATE TABLE IF NOT EXISTS `%s` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)")
            raw.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_activities_name` ON `activities` (`name`)")

            exec(raw, "remedies", "CREATE TABLE IF NOT EXISTS `%s` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)")
            raw.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_remedies_name` ON `remedies` (`name`)")

            exec(raw, "symptom_activity_cross_ref", "CREATE TABLE IF NOT EXISTS `%s` (`symptomId` INTEGER NOT NULL, `activityId` INTEGER NOT NULL, PRIMARY KEY(`symptomId`, `activityId`), FOREIGN KEY(`symptomId`) REFERENCES `symptoms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`activityId`) REFERENCES `activities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_symptom_activity_cross_ref_activityId` ON `symptom_activity_cross_ref` (`activityId`)")

            exec(raw, "symptom_remedy_cross_ref", "CREATE TABLE IF NOT EXISTS `%s` (`symptomId` INTEGER NOT NULL, `remedyId` INTEGER NOT NULL, PRIMARY KEY(`symptomId`, `remedyId`), FOREIGN KEY(`symptomId`) REFERENCES `symptoms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`remedyId`) REFERENCES `remedies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_symptom_remedy_cross_ref_remedyId` ON `symptom_remedy_cross_ref` (`remedyId`)")

            raw.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
            )
            raw.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                    "VALUES(42, '55a6e10296ace499d7eb4fb01b970b2f')"
            )

            val coffee = ContentValues().apply {
                put("id", 1)
                put("name", "Kaffee")
                put("category", "Getränke")
                put("iconName", "local_cafe")
                put("isCustom", 0)
            }
            raw.insertOrThrow("foods", null, coffee)

            val customFood = ContentValues().apply {
                put("id", 2)
                put("name", "Mein eigenes Essen")
                put("category", "Eigene Lebensmittel")
                put("iconName", "restaurant")
                put("isCustom", 1)
            }
            raw.insertOrThrow("foods", null, customFood)

            val meal = ContentValues().apply {
                put("id", 1)
                put("timestamp", 1_000L)
                put("mealType", "Mittagessen")
                put("portion", "Normal")
                put("notes", "")
                put("createdAt", 1_000L)
            }
            raw.insertOrThrow("meals", null, meal)

            val crossRef = ContentValues().apply {
                put("mealId", 1)
                put("foodId", 1)
            }
            raw.insertOrThrow("meal_food_cross_ref", null, crossRef)

            val symptom = ContentValues().apply {
                put("id", 1)
                put("timestamp", 1_500L)
                put("symptomType", "Sodbrennen")
                put("intensity", 4)
                putNull("durationMinutes")
                put("notes", "")
                put("createdAt", 1_500L)
                put("symptomTypesJson", "[\"Sodbrennen\"]")
            }
            raw.insertOrThrow("symptoms", null, symptom)

            raw.version = 2
        }
    }

    private fun exec(raw: SQLiteDatabase, table: String, template: String) {
        raw.execSQL(template.format(table))
    }

    private fun deleteDbFiles() {
        listOf("", "-shm", "-wal", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).delete()
        }
    }
}
