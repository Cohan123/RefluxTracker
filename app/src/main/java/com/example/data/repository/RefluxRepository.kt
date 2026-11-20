package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.ActivityDao
import com.example.data.local.AppDatabase
import com.example.data.local.FoodDao
import com.example.data.local.HabitOverrideDao
import com.example.data.local.InitialCatalogData
import com.example.data.local.InitialFoodData
import com.example.data.local.MealDao
import com.example.data.local.RemedyDao
import com.example.data.local.SymptomDao
import com.example.data.model.ActivityEntity
import com.example.data.model.FoodCategories
import com.example.data.model.FoodEntity
import com.example.data.model.HabitOverrideEntity
import com.example.data.model.ImportedEntry
import com.example.data.model.MealEntity
import com.example.data.model.MealWithFoods
import com.example.data.model.RemedyEntity
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomTypesJson
import com.example.data.model.SymptomWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

sealed class TimelineEntry(open val timestamp: Long) {
    data class Meal(val mealWithFoods: MealWithFoods) : TimelineEntry(mealWithFoods.meal.timestamp)
    data class Symptom(
        val symptom: SymptomEntity,
        val activities: List<ActivityEntity> = emptyList(),
        val remedies: List<RemedyEntity> = emptyList()
    ) : TimelineEntry(symptom.timestamp)
}

data class ImportWriteResult(
    val mealsImported: Int,
    val symptomsImported: Int,
    val duplicatesSkipped: Int
)

class RefluxRepository(
    private val database: AppDatabase,
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val symptomDao: SymptomDao,
    private val activityDao: ActivityDao,
    private val remedyDao: RemedyDao,
    private val habitOverrideDao: HabitOverrideDao
) {
    val allFoods: Flow<List<FoodEntity>> = foodDao.getAllFoods()
    val allMealsWithFoods: Flow<List<MealWithFoods>> = mealDao.getAllMealsWithFoods()
    val allSymptoms: Flow<List<SymptomEntity>> = symptomDao.getAllSymptoms()
    val allSymptomsWithDetails: Flow<List<SymptomWithDetails>> =
        symptomDao.getAllSymptomsWithDetails()
    val allActivities: Flow<List<ActivityEntity>> = activityDao.getAllActivities()
    val allRemedies: Flow<List<RemedyEntity>> = remedyDao.getAllRemedies()
    val allHabitOverrides: Flow<List<HabitOverrideEntity>> = habitOverrideDao.getAllOverrides()

    fun searchFoods(query: String): Flow<List<FoodEntity>> {
        return if (query.isBlank()) {
            foodDao.getAllFoods()
        } else {
            foodDao.searchFoods(query)
        }
    }

    suspend fun insertCustomFood(
        name: String,
        category: String = "Eigene Lebensmittel",
        categories: List<String> = emptyList()
    ): Long {
        return withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Food name must not be blank" }
            val key = catalogKey(trimmed)
            val encoded = FoodCategories.encode(categories)
            // Match the same normalized key that import uses so "Kaffee" /
            // "kaffee" and "Käse" / "Kaese" do not create parallel entries.
            val existing = foodDao.getFoods().firstOrNull { catalogKey(it.name) == key }
            if (existing != null) {
                if (encoded.isNotEmpty() && existing.categories.isBlank()) {
                    foodDao.updateCategories(existing.id, encoded)
                }
                existing.id
            } else {
                foodDao.insertFood(
                    FoodEntity(
                        name = trimmed,
                        category = category,
                        iconName = "restaurant",
                        isCustom = true,
                        categories = encoded
                    )
                )
            }
        }
    }

    /** Replaces the analysis categories of a food. Returns `false` if it is gone. */
    suspend fun updateFoodCategories(foodId: Long, categories: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = foodDao.getFoodById(foodId) ?: return@withContext false
            foodDao.updateCategories(existing.id, FoodCategories.encode(categories))
            true
        }
    }

    suspend fun getHabitOverrides(): List<HabitOverrideEntity> = withContext(Dispatchers.IO) {
        habitOverrideDao.getOverrides()
    }

    /**
     * Persists a rename, a custom food selection or a "deleted" marker for an
     * automatically detected habit. The signature comes from the detector.
     */
    suspend fun saveHabitOverride(
        signature: String,
        customName: String?,
        customFoods: List<String>?,
        isDeleted: Boolean
    ) {
        withContext(Dispatchers.IO) {
            require(signature.isNotBlank()) { "Habit signature must not be blank" }
            habitOverrideDao.upsert(
                HabitOverrideEntity(
                    signature = signature,
                    customName = customName?.trim()?.takeIf { it.isNotEmpty() },
                    customFoods = customFoods
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.distinct()
                        ?.joinToString(","),
                    isDeleted = isDeleted
                )
            )
        }
    }

    suspend fun deleteHabitOverride(signature: String) {
        withContext(Dispatchers.IO) { habitOverrideDao.delete(signature) }
    }

    suspend fun ensureDefaultFoods() {
        withContext(Dispatchers.IO) {
            if (foodDao.getFoodCount() == 0) {
                foodDao.insertAllFoods(InitialFoodData.PREPOPULATED_FOODS)
            }
        }
    }

    /**
     * Seeds the activity/remedy catalogues. The inserts use
     * `OnConflictStrategy.IGNORE` on the unique name index, so this is
     * idempotent and safe even if the user adds a custom entry while the
     * seeding coroutine is still running.
     */
    suspend fun ensureDefaultCatalogs() {
        withContext(Dispatchers.IO) {
            activityDao.insertAllActivities(InitialCatalogData.ACTIVITIES)
            remedyDao.insertAllRemedies(InitialCatalogData.REMEDIES)
        }
    }

    suspend fun insertCustomActivity(name: String): Long {
        return withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Activity name must not be blank" }
            val key = catalogKey(trimmed)
            activityDao.getActivities().firstOrNull { catalogKey(it.name) == key }?.id
                ?: activityDao.insertActivity(ActivityEntity(name = trimmed, isCustom = true))
        }
    }

    suspend fun insertCustomRemedy(name: String): Long {
        return withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Remedy name must not be blank" }
            val key = catalogKey(trimmed)
            remedyDao.getRemedies().firstOrNull { catalogKey(it.name) == key }?.id
                ?: remedyDao.insertRemedy(RemedyEntity(name = trimmed, isCustom = true))
        }
    }

    suspend fun getSymptomDetails(symptomId: Long): SymptomWithDetails? {
        return withContext(Dispatchers.IO) {
            symptomDao.getSymptomWithDetails(symptomId)
        }
    }

    suspend fun addMeal(
        timestamp: Long,
        mealType: String,
        portion: String,
        notes: String,
        foodIds: List<Long>
    ): Long {
        return withContext(Dispatchers.IO) {
            mealDao.insertMealWithFoods(
                MealEntity(
                    timestamp = timestamp,
                    mealType = mealType,
                    portion = portion,
                    notes = notes
                ),
                foodIds
            )
        }
    }

    /**
     * Updates an existing meal. Returns `false` when [mealId] no longer exists.
     * `createdAt` is preserved.
     */
    suspend fun updateMeal(
        mealId: Long,
        timestamp: Long,
        mealType: String,
        portion: String,
        notes: String,
        foodIds: List<Long>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = mealDao.getMealWithFoodsById(mealId)?.meal
                ?: return@withContext false
            val updated = existing.copy(
                timestamp = timestamp,
                mealType = mealType,
                portion = portion,
                notes = notes
            )
            mealDao.updateMealWithFoods(updated, foodIds)
            true
        }
    }

    suspend fun getMealDetails(mealId: Long): MealWithFoods? {
        return withContext(Dispatchers.IO) {
            mealDao.getMealWithFoodsById(mealId)
        }
    }

    suspend fun deleteMeal(mealId: Long) {
        withContext(Dispatchers.IO) {
            mealDao.deleteMealWithFoods(mealId)
        }
    }

    /**
     * Stores a symptom together with its multi-select details.
     *
     * [symptomType] is the primary type (used by analysis/CSV for backwards
     * compatibility); [symptomTypes] contains every selected type and is stored
     * as JSON. If [symptomTypes] is empty, only [symptomType] is recorded.
     * [activityIds] and [remedyIds] refer to the catalogues returned by
     * [allActivities] / [allRemedies].
     */
    suspend fun addSymptom(
        timestamp: Long,
        symptomType: String,
        intensity: Int,
        durationMinutes: Int?,
        notes: String,
        symptomTypes: List<String> = emptyList(),
        activityIds: List<Long> = emptyList(),
        remedyIds: List<Long> = emptyList()
    ): Long {
        return withContext(Dispatchers.IO) {
            val normalized = normalizeSymptomTypes(symptomType, symptomTypes)
            val symptom = SymptomEntity(
                timestamp = timestamp,
                symptomType = normalized.primary,
                intensity = intensity,
                durationMinutes = durationMinutes,
                notes = notes,
                symptomTypesJson = SymptomTypesJson.encode(normalized.all)
            )
            symptomDao.insertSymptomWithDetails(symptom, activityIds, remedyIds)
        }
    }

    /**
     * Updates an existing symptom. Returns `false` when [symptomId] no longer
     * exists (e.g. it was deleted from another screen).
     */
    suspend fun updateSymptom(
        symptomId: Long,
        timestamp: Long,
        symptomType: String,
        intensity: Int,
        durationMinutes: Int?,
        notes: String,
        symptomTypes: List<String> = emptyList(),
        activityIds: List<Long> = emptyList(),
        remedyIds: List<Long> = emptyList()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = symptomDao.getSymptomById(symptomId)
                ?: return@withContext false
            val normalized = normalizeSymptomTypes(symptomType, symptomTypes)
            val updated = existing.copy(
                timestamp = timestamp,
                symptomType = normalized.primary,
                intensity = intensity,
                durationMinutes = durationMinutes,
                notes = notes,
                symptomTypesJson = SymptomTypesJson.encode(normalized.all)
            )
            symptomDao.updateSymptomWithDetails(updated, activityIds, remedyIds)
            true
        }
    }

    private fun normalizeSymptomTypes(
        symptomType: String,
        symptomTypes: List<String>
    ): NormalizedSymptomTypes {
        val selected = symptomTypes
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .ifEmpty { listOfNotNull(symptomType.trim().takeIf { it.isNotEmpty() }) }
        require(selected.isNotEmpty()) { "At least one symptom type is required" }
        val primary = symptomType.trim().ifBlank { selected.first() }
        return NormalizedSymptomTypes(
            primary = primary,
            all = (listOf(primary) + selected).distinct()
        )
    }

    private data class NormalizedSymptomTypes(
        val primary: String,
        val all: List<String>
    )

    suspend fun deleteSymptom(symptomId: Long) {
        withContext(Dispatchers.IO) {
            symptomDao.deleteSymptomWithDetails(symptomId)
        }
    }

    /**
     * Imports meals and symptoms from a parsed backup in one transaction.
     *
     * Foods, activities and remedies referenced by the backup are matched by
     * name (case-insensitive) and created as custom catalogue entries when they
     * do not exist yet. Entries that already exist with the same timestamp and
     * content are skipped, so importing the same backup twice does not create
     * duplicates.
     */
    suspend fun importEntries(entries: List<ImportedEntry>): ImportWriteResult {
        if (entries.isEmpty()) return ImportWriteResult(0, 0, 0)

        return withContext(Dispatchers.IO) {
            database.withTransaction {
                val foods = foodDao.getFoods()
                    .associateBy { catalogKey(it.name) }
                    .toMutableMap()
                val activities = activityDao.getActivities()
                    .associateBy { catalogKey(it.name) }
                    .toMutableMap()
                val remedies = remedyDao.getRemedies()
                    .associateBy { catalogKey(it.name) }
                    .toMutableMap()

                val existingMealKeys = mealDao.getAllMealsWithFoodsOnce()
                    .map { mealImportKey(it) }
                    .toMutableSet()
                val existingSymptomKeys = symptomDao.getAllSymptomsWithDetailsOnce()
                    .map { symptomImportKey(it) }
                    .toMutableSet()

                suspend fun resolveFood(rawName: String): Long {
                    val name = rawName.trim()
                    val key = catalogKey(name)
                    foods[key]?.let { return it.id }
                    val id = foodDao.insertFood(
                        FoodEntity(
                            name = name,
                            category = "Import",
                            iconName = "restaurant",
                            isCustom = true
                        )
                    )
                    val entity = if (id == -1L) {
                        foodDao.getFoodByName(name)
                            ?: error("Lebensmittel „$name“ konnte nicht angelegt werden.")
                    } else {
                        FoodEntity(
                            id = id,
                            name = name,
                            category = "Import",
                            iconName = "restaurant",
                            isCustom = true
                        )
                    }
                    foods[key] = entity
                    return entity.id
                }

                suspend fun resolveActivity(rawName: String): Long {
                    val name = rawName.trim()
                    val key = catalogKey(name)
                    activities[key]?.let { return it.id }
                    val id = activityDao.insertActivity(
                        ActivityEntity(name = name, isCustom = true)
                    )
                    val entity = if (id == -1L) {
                        activityDao.getActivityByName(name)
                            ?: error("Tätigkeit „$name“ konnte nicht angelegt werden.")
                    } else {
                        ActivityEntity(id = id, name = name, isCustom = true)
                    }
                    activities[key] = entity
                    return entity.id
                }

                suspend fun resolveRemedy(rawName: String): Long {
                    val name = rawName.trim()
                    val key = catalogKey(name)
                    remedies[key]?.let { return it.id }
                    val id = remedyDao.insertRemedy(
                        RemedyEntity(name = name, isCustom = true)
                    )
                    val entity = if (id == -1L) {
                        remedyDao.getRemedyByName(name)
                            ?: error("Maßnahme „$name“ konnte nicht angelegt werden.")
                    } else {
                        RemedyEntity(id = id, name = name, isCustom = true)
                    }
                    remedies[key] = entity
                    return entity.id
                }

                var mealsImported = 0
                var symptomsImported = 0
                var duplicatesSkipped = 0

                for (entry in entries) {
                    when (entry) {
                        is ImportedEntry.Meal -> {
                            val key = mealImportKey(
                                timestamp = entry.timestamp,
                                mealType = entry.mealType,
                                portion = entry.portion,
                                foods = entry.foods,
                                notes = entry.notes
                            )
                            if (!existingMealKeys.add(key)) {
                                duplicatesSkipped++
                                continue
                            }

                            val foodIds = mutableListOf<Long>()
                            for (food in entry.foods) {
                                val id = resolveFood(food)
                                if (id !in foodIds) foodIds += id
                            }
                            mealDao.insertMealWithFoods(
                                MealEntity(
                                    timestamp = entry.timestamp,
                                    mealType = entry.mealType,
                                    portion = entry.portion,
                                    notes = entry.notes
                                ),
                                foodIds
                            )
                            mealsImported++
                        }

                        is ImportedEntry.Symptom -> {
                            val key = symptomImportKey(
                                timestamp = entry.timestamp,
                                symptomTypes = entry.symptomTypes,
                                intensity = entry.intensity,
                                durationMinutes = entry.durationMinutes,
                                activities = entry.activities,
                                remedies = entry.remedies,
                                notes = entry.notes
                            )
                            if (!existingSymptomKeys.add(key)) {
                                duplicatesSkipped++
                                continue
                            }

                            val activityIds = mutableListOf<Long>()
                            for (activity in entry.activities) {
                                val id = resolveActivity(activity)
                                if (id !in activityIds) activityIds += id
                            }
                            val remedyIds = mutableListOf<Long>()
                            for (remedy in entry.remedies) {
                                val id = resolveRemedy(remedy)
                                if (id !in remedyIds) remedyIds += id
                            }
                            val types = entry.symptomTypes
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .distinct()
                            val primary = types.first()
                            symptomDao.insertSymptomWithDetails(
                                SymptomEntity(
                                    timestamp = entry.timestamp,
                                    symptomType = primary,
                                    intensity = entry.intensity.coerceIn(0, 10),
                                    durationMinutes = entry.durationMinutes,
                                    notes = entry.notes,
                                    symptomTypesJson = SymptomTypesJson.encode(types)
                                ),
                                activityIds,
                                remedyIds
                            )
                            symptomsImported++
                        }
                    }
                }

                ImportWriteResult(
                    mealsImported = mealsImported,
                    symptomsImported = symptomsImported,
                    duplicatesSkipped = duplicatesSkipped
                )
            }
        }
    }

    private fun mealImportKey(meal: MealWithFoods): String = mealImportKey(
        timestamp = meal.meal.timestamp,
        mealType = meal.meal.mealType,
        portion = meal.meal.portion,
        foods = meal.foods.map { it.name },
        notes = meal.meal.notes
    )

    private fun mealImportKey(
        timestamp: Long,
        mealType: String,
        portion: String,
        foods: List<String>,
        notes: String
    ): String = listOf(
        // Compare at minute precision so a CSV export (which only contains
        // HH:mm) also matches entries that were originally stored with seconds.
        (timestamp / 60_000L).toString(),
        catalogKey(mealType),
        catalogKey(portion),
        foods.map(::catalogKey).sorted().joinToString("|"),
        notes.trim()
    ).joinToString(";")

    private fun symptomImportKey(details: SymptomWithDetails): String {
        val symptom = details.symptom
        return symptomImportKey(
            timestamp = symptom.timestamp,
            symptomTypes = SymptomTypesJson.decodeOrPrimary(
                symptom.symptomTypesJson,
                symptom.symptomType
            ),
            intensity = symptom.intensity,
            durationMinutes = symptom.durationMinutes,
            activities = details.activities.map { it.name },
            remedies = details.remedies.map { it.name },
            notes = symptom.notes
        )
    }

    private fun symptomImportKey(
        timestamp: Long,
        symptomTypes: List<String>,
        intensity: Int,
        durationMinutes: Int?,
        activities: List<String>,
        remedies: List<String>,
        notes: String
    ): String = listOf(
        (timestamp / 60_000L).toString(),
        symptomTypes.map(::catalogKey).sorted().joinToString("|"),
        intensity.toString(),
        durationMinutes?.toString() ?: "",
        activities.map(::catalogKey).sorted().joinToString("|"),
        remedies.map(::catalogKey).sorted().joinToString("|"),
        notes.trim()
    ).joinToString(";")

    private fun catalogKey(value: String): String = value.trim()
        .lowercase(Locale.GERMAN)
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")

    fun getDayTimeline(startEpoch: Long, endEpoch: Long): Flow<List<TimelineEntry>> {
        val mealsFlow = mealDao.getMealsBetween(startEpoch, endEpoch)
        val symptomsFlow = symptomDao.getSymptomsBetweenWithDetails(startEpoch, endEpoch)

        return combine(mealsFlow, symptomsFlow) { meals, symptoms ->
            val entries = mutableListOf<TimelineEntry>()
            entries.addAll(meals.map { TimelineEntry.Meal(it) })
            entries.addAll(
                symptoms.map {
                    TimelineEntry.Symptom(it.symptom, it.activities, it.remedies)
                }
            )
            entries.sortedBy { it.timestamp }
        }
    }

    suspend fun getEarliestLogTimestamp(): Long? {
        return withContext(Dispatchers.IO) {
            val mealTs = mealDao.getFirstMealTimestamp()
            val sympTs = symptomDao.getFirstSymptomTimestamp()
            when {
                mealTs == null -> sympTs
                sympTs == null -> mealTs
                else -> minOf(mealTs, sympTs)
            }
        }
    }

    companion object {
        fun create(database: AppDatabase): RefluxRepository {
            return RefluxRepository(
                database = database,
                foodDao = database.foodDao(),
                mealDao = database.mealDao(),
                symptomDao = database.symptomDao(),
                activityDao = database.activityDao(),
                remedyDao = database.remedyDao(),
                habitOverrideDao = database.habitOverrideDao()
            )
        }
    }
}
