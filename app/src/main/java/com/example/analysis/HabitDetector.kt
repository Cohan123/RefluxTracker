package com.example.analysis

import com.example.data.model.HabitOverrideEntity
import com.example.data.model.MealWithFoods
import com.example.util.DateTimeUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A recurring meal combination detected from the local meal history.
 *
 * Habits are not stored as a duplicate of the meals. They are recomputed from
 * the entries every time; only user adjustments (name, food selection, deleted
 * marker) are persisted in `habit_overrides`.
 */
data class DetectedHabit(
    /** Stable key derived from meal type and the detected food set. */
    val signature: String,
    val name: String,
    val mealType: String,
    val foodNames: List<String>,
    val occurrenceCount: Int,
    val distinctDayCount: Int,
    /** Circular mean of the meal times, in minutes since local midnight. */
    val typicalMinutesOfDay: Int,
    val lastUsedAt: Long,
    /** True when the user renamed the habit or changed its food selection. */
    val isEdited: Boolean
) {
    val typicalTimeLabel: String
        get() = String.format(
            Locale.GERMAN,
            "%02d:%02d",
            typicalMinutesOfDay / 60,
            typicalMinutesOfDay % 60
        )
}

/**
 * Deterministic, fully local habit detection.
 *
 * Requirements implemented here:
 *  - at least [MIN_OCCURRENCES] sufficiently similar meals,
 *  - repetitions on at least [MIN_DISTINCT_DAYS] different local days,
 *  - tolerant to small deviations: two meals are similar when the Jaccard
 *    similarity of their food-name sets is at least [MIN_SIMILARITY], and
 *    their time of day differs by at most [MAX_TIME_DISTANCE_MINUTES],
 *  - the food list only contains foods that occurred in at least 60 % of the
 *    cluster's meals, so a single deviating item does not become part of the
 *    habit.
 *
 * The algorithm is greedy and processes meals chronologically. That keeps the
 * result reproducible for a given meal history.
 */
object HabitDetector {

    const val MIN_OCCURRENCES = 3
    const val MIN_DISTINCT_DAYS = 3
    const val MIN_SIMILARITY = 0.6
    const val MAX_TIME_DISTANCE_MINUTES = 180

    private const val MINUTES_PER_DAY = 24 * 60

    fun detect(
        meals: List<MealWithFoods>,
        overrides: List<HabitOverrideEntity> = emptyList()
    ): List<DetectedHabit> {
        val overrideBySignature = overrides.associateBy { it.signature }
        val clusters = mutableListOf<Cluster>()

        meals
            .asSequence()
            .filter { it.foods.isNotEmpty() }
            .sortedWith(compareBy({ it.meal.timestamp }, { it.meal.id }))
            .forEach { meal ->
                val foods = meal.foods
                    .map { it.name.trim() }
                    .filter { it.isNotEmpty() }
                if (foods.isEmpty()) return@forEach

                val keys = foods.map(::normalizeKey).toSet()
                val minutes = DateTimeUtils.minutesOfDay(meal.meal.timestamp)
                val day = DateTimeUtils.startOfLocalDay(meal.meal.timestamp)

                val match = clusters
                    .filter { cluster ->
                        cluster.mealType == meal.meal.typeOrFallback() &&
                            jaccard(keys, cluster.keys) >= MIN_SIMILARITY &&
                            circularDistance(minutes, cluster.meanMinutes) <=
                            MAX_TIME_DISTANCE_MINUTES
                    }
                    .maxWithOrNull(
                        compareBy<Cluster> { jaccard(keys, it.keys) }
                            .thenBy { -circularDistance(minutes, it.meanMinutes) }
                            .thenByDescending { it.occurrenceCount }
                    )

                if (match != null) {
                    match.add(timestamp = meal.meal.timestamp, minutes = minutes, day = day, foods = foods)
                } else {
                    clusters += Cluster(
                        mealType = meal.meal.typeOrFallback(),
                        timestamp = meal.meal.timestamp,
                        minutes = minutes,
                        day = day,
                        foods = foods
                    )
                }
            }

        return clusters
            .filter {
                it.occurrenceCount >= MIN_OCCURRENCES &&
                    it.distinctDays.size >= MIN_DISTINCT_DAYS
            }
            .mapNotNull { cluster ->
                val detectedFoods = cluster.representativeFoods()
                if (detectedFoods.isEmpty()) return@mapNotNull null
                val signature = signatureFor(cluster.mealType, detectedFoods)
                val override = overrideBySignature[signature]
                if (override?.isDeleted == true) return@mapNotNull null

                val overrideFoods = override?.customFoods
                    ?.split(',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.takeIf { it.isNotEmpty() }
                val foods = overrideFoods ?: detectedFoods

                DetectedHabit(
                    signature = signature,
                    name = override?.customName?.takeIf { it.isNotBlank() }
                        ?: defaultName(cluster.mealType),
                    mealType = cluster.mealType,
                    foodNames = foods,
                    occurrenceCount = cluster.occurrenceCount,
                    distinctDayCount = cluster.distinctDays.size,
                    typicalMinutesOfDay = circularMean(cluster.minuteSamples),
                    lastUsedAt = cluster.lastUsedAt,
                    isEdited = override != null && !override.isDeleted &&
                        (override.customName != null || override.customFoods != null)
                )
            }
            .sortedWith(
                compareByDescending<DetectedHabit> { it.occurrenceCount }
                    .thenByDescending { it.lastUsedAt }
                    .thenBy { it.name.lowercase(Locale.GERMAN) }
            )
    }

    /**
     * Habits worth offering for a new meal, ordered by frequency.
     *
     * A habit is skipped when all of its foods are already selected. An empty
     * [selectedFoodNames] offers every habit of the meal type.
     */
    fun suggestions(
        habits: List<DetectedHabit>,
        mealType: String,
        selectedFoodNames: Collection<String> = emptyList(),
        limit: Int = 2
    ): List<DetectedHabit> {
        val selectedKeys = selectedFoodNames.map(::normalizeKey).toSet()
        return habits
            .filter { it.mealType == mealType }
            .filter { habit -> habit.foodNames.any { normalizeKey(it) !in selectedKeys } }
            .sortedWith(
                compareByDescending<DetectedHabit> { it.distinctDayCount }
                    .thenByDescending { it.occurrenceCount }
                    .thenByDescending { it.lastUsedAt }
            )
            .take(limit.coerceAtLeast(0))
    }

    fun signatureFor(mealType: String, foodNames: Collection<String>): String {
        val key = foodNames
            .map(::normalizeKey)
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .joinToString("|")
        return "${normalizeKey(mealType)}::$key"
    }

    fun normalizeKey(value: String): String = value
        .trim()
        .lowercase(Locale.GERMAN)
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
        .replace(Regex("\\s+"), " ")

    private fun defaultName(mealType: String): String = when (mealType) {
        "Frühstück" -> "Häufiges Frühstück"
        "Mittagessen" -> "Häufiges Mittagessen"
        "Abendessen" -> "Häufiges Abendessen"
        "Snack" -> "Häufiger Snack"
        "Getränk" -> "Häufiges Getränk"
        else -> "Häufige Mahlzeit"
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 0.0
        val intersection = a.count { it in b }
        val union = a.size + b.size - intersection
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    private fun circularDistance(a: Int, b: Int): Int {
        val raw = abs(a - b) % MINUTES_PER_DAY
        return minOf(raw, MINUTES_PER_DAY - raw)
    }

    private fun circularMean(minutes: List<Int>): Int {
        if (minutes.isEmpty()) return 0
        var x = 0.0
        var y = 0.0
        for (minute in minutes) {
            val angle = 2.0 * Math.PI * minute / MINUTES_PER_DAY
            x += cos(angle)
            y += sin(angle)
        }
        val mean = atan2(y, x)
        val degrees = Math.toDegrees(mean)
        val minutesOfDay = ((degrees / 360.0 * MINUTES_PER_DAY).toInt() % MINUTES_PER_DAY)
        return (minutesOfDay + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    private fun com.example.data.model.MealEntity.typeOrFallback(): String =
        mealType.trim().ifEmpty { "Sonstiges" }

    private class Cluster(
        val mealType: String,
        timestamp: Long,
        minutes: Int,
        day: Long,
        foods: List<String>
    ) {
        val keys: MutableSet<String> = foods.map(::normalizeKey).toMutableSet()
        val distinctDays: MutableSet<Long> = mutableSetOf(day)
        val minuteSamples: MutableList<Int> = mutableListOf(minutes)
        private val foodCounts = mutableMapOf<String, Int>()
        private val foodDisplayNames = mutableMapOf<String, String>()
        var occurrenceCount: Int = 0
        var lastUsedAt: Long = timestamp

        init {
            addFoods(foods)
            occurrenceCount = 1
        }

        val meanMinutes: Int
            get() = circularMean(minuteSamples)

        fun add(timestamp: Long, minutes: Int, day: Long, foods: List<String>) {
            addFoods(foods)
            keys += foods.map(::normalizeKey)
            distinctDays += day
            minuteSamples += minutes
            occurrenceCount++
            lastUsedAt = maxOf(lastUsedAt, timestamp)
        }

        fun representativeFoods(): List<String> {
            val threshold = maxOf(2, kotlin.math.ceil(occurrenceCount * 0.6).toInt())
            val selected = foodCounts
                .filterValues { it >= threshold }
                .toList()
                .sortedWith(
                    compareByDescending<Pair<String, Int>> { it.second }
                        .thenBy { foodDisplayNames[it.first]?.lowercase(Locale.GERMAN) ?: it.first }
                )
                .map { foodDisplayNames[it.first] ?: it.first }
            if (selected.isNotEmpty()) return selected
            // Extreme deviation (e.g. every meal uses a different side dish):
            // fall back to the foods of the newest meal, keeping the habit
            // useful instead of returning an empty list.
            return foodCounts.entries
                .maxByOrNull { it.value }
                ?.let { listOf(foodDisplayNames[it.key] ?: it.key) }
                .orEmpty()
        }

        private fun addFoods(foods: List<String>) {
            for (name in foods) {
                val key = normalizeKey(name)
                if (key.isEmpty()) continue
                foodCounts[key] = (foodCounts[key] ?: 0) + 1
                foodDisplayNames.putIfAbsent(key, name)
            }
        }
    }
}
