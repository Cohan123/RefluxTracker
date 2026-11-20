package com.example.analysis

import com.example.data.model.FoodCategories
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomWithDetails
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Fixed reporting windows exposed by [RefluxAnalyticsService].
 *
 * The day values are the ones used by `NOW - days` in the service.
 */
enum class AnalyticsWindow(val days: Int, val label: String) {
    SEVEN_DAYS(7, "7 Tage"),
    FOURTEEN_DAYS(14, "14 Tage"),
    THIRTY_DAYS(30, "30 Tage"),
    ONE_YEAR(365, "1 Jahr")
}

/** Average symptom intensity for one local calendar day. */
data class DailySeverity(
    val dayStartMillis: Long,
    val symptomCount: Int,
    val averageSeverity: Double
)

/**
 * One local day in the burden chart.
 *
 * [burden] is `null` when the user recorded *nothing* that day. Such a gap is
 * not a symptom-free day. A documented day without symptoms has [hasEntries]
 * true, [symptomCount] 0 and a burden of 0.
 */
data class DailyBurden(
    val dayStartMillis: Long,
    val symptomCount: Int,
    val averageSeverity: Double?,
    val burden: Double?,
    val hasEntries: Boolean,
    val isSpike: Boolean
) {
    val isSymptomFree: Boolean get() = hasEntries && symptomCount == 0
    val isMissing: Boolean get() = !hasEntries
}

/** Aggregated symptom frequency/intensity for one local hour (0..23). */
data class TimeOfDayBucket(
    val hour: Int,
    val symptomCount: Int,
    val averageIntensity: Double?,
    val strongSymptomCount: Int,
    val intensitySum: Int
)

data class TimeOfDaySummary(
    val buckets: List<TimeOfDayBucket>,
    /** Strongest contiguous 5-hour window; end hour is exclusive. */
    val peakStartHour: Int,
    val peakEndHourExclusive: Int,
    val strongSymptomCount: Int,
    val strongSymptomsInPeak: Int,
    val strongShareInPeak: Double,
    /** Data-derived sentence, or null when the sample is too small. */
    val insight: String?
)

/**
 * Daily burden trend. The linear regression uses every documented day
 * (including documented symptom-free days) but never missing days.
 */
data class BurdenTrend(
    val documentedDayCount: Int,
    val slopePerDay: Double?,
    val firstHalfAverageBurden: Double?,
    val secondHalfAverageBurden: Double?,
    val changeAbsolute: Double?,
    val changePercent: Double?,
    val symptomFreeDayCount: Int,
    val missingDayCount: Int,
    val spikeDayStarts: List<Long>,
    /** Number of documented days in each half, for the report's data basis. */
    val firstHalfDocumentedDays: Int = 0,
    val secondHalfDocumentedDays: Int = 0,
    /** Linear-fit value at the first documented day (for the chart line). */
    val fittedStartBurden: Double? = null,
    /** Linear-fit value at the last documented day (for the chart line). */
    val fittedEndBurden: Double? = null,
    /** Calendar days the fitted line spans (for correct chart placement). */
    val fittedStartDayStartMillis: Long? = null,
    val fittedEndDayStartMillis: Long? = null
)

enum class FactorKind { CATEGORY, BEHAVIOUR }

/**
 * Meal-level comparison of symptom probability with and without a factor.
 *
 * Exposure unit is a meal: a meal counts as "followed by a symptom" when at
 * least one symptom lies in the 30-min–4-h window. [relativeChangePercent] is
 * only populated when both groups have at least three meals and the baseline
 * probability is at least 5 %, so tiny groups cannot produce misleading
 * percentages. [absoluteDifference] is always the difference in percentage
 * points (with minus without).
 */
data class FactorAssociation(
    val key: String,
    val label: String,
    val kind: FactorKind,
    val exposedMealCount: Int,
    val unexposedMealCount: Int,
    val exposedSymptomMealCount: Int,
    val unexposedSymptomMealCount: Int,
    val probabilityWith: Double,
    val probabilityWithout: Double,
    val absoluteDifference: Double,
    val relativeChangePercent: Double?,
    val averageIntensityWith: Double?,
    val averageLatencyWithMinutes: Double?,
    val dataQuality: DataQuality,
    val isReliable: Boolean
) {
    val absoluteDifferencePercentagePoints: Double get() = absoluteDifference * 100.0
}


/**
 * Descriptive statistics for one food name or text entry.
 *
 * A symptom is counted once per consumption event, even when several symptoms
 * fall into the same 30-min–4-h window. [triggerProbability] is weighted by
 * portion size; [rawTriggerProbability] is the unweighted ratio.
 */
data class TriggerStatistic(
    val itemName: String,
    val consumptionCount: Int,
    /** Consumption events with at least one symptom in the 30-min–4-h window. */
    val symptomMealCount: Int,
    /** All symptom entries in matching windows; can exceed [consumptionCount]. */
    val matchingSymptomCount: Int,
    val weightedConsumption: Double,
    val weightedSymptomMeals: Double,
    /** Portion-weighted share of consumption events followed by a symptom. */
    val triggerProbability: Double,
    /** Unweighted share of consumption events followed by a symptom. */
    val rawTriggerProbability: Double,
    val averageLatencyMinutes: Double?,
    val averageSymptomSeverity: Double?,
    val averagePortionWeight: Double,
    /** Confidence-adjusted 0..100 ranking score used for the top-3 list. */
    val score: Int,
    val dataQuality: DataQuality
)

/**
 * How often an activity was logged together with a symptom.
 *
 * Activities are only known when a symptom is logged, so this is a descriptive
 * association, not an activity-exposure risk. Multi-select activities mean the
 * shares can add up to more than 1.0.
 */
data class ActivityRisk(
    val activityName: String,
    val symptomCount: Int,
    val shareOfAllSymptoms: Double,
    val shareOfSymptomsWithActivity: Double,
    val averageSeverity: Double,
    val averageDurationMinutes: Double?,
    val durationSampleCount: Int
)

/**
 * Compares symptoms treated with one measure against symptoms with no measure
 * at all.
 *
 * A positive [effectScore] means lower average severity and/or shorter average
 * duration in the measure group. The score is descriptive, not causal.
 */
data class MeasureEffectiveness(
    val measureName: String,
    val withMeasureCount: Int,
    val withAverageSeverity: Double?,
    val withAverageDurationMinutes: Double?,
    val withoutMeasureCount: Int,
    val withoutAverageSeverity: Double?,
    val withoutAverageDurationMinutes: Double?,
    /** `with - without`; negative values mean improvement. */
    val severityDelta: Double?,
    /** `with - without` in minutes; negative values mean improvement. */
    val durationDeltaMinutes: Double?,
    /** -100..100; positive means better than no measure. Null without baseline. */
    val effectScore: Double?,
    /** At least three observations in both the measure and no-measure group. */
    val isReliable: Boolean
)

/**
 * Aggregated result for one fixed window (7, 14, 30 or 365 days).
 *
 * [dailyAverageSeverity] contains only local days with at least one symptom.
 * Days without symptoms are implicitly empty rather than null entries.
 */
data class AnalyticsSummary(
    val timeWindowDays: Int,
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val mealCount: Int,
    val symptomCount: Int,
    val averageSymptomSeverity: Double?,
    val averageSymptomDurationMinutes: Double?,
    val dailyAverageSeverity: List<DailySeverity>,
    val triggers: List<TriggerStatistic>,
    val topTriggers: List<TriggerStatistic>,
    val activities: List<ActivityRisk>,
    val topActivities: List<ActivityRisk>,
    val measures: List<MeasureEffectiveness>,
    val topEffectiveMeasures: List<MeasureEffectiveness>,
    /** Every local day of the window; missing days have a null burden. */
    val dailyBurden: List<DailyBurden> = emptyList(),
    val trend: BurdenTrend = BurdenTrend(0, null, null, null, null, null, 0, 0, emptyList()),
    val timeOfDay: TimeOfDaySummary = TimeOfDaySummary(
        buckets = (0..23).map {
            TimeOfDayBucket(it, 0, null, 0, 0)
        },
        peakStartHour = 0,
        peakEndHourExclusive = 5,
        strongSymptomCount = 0,
        strongSymptomsInPeak = 0,
        strongShareInPeak = 0.0,
        insight = null
    ),
    val factorAssociations: List<FactorAssociation> = emptyList(),
    val insights: List<String> = emptyList(),
    /**
     * Factors the app cannot evaluate because the user has not recorded the
     * necessary data. Listed explicitly so the UI does not imply "no effect".
     */
    val unavailableFactors: List<String> = emptyList()
)

/**
 * Pure analytics module for the reflux tracker.
 *
 * ## Matching rules
 * A meal occurrence is linked to a symptom when the symptom occurs between
 * 30 minutes and 4 hours after the meal (both boundaries inclusive). If two
 * meals overlap, the same symptom can belong to both; probabilities are per
 * consumption event, not per unique symptom. A meal with several foods
 * contributes one exposure per food. A meal without structured foods falls
 * back to its trimmed note as a text entry.
 *
 * ## Portion weighting
 * `Klein` → 0.8, `Normal`/empty → 1.0, `Groß` → 1.25. Numeric portion values
 * are used as relative weights (clamped to 0.1..10). The weighted trigger
 * probability is:
 *
 * `sum(portion weight of meals with a matching symptom) / sum(all portion weights)`
 *
 * This is an exposure-weighted ratio, not a medical risk probability.
 *
 * ## Edge cases
 * - Empty meal/symptom lists produce an empty but valid summary.
 * - Only entries inside the rolling `[now - days, now]` period are used.
 * - Symptoms with missing duration are ignored for duration averages.
 * - Activities/remedies are multi-select; each selected name is counted once
 *   per symptom.
 * - Measures are compared with symptoms that have no measure at all. Without
 *   such a baseline, [MeasureEffectiveness.effectScore] is null.
 * - The top lists use small, documented confidence/baseline rules and should
 *   be read as exploratory statistics.
 */
object RefluxAnalyticsService {

    const val MIN_DELAY_MINUTES: Long = 30L
    const val MAX_DELAY_MINUTES: Long = 240L

    private const val MINUTES_TO_MILLIS = 60_000L
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val MAX_WINDOW_DAYS = 3_650
    private const val SMALL_PORTION_WEIGHT = 0.8
    private const val NORMAL_PORTION_WEIGHT = 1.0
    private const val LARGE_PORTION_WEIGHT = 1.25
    private const val MIN_RELIABLE_SAMPLE_SIZE = 3
    private const val MIN_INSIGHT_OBSERVATIONS = 3
    private const val MIN_BASELINE_PROBABILITY = 0.05
    private const val STRONG_SYMPTOM_INTENSITY = 6
    private const val TIME_WINDOW_HOURS = 5

    /**
     * Aggregates all metrics for the rolling period `now - timeWindowDays` to
     * `now`.
     *
     * @param meals meals including their food relations.
     * @param symptoms symptoms including activity/remedy relations.
     * @param timeWindowDays a positive number of days; use [AnalyticsWindow] for
     *   the standard 7/14/30/365-day windows.
     * @param nowMillis injectable clock for tests and reproducible reports.
     * @param timeZone used for the local-day grouping in
     *   [AnalyticsSummary.dailyAverageSeverity].
     */
    fun getAnalyticsSummary(
        meals: List<MealWithFoods>,
        symptoms: List<SymptomWithDetails>,
        timeWindowDays: Int,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): AnalyticsSummary {
        require(timeWindowDays in 1..MAX_WINDOW_DAYS) {
            "timeWindowDays must be between 1 and $MAX_WINDOW_DAYS"
        }

        val periodStart = nowMillis - timeWindowDays.toLong() * DAY_MILLIS
        val filteredMeals = meals.filter { it.meal.timestamp in periodStart..nowMillis }
        val filteredSymptoms = symptoms.filter { it.symptom.timestamp in periodStart..nowMillis }
        val sortedSymptoms = filteredSymptoms.sortedBy { it.symptom.timestamp }

        val triggers = buildTriggers(filteredMeals, sortedSymptoms)
        val activities = buildActivities(filteredSymptoms)
        val measures = buildMeasureEffectiveness(filteredSymptoms)

        val dailySeverity = buildDailySeverity(filteredSymptoms, timeZone)
        val dailyBurden = buildDailyBurden(
            meals = filteredMeals,
            symptoms = filteredSymptoms,
            timeZone = timeZone,
            windowDays = timeWindowDays,
            nowMillis = nowMillis
        )
        val trend = buildBurdenTrend(dailyBurden)
        val timeOfDay = buildTimeOfDay(filteredSymptoms, timeZone)
        val factors = buildFactorAssociations(filteredMeals, sortedSymptoms, timeZone)
        val insights = buildInsights(timeOfDay, trend, factors)
        val durations = filteredSymptoms.mapNotNull { it.validDuration() }

        return AnalyticsSummary(
            timeWindowDays = timeWindowDays,
            periodStartMillis = periodStart,
            periodEndMillis = nowMillis,
            mealCount = filteredMeals.size,
            symptomCount = filteredSymptoms.size,
            averageSymptomSeverity = filteredSymptoms
                .map { it.symptom.intensity }
                .averageOrNull(),
            averageSymptomDurationMinutes = durations.averageOrNull(),
            dailyAverageSeverity = dailySeverity,
            triggers = triggers,
            topTriggers = triggers.filter { it.symptomMealCount > 0 }.take(3),
            activities = activities,
            topActivities = activities.take(3),
            measures = measures,
            topEffectiveMeasures = selectTopEffectiveMeasures(measures),
            dailyBurden = dailyBurden,
            trend = trend,
            timeOfDay = timeOfDay,
            factorAssociations = factors,
            insights = insights,
            unavailableFactors = listOf(
                "Stress",
                "Schlafqualität",
                "Abstand der letzten Mahlzeit zum Schlafbeginn",
                "Medikamenteneinnahme (nicht je Mahlzeit erfasst)"
            )
        )
    }

    /** Convenience overload for the standard reporting windows. */
    fun getAnalyticsSummary(
        meals: List<MealWithFoods>,
        symptoms: List<SymptomWithDetails>,
        window: AnalyticsWindow,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): AnalyticsSummary = getAnalyticsSummary(
        meals = meals,
        symptoms = symptoms,
        timeWindowDays = window.days,
        nowMillis = nowMillis,
        timeZone = timeZone
    )

    /**
     * Relative exposure weight for a portion.
     *
     * Categorical values match the app's meal editor. Numeric values are
     * treated as relative weights so a model with a 1–3 scale also works.
     * Unknown or empty values use the neutral weight 1.0.
     */
    fun portionWeight(portion: String): Double {
        val normalized = portion.trim().lowercase(Locale.GERMAN)
        return when (normalized) {
            "klein", "small" -> SMALL_PORTION_WEIGHT
            "groß", "gross", "large" -> LARGE_PORTION_WEIGHT
            "normal", "mittel", "medium", "" -> NORMAL_PORTION_WEIGHT
            else -> normalized.replace(',', '.').toDoubleOrNull()
                ?.takeIf { it > 0.0 }
                ?.coerceIn(0.1, 10.0)
                ?: NORMAL_PORTION_WEIGHT
        }
    }

    private data class FoodOccurrence(
        val itemKey: String,
        val itemName: String,
        val timestamp: Long,
        val portionWeight: Double
    )

    private fun buildTriggers(
        meals: List<MealWithFoods>,
        sortedSymptoms: List<SymptomWithDetails>
    ): List<TriggerStatistic> {
        val occurrences = mutableListOf<FoodOccurrence>()

        for (mealWithFoods in meals) {
            val meal = mealWithFoods.meal
            val mealPortionWeight = portionWeight(meal.portion)
            val foodNames = mealWithFoods.foods
                .map { it.name.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy(::normalizeItemKey)
                .ifEmpty {
                    listOf(meal.notes.trim()).filter { it.isNotEmpty() }
                }

            for (foodName in foodNames) {
                occurrences += FoodOccurrence(
                    itemKey = normalizeItemKey(foodName),
                    itemName = foodName,
                    timestamp = meal.timestamp,
                    portionWeight = mealPortionWeight
                )
            }
        }

        return occurrences
            .groupBy { it.itemKey }
            .map { (_, itemOccurrences) ->
                val firstOccurrence = itemOccurrences.first()
                val consumptionCount = itemOccurrences.size
                val weightedConsumption = itemOccurrences.sumOf { it.portionWeight }
                val latencies = mutableListOf<Double>()
                val intensities = mutableListOf<Int>()
                var symptomMealCount = 0
                var matchingSymptomCount = 0
                var weightedSymptomMeals = 0.0

                for (occurrence in itemOccurrences) {
                    val windowStart = occurrence.timestamp +
                        MIN_DELAY_MINUTES * MINUTES_TO_MILLIS
                    val windowEnd = occurrence.timestamp +
                        MAX_DELAY_MINUTES * MINUTES_TO_MILLIS
                    val matching = matchingSymptoms(
                        sortedSymptoms = sortedSymptoms,
                        startInclusive = windowStart,
                        endInclusive = windowEnd
                    )

                    if (matching.isNotEmpty()) {
                        symptomMealCount++
                        matchingSymptomCount += matching.size
                        weightedSymptomMeals += occurrence.portionWeight
                        // The first symptom is the onset latency; all matching
                        // symptoms contribute to the average severity.
                        latencies += (
                            matching.first().symptom.timestamp - occurrence.timestamp
                            ).toDouble() / MINUTES_TO_MILLIS
                        intensities += matching.map { it.symptom.intensity }
                    }
                }

                val weightedProbability = if (weightedConsumption > 0.0) {
                    (weightedSymptomMeals / weightedConsumption).coerceIn(0.0, 1.0)
                } else {
                    0.0
                }
                val rawProbability = symptomMealCount.toDouble() / consumptionCount
                val score = (
                    weightedProbability * 100.0 * confidenceFactor(consumptionCount)
                    ).roundToInt().coerceIn(0, 100)

                TriggerStatistic(
                    itemName = firstOccurrence.itemName,
                    consumptionCount = consumptionCount,
                    symptomMealCount = symptomMealCount,
                    matchingSymptomCount = matchingSymptomCount,
                    weightedConsumption = weightedConsumption,
                    weightedSymptomMeals = weightedSymptomMeals,
                    triggerProbability = weightedProbability,
                    rawTriggerProbability = rawProbability,
                    averageLatencyMinutes = latencies.averageOrNull(),
                    averageSymptomSeverity = intensities.averageOrNull(),
                    averagePortionWeight = weightedConsumption / consumptionCount,
                    score = score,
                    dataQuality = DataQuality.fromCount(consumptionCount)
                )
            }
            .sortedWith(
                compareByDescending<TriggerStatistic> { it.score }
                    .thenByDescending { it.triggerProbability }
                    .thenByDescending { it.consumptionCount }
                    .thenBy { it.itemName.lowercase(Locale.GERMAN) }
            )
    }

    private fun buildActivities(
        symptoms: List<SymptomWithDetails>
    ): List<ActivityRisk> {
        if (symptoms.isEmpty()) return emptyList()

        val byActivity = mutableMapOf<String, Pair<String, MutableList<SymptomWithDetails>>>()
        for (details in symptoms) {
            val names = details.activities
                .map { it.name.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy(::normalizeItemKey)

            for (name in names) {
                val key = normalizeItemKey(name)
                val entry = byActivity.getOrPut(key) { name to mutableListOf() }
                entry.second.add(details)
            }
        }

        val symptomsWithActivity = symptoms.count { details ->
            details.activities.any { it.name.isNotBlank() }
        }

        return byActivity.values.map { (activityName, matchingSymptoms) ->
            val durations = matchingSymptoms.mapNotNull { it.validDuration() }
            ActivityRisk(
                activityName = activityName,
                symptomCount = matchingSymptoms.size,
                shareOfAllSymptoms = matchingSymptoms.size.toDouble() / symptoms.size,
                shareOfSymptomsWithActivity = if (symptomsWithActivity > 0) {
                    matchingSymptoms.size.toDouble() / symptomsWithActivity
                } else {
                    0.0
                },
                averageSeverity = matchingSymptoms
                    .map { it.symptom.intensity }
                    .average(),
                averageDurationMinutes = durations.averageOrNull(),
                durationSampleCount = durations.size
            )
        }.sortedWith(
            compareByDescending<ActivityRisk> { it.symptomCount }
                .thenByDescending { it.averageSeverity }
                .thenBy { it.activityName.lowercase(Locale.GERMAN) }
        )
    }

    private fun buildMeasureEffectiveness(
        symptoms: List<SymptomWithDetails>
    ): List<MeasureEffectiveness> {
        if (symptoms.isEmpty()) return emptyList()

        // Baseline: all symptoms for which no measure was recorded.
        val withoutMeasure = symptoms.filter { it.remedies.isEmpty() }
        val withoutCount = withoutMeasure.size
        val withoutAverageSeverity = withoutMeasure
            .map { it.symptom.intensity }
            .averageOrNull()
        val withoutDurations = withoutMeasure.mapNotNull { it.validDuration() }
        val withoutAverageDuration = withoutDurations.averageOrNull()

        val byMeasure = mutableMapOf<String, Pair<String, MutableList<SymptomWithDetails>>>()
        for (details in symptoms) {
            val names = details.remedies
                .map { it.name.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy(::normalizeItemKey)

            for (name in names) {
                val key = normalizeItemKey(name)
                val entry = byMeasure.getOrPut(key) { name to mutableListOf() }
                entry.second.add(details)
            }
        }

        return byMeasure.values.map { (measureName, matchingSymptoms) ->
            val withCount = matchingSymptoms.size
            val withAverageSeverity = matchingSymptoms
                .map { it.symptom.intensity }
                .averageOrNull()
            val withDurations = matchingSymptoms.mapNotNull { it.validDuration() }
            val withAverageDuration = withDurations.averageOrNull()

            val severityDelta = if (
                withAverageSeverity != null && withoutAverageSeverity != null
            ) {
                withAverageSeverity - withoutAverageSeverity
            } else {
                null
            }
            val durationDelta = if (
                withAverageDuration != null && withoutAverageDuration != null
            ) {
                withAverageDuration - withoutAverageDuration
            } else {
                null
            }

            val effectScore = if (
                withCount > 0 && withoutCount > 0 && withoutAverageSeverity != null
            ) {
                val severityComponent = (-(severityDelta ?: 0.0) / 10.0) * 60.0
                val durationComponent = if (
                    durationDelta != null &&
                    withoutAverageDuration != null &&
                    withoutAverageDuration > 0.0
                ) {
                    (-durationDelta / withoutAverageDuration) * 40.0
                } else {
                    0.0
                }
                (severityComponent + durationComponent).coerceIn(-100.0, 100.0)
            } else {
                null
            }

            MeasureEffectiveness(
                measureName = measureName,
                withMeasureCount = withCount,
                withAverageSeverity = withAverageSeverity,
                withAverageDurationMinutes = withAverageDuration,
                withoutMeasureCount = withoutCount,
                withoutAverageSeverity = withoutAverageSeverity,
                withoutAverageDurationMinutes = withoutAverageDuration,
                severityDelta = severityDelta,
                durationDeltaMinutes = durationDelta,
                effectScore = effectScore,
                isReliable = withCount >= MIN_RELIABLE_SAMPLE_SIZE &&
                    withoutCount >= MIN_RELIABLE_SAMPLE_SIZE
            )
        }.sortedWith(
            compareByDescending<MeasureEffectiveness> {
                it.effectScore ?: Double.NEGATIVE_INFINITY
            }
                .thenByDescending { it.withMeasureCount }
                .thenBy { it.measureName.lowercase(Locale.GERMAN) }
        )
    }

    private fun selectTopEffectiveMeasures(
        measures: List<MeasureEffectiveness>
    ): List<MeasureEffectiveness> {
        val positive = measures.filter { (it.effectScore ?: 0.0) > 0.0 }
        if (positive.isEmpty()) return emptyList()

        val reliable = positive.filter { it.isReliable }
        val pool = if (reliable.isNotEmpty()) reliable else positive
        return pool.sortedWith(
            compareByDescending<MeasureEffectiveness> { it.effectScore ?: 0.0 }
                .thenByDescending { it.withMeasureCount }
                .thenBy { it.measureName.lowercase(Locale.GERMAN) }
        ).take(3)
    }

    private fun buildDailySeverity(
        symptoms: List<SymptomWithDetails>,
        timeZone: TimeZone
    ): List<DailySeverity> {
        if (symptoms.isEmpty()) return emptyList()

        return symptoms
            .groupBy { startOfLocalDay(it.symptom.timestamp, timeZone) }
            .map { (dayStart, symptomsOnDay) ->
                DailySeverity(
                    dayStartMillis = dayStart,
                    symptomCount = symptomsOnDay.size,
                    averageSeverity = symptomsOnDay
                        .map { it.symptom.intensity }
                        .average()
                )
            }
            .sortedBy { it.dayStartMillis }
    }

    /**
     * Builds one bucket per local day of the reporting window.
     *
     * GERD burden (0–100), documented definition:
     * `intensityPart = (average intensity / 10) * 60`,
     * `frequencyPart = (min(symptom count, 4) / 4) * 40`.
     * A documented day without symptoms has burden 0. A day without *any*
     * entry (no meal, no symptom) has `burden = null` and is never treated as
     * symptom-free. A spike is a non-zero burden of at least
     * `max(mean + standard deviation, mean + 10)` over the window's documented
     * days (at least two documented days required).
     */
    private fun buildDailyBurden(
        meals: List<MealWithFoods>,
        symptoms: List<SymptomWithDetails>,
        timeZone: TimeZone,
        windowDays: Int,
        nowMillis: Long
    ): List<DailyBurden> {
        val today = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstDay = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -(windowDays - 1))
        }

        val dayStarts = mutableListOf<Long>()
        val cursor = firstDay.clone() as Calendar
        while (!cursor.after(today)) {
            dayStarts += cursor.timeInMillis
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }

        val mealsByDay = meals.groupBy { startOfLocalDay(it.meal.timestamp, timeZone) }
        val symptomsByDay = symptoms.groupBy { startOfLocalDay(it.symptom.timestamp, timeZone) }

        val base = dayStarts.map { dayStart ->
            val mealsOnDay = mealsByDay[dayStart].orEmpty()
            val symptomsOnDay = symptomsByDay[dayStart].orEmpty()
            val average = symptomsOnDay
                .map { it.symptom.intensity }
                .averageOrNull()
            val burden = when {
                symptomsOnDay.isEmpty() && mealsOnDay.isEmpty() -> null
                symptomsOnDay.isEmpty() -> 0.0
                else -> {
                    val intensityPart = ((average ?: 0.0) / 10.0) * 60.0
                    val frequencyPart = (minOf(symptomsOnDay.size, 4) / 4.0) * 40.0
                    (intensityPart + frequencyPart).coerceIn(0.0, 100.0)
                }
            }
            DailyBurden(
                dayStartMillis = dayStart,
                symptomCount = symptomsOnDay.size,
                averageSeverity = average,
                burden = burden,
                hasEntries = mealsOnDay.isNotEmpty() || symptomsOnDay.isNotEmpty(),
                isSpike = false
            )
        }

        val values = base.mapNotNull { it.burden }
        if (values.size < 2) return base
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        val standardDeviation = kotlin.math.sqrt(variance)
        val threshold = maxOf(mean + standardDeviation, mean + 10.0)

        return base.map { day ->
            if (day.burden != null && day.burden > 0.0 && day.burden >= threshold) {
                day.copy(isSpike = true)
            } else {
                day
            }
        }
    }

    private fun buildBurdenTrend(dailyBurden: List<DailyBurden>): BurdenTrend {
        val documented = dailyBurden.mapNotNull { day ->
            day.burden?.let { day.dayStartMillis to it }
        }
        var fittedStart: Double? = null
        var fittedEnd: Double? = null
        var fittedStartDay: Long? = null
        var fittedEndDay: Long? = null
        val slope = if (documented.size >= 3) {
            // Use calendar-day distance, not the ordinal of the observation, so
            // a gap does not turn the slope into "per documented entry".
            val firstDay = documented.first().first
            val xs = documented.map {
                (it.first - firstDay).toDouble() / DAY_MILLIS
            }
            val ys = documented.map { it.second }
            val meanX = xs.average()
            val meanY = ys.average()
            var numerator = 0.0
            var denominator = 0.0
            xs.forEachIndexed { index, x ->
                val dx = x - meanX
                numerator += dx * (ys[index] - meanY)
                denominator += dx * dx
            }
            if (denominator == 0.0) {
                null
            } else {
                val result = numerator / denominator
                val intercept = meanY - result * meanX
                fittedStart = intercept.coerceIn(0.0, 100.0)
                fittedEnd = (intercept + result * xs.last()).coerceIn(0.0, 100.0)
                fittedStartDay = documented.first().first
                fittedEndDay = documented.last().first
                result
            }
        } else {
            null
        }

        val half = dailyBurden.size / 2
        val firstHalf = dailyBurden.take(half).mapNotNull { it.burden }
        val secondHalf = dailyBurden.drop(half).mapNotNull { it.burden }
        val firstAverage = firstHalf.averageOrNull()
        val secondAverage = secondHalf.averageOrNull()
        val absoluteChange = if (firstAverage != null && secondAverage != null) {
            secondAverage - firstAverage
        } else {
            null
        }
        val percentChange = if (
            absoluteChange != null && firstAverage != null && firstAverage > 0.0
        ) {
            (absoluteChange / firstAverage) * 100.0
        } else {
            null
        }

        return BurdenTrend(
            documentedDayCount = documented.size,
            slopePerDay = slope,
            firstHalfAverageBurden = firstAverage,
            secondHalfAverageBurden = secondAverage,
            changeAbsolute = absoluteChange,
            changePercent = percentChange,
            symptomFreeDayCount = dailyBurden.count { it.isSymptomFree },
            missingDayCount = dailyBurden.count { it.isMissing },
            spikeDayStarts = dailyBurden.filter { it.isSpike }.map { it.dayStartMillis },
            firstHalfDocumentedDays = firstHalf.size,
            secondHalfDocumentedDays = secondHalf.size,
            fittedStartBurden = fittedStart,
            fittedEndBurden = fittedEnd,
            fittedStartDayStartMillis = fittedStartDay,
            fittedEndDayStartMillis = fittedEndDay
        )
    }

    private fun buildTimeOfDay(
        symptoms: List<SymptomWithDetails>,
        timeZone: TimeZone
    ): TimeOfDaySummary {
        val byHour = IntArray(24)
        val strongByHour = IntArray(24)
        val intensityByHour = IntArray(24)
        for (details in symptoms) {
            val hour = Calendar.getInstance(timeZone).apply {
                timeInMillis = details.symptom.timestamp
            }.get(Calendar.HOUR_OF_DAY)
            byHour[hour]++
            intensityByHour[hour] += details.symptom.intensity
            if (details.symptom.intensity >= STRONG_SYMPTOM_INTENSITY) {
                strongByHour[hour]++
            }
        }

        val buckets = (0..23).map { hour ->
            TimeOfDayBucket(
                hour = hour,
                symptomCount = byHour[hour],
                averageIntensity = if (byHour[hour] == 0) {
                    null
                } else {
                    intensityByHour[hour].toDouble() / byHour[hour]
                },
                strongSymptomCount = strongByHour[hour],
                intensitySum = intensityByHour[hour]
            )
        }

        val total = symptoms.size
        val strongTotal = strongByHour.sum()
        val strongPeak = peakWindow(strongByHour, byHour)
        val allPeak = peakWindow(byHour, strongByHour)

        val useStrong = strongTotal >= MIN_INSIGHT_OBSERVATIONS
        val peak = if (useStrong) strongPeak else allPeak
        val peakCount = if (useStrong) strongTotal else total
        val inPeak = if (useStrong) {
            strongPeak.count
        } else {
            byHour[allPeak.start] + byHour[(allPeak.start + 1) % 24] +
                byHour[(allPeak.start + 2) % 24] + byHour[(allPeak.start + 3) % 24] +
                byHour[(allPeak.start + 4) % 24]
        }
        val share = if (peakCount > 0) inPeak.toDouble() / peakCount else 0.0
        val end = (peak.start + TIME_WINDOW_HOURS) % 24

        val insight = if (peakCount >= MIN_INSIGHT_OBSERVATIONS && share >= 0.5) {
            if (useStrong) {
                String.format(
                    Locale.GERMAN,
                    "%d %% deiner stärkeren Symptome (Intensität ≥ %d) traten zwischen " +
                        "%02d:00 und %02d:00 Uhr auf (%d von %d).",
                    (share * 100.0).roundToInt(),
                    STRONG_SYMPTOM_INTENSITY,
                    peak.start,
                    end,
                    inPeak,
                    peakCount
                )
            } else {
                String.format(
                    Locale.GERMAN,
                    "%d %% deiner erfassten Symptome traten zwischen %02d:00 und %02d:00 Uhr " +
                        "auf (%d von %d).",
                    (share * 100.0).roundToInt(),
                    peak.start,
                    end,
                    inPeak,
                    peakCount
                )
            }
        } else {
            null
        }

        return TimeOfDaySummary(
            buckets = buckets,
            peakStartHour = peak.start,
            peakEndHourExclusive = end,
            strongSymptomCount = strongTotal,
            strongSymptomsInPeak = if (useStrong) inPeak else strongPeak.count,
            strongShareInPeak = if (useStrong) share else 0.0,
            insight = insight
        )
    }

    /**
     * Returns the contiguous [TIME_WINDOW_HOURS]-hour window with the most
     * events. Ties are broken by the secondary count and then by the earlier
     * start hour, which keeps the result deterministic.
     */
    private fun peakWindow(primary: IntArray, secondary: IntArray): PeakWindow {
        var best = PeakWindow(0, -1, -1)
        for (start in 0..23) {
            var count = 0
            var secondaryCount = 0
            for (offset in 0 until TIME_WINDOW_HOURS) {
                val hour = (start + offset) % 24
                count += primary[hour]
                secondaryCount += secondary[hour]
            }
            if (count > best.count) {
                best = PeakWindow(start, count, secondaryCount)
            } else if (count == best.count && count > 0 && secondaryCount > best.secondaryCount) {
                best = PeakWindow(start, count, secondaryCount)
            }
        }
        return best
    }

    private data class PeakWindow(val start: Int, val count: Int, val secondaryCount: Int)

    /**
     * Meal-level factor comparison for categories and behaviours.
     *
     * Only factors that actually occur in the window and also have at least one
     * comparison meal are returned. Missing categories are therefore neither a
     * positive nor a negative observation.
     */
    private fun buildFactorAssociations(
        meals: List<MealWithFoods>,
        sortedSymptoms: List<SymptomWithDetails>,
        timeZone: TimeZone
    ): List<FactorAssociation> {
        if (meals.size < 2) return emptyList()

        data class Observation(
            val meal: MealWithFoods,
            val hasSymptom: Boolean,
            val intensities: List<Int>,
            val latencies: List<Double>
        )

        val observations = meals.map { mealWithFoods ->
            val start = mealWithFoods.meal.timestamp + MIN_DELAY_MINUTES * MINUTES_TO_MILLIS
            val end = mealWithFoods.meal.timestamp + MAX_DELAY_MINUTES * MINUTES_TO_MILLIS
            val matching = matchingSymptoms(sortedSymptoms, start, end)
            Observation(
                meal = mealWithFoods,
                hasSymptom = matching.isNotEmpty(),
                intensities = matching.map { it.symptom.intensity },
                latencies = matching.map {
                    (it.symptom.timestamp - mealWithFoods.meal.timestamp).toDouble() /
                        MINUTES_TO_MILLIS
                }
            )
        }

        val factorDefinitions = mutableListOf<FactorDefinition>()

        for (category in FoodCategories.ALL) {
            factorDefinitions += FactorDefinition(
                key = "category:${category.key}",
                label = category.label,
                kind = FactorKind.CATEGORY,
                exposure = { meal ->
                    if (category.key == FoodCategories.LARGE_PORTION) {
                        isLargePortion(meal.meal.portion) ||
                            meal.foods.any {
                                category.key in FoodCategories.decode(it.categories)
                            }
                    } else {
                        meal.foods.any {
                            category.key in FoodCategories.decode(it.categories)
                        }
                    }
                }
            )
        }

        factorDefinitions += FactorDefinition(
            key = "behaviour:evening_meal",
            label = "Abendmahlzeit (ab 18 Uhr)",
            kind = FactorKind.BEHAVIOUR,
            exposure = { meal -> localHour(meal.meal.timestamp, timeZone) >= 18 }
        )
        factorDefinitions += FactorDefinition(
            key = "behaviour:late_meal",
            label = "Späte Mahlzeit (ab 21 Uhr)",
            kind = FactorKind.BEHAVIOUR,
            exposure = { meal -> localHour(meal.meal.timestamp, timeZone) >= 21 }
        )

        return factorDefinitions.mapNotNull { definition ->
            val exposed = observations.filter { definition.exposure(it.meal) }
            val unexposed = observations.filterNot { definition.exposure(it.meal) }
            if (exposed.isEmpty() || unexposed.isEmpty()) return@mapNotNull null

            val exposedSymptomCount = exposed.count { it.hasSymptom }
            val unexposedSymptomCount = unexposed.count { it.hasSymptom }
            val probabilityWith = exposedSymptomCount.toDouble() / exposed.size
            val probabilityWithout = unexposedSymptomCount.toDouble() / unexposed.size
            val absoluteDifference = probabilityWith - probabilityWithout

            val sampleSize = minOf(exposed.size, unexposed.size)
            val relative = if (
                exposed.size >= MIN_RELIABLE_SAMPLE_SIZE &&
                unexposed.size >= MIN_RELIABLE_SAMPLE_SIZE &&
                probabilityWithout >= MIN_BASELINE_PROBABILITY
            ) {
                (absoluteDifference / probabilityWithout) * 100.0
            } else {
                null
            }

            val intensities = exposed.flatMap { it.intensities }
            val latencies = exposed.flatMap { it.latencies }

            FactorAssociation(
                key = definition.key,
                label = definition.label,
                kind = definition.kind,
                exposedMealCount = exposed.size,
                unexposedMealCount = unexposed.size,
                exposedSymptomMealCount = exposedSymptomCount,
                unexposedSymptomMealCount = unexposedSymptomCount,
                probabilityWith = probabilityWith,
                probabilityWithout = probabilityWithout,
                absoluteDifference = absoluteDifference,
                relativeChangePercent = relative,
                averageIntensityWith = intensities.averageOrNull(),
                averageLatencyWithMinutes = latencies.averageOrNull(),
                dataQuality = DataQuality.fromCount(sampleSize),
                isReliable = sampleSize >= MIN_RELIABLE_SAMPLE_SIZE
            )
        }.sortedWith(
            compareByDescending<FactorAssociation> { it.isReliable }
                .thenByDescending { it.dataQuality.ordinal }
                .thenByDescending { kotlin.math.abs(it.absoluteDifference) }
                .thenBy { it.label.lowercase(Locale.GERMAN) }
        )
    }

    private data class FactorDefinition(
        val key: String,
        val label: String,
        val kind: FactorKind,
        val exposure: (MealWithFoods) -> Boolean
    )

    private fun buildInsights(
        timeOfDay: TimeOfDaySummary,
        trend: BurdenTrend,
        factors: List<FactorAssociation>
    ): List<String> {
        val insights = mutableListOf<String>()

        timeOfDay.insight?.let { insights += it }

        val slope = trend.slopePerDay
        if (slope != null && trend.documentedDayCount >= 3) {
            val direction = when {
                slope <= -0.2 -> "gesunken"
                slope >= 0.2 -> "gestiegen"
                else -> "stabil geblieben"
            }
            insights += String.format(
                Locale.GERMAN,
                "Der tägliche GERD-Burden ist im Zeitraum %s (lineare Tendenz %+.1f Punkte " +
                    "pro Tag über %d Tage mit Einträgen).",
                direction,
                slope,
                trend.documentedDayCount
            )
        }

        if (trend.documentedDayCount >= 3 && trend.symptomFreeDayCount > 0) {
            insights += "An ${trend.symptomFreeDayCount} von " +
                "${trend.documentedDayCount} dokumentierten Tagen wurden keine Symptome erfasst."
        }

        val topFactor = factors.firstOrNull {
            it.isReliable && it.relativeChangePercent != null &&
                kotlin.math.abs(it.relativeChangePercent) >= 20.0
        }
        if (topFactor != null && topFactor.relativeChangePercent != null) {
            insights += String.format(
                Locale.GERMAN,
                "Mahlzeiten mit %s waren in deinen Daten %+.0f %% häufiger von Symptomen im " +
                    "30-Min.–4-Std.-Fenster gefolgt (%d von %d gegenüber %d von %d). " +
                    "Das ist ein beobachteter Zusammenhang, keine Ursache.",
                topFactor.label,
                topFactor.relativeChangePercent,
                topFactor.exposedSymptomMealCount,
                topFactor.exposedMealCount,
                topFactor.unexposedSymptomMealCount,
                topFactor.unexposedMealCount
            )
        }

        return insights
    }

    /**
     * True for the meal-level "large portion" category: the categorical value
     * "Groß" or a numeric relative weight of at least 1.25.
     */
    fun isLargePortion(portion: String): Boolean {
        val normalized = portion.trim().lowercase(Locale.GERMAN)
        if (normalized == "groß" || normalized == "gross" || normalized == "large") return true
        return portionWeight(portion) >= LARGE_PORTION_WEIGHT
    }

    private fun localHour(timestamp: Long, timeZone: TimeZone): Int =
        Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }
            .get(Calendar.HOUR_OF_DAY)

    private fun matchingSymptoms(
        sortedSymptoms: List<SymptomWithDetails>,
        startInclusive: Long,
        endInclusive: Long
    ): List<SymptomWithDetails> {
        if (sortedSymptoms.isEmpty() || startInclusive > endInclusive) return emptyList()
        val from = lowerBound(sortedSymptoms, startInclusive)
        val to = upperBound(sortedSymptoms, endInclusive)
        return if (from < to) sortedSymptoms.subList(from, to) else emptyList()
    }

    /** First index whose timestamp is >= [time]. */
    private fun lowerBound(
        sortedSymptoms: List<SymptomWithDetails>,
        time: Long
    ): Int {
        var low = 0
        var high = sortedSymptoms.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (sortedSymptoms[mid].symptom.timestamp < time) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low
    }

    /** First index whose timestamp is > [time]. */
    private fun upperBound(
        sortedSymptoms: List<SymptomWithDetails>,
        time: Long
    ): Int {
        var low = 0
        var high = sortedSymptoms.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (sortedSymptoms[mid].symptom.timestamp <= time) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low
    }

    private fun confidenceFactor(consumptionCount: Int): Double = when {
        consumptionCount <= 0 -> 0.0
        consumptionCount == 1 -> 0.35
        consumptionCount == 2 -> 0.55
        consumptionCount == 3 -> 0.70
        consumptionCount == 4 -> 0.80
        consumptionCount in 5..9 -> 0.90
        else -> 1.0
    }

    private fun normalizeItemKey(value: String): String = value
        .trim()
        .lowercase(Locale.GERMAN)
        .replace(Regex("\\s+"), " ")

    private fun SymptomWithDetails.validDuration(): Int? =
        symptom.durationMinutes?.takeIf { it >= 0 }

    private fun startOfLocalDay(timestamp: Long, timeZone: TimeZone): Long {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun Iterable<Number>.averageOrNull(): Double? {
        var sum = 0.0
        var count = 0
        for (value in this) {
            sum += value.toDouble()
            count++
        }
        return if (count == 0) null else sum / count
    }
}
