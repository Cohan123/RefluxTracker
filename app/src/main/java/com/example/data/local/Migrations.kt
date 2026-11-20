package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds multi-select support to symptom entries:
 *
 *  - `symptoms.symptomTypesJson` stores all selected symptom names (the
 *    existing `symptomType` column keeps the primary selection).
 *  - `activities` / `remedies` are user-extensible catalogues.
 *  - the two junction tables connect a symptom with its selected activities
 *    and remedies.
 *
 * Existing rows are backfilled from `symptomType`, so no entry loses data.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `symptoms` ADD COLUMN `symptomTypesJson` TEXT NOT NULL DEFAULT '[]'"
        )

        // Backfill the JSON list from the old single-value column. Raw string:
        // the backslashes are part of the SQL literals (SQLite does not treat
        // backslash as an escape character), exactly what JSON escaping needs.
        db.execSQL(
            """
            UPDATE `symptoms`
            SET `symptomTypesJson` = '["' || REPLACE(REPLACE(`symptomType`, '\', '\\'), '"', '\"') || '"]'
            WHERE `symptomType` <> '' AND `symptomTypesJson` = '[]'
            """.trimIndent()
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `activities` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`isCustom` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_activities_name` " +
                "ON `activities` (`name`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `remedies` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`isCustom` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_remedies_name` " +
                "ON `remedies` (`name`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `symptom_activity_cross_ref` (" +
                "`symptomId` INTEGER NOT NULL, " +
                "`activityId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`symptomId`, `activityId`), " +
                "FOREIGN KEY(`symptomId`) REFERENCES `symptoms`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`activityId`) REFERENCES `activities`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_symptom_activity_cross_ref_activityId` " +
                "ON `symptom_activity_cross_ref` (`activityId`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `symptom_remedy_cross_ref` (" +
                "`symptomId` INTEGER NOT NULL, " +
                "`remedyId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`symptomId`, `remedyId`), " +
                "FOREIGN KEY(`symptomId`) REFERENCES `symptoms`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`remedyId`) REFERENCES `remedies`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_symptom_remedy_cross_ref_remedyId` " +
                "ON `symptom_remedy_cross_ref` (`remedyId`)"
        )
    }
}

/**
 * Schema v3 adds:
 *
 *  - `foods.categories` with the manually assigned analysis categories
 *    (comma-separated stable keys, see `FoodCategories`). Existing foods keep
 *    working with an empty value and can be categorised in the food editor.
 *  - `habit_overrides` for renaming/editing/deleting automatically detected
 *    meal habits. The habits themselves are derived from the meal history, so
 *    this table never contains a stale copy of meals or symptoms.
 *
 * The migration only fills the category column for the exact names of the
 * shipped default catalogue. That is initial metadata, not name-based
 * inference during analysis, and it never overwrites user data.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `foods` ADD COLUMN `categories` TEXT NOT NULL DEFAULT ''"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `habit_overrides` (" +
                "`signature` TEXT NOT NULL, " +
                "`customName` TEXT, " +
                "`customFoods` TEXT, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "PRIMARY KEY(`signature`))"
        )

        for (food in InitialFoodData.PREPOPULATED_FOODS) {
            if (food.categories.isEmpty()) continue
            db.execSQL(
                "UPDATE `foods` SET `categories` = ? " +
                    "WHERE `name` = ? AND `categories` = ''",
                arrayOf(food.categories, food.name)
            )
        }
    }
}
