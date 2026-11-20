package com.example.report

import com.example.analysis.AnalyticsSummary
import com.example.analysis.BurdenTrend
import com.example.analysis.DailyBurden
import com.example.analysis.DataQuality
import com.example.analysis.FactorAssociation
import com.example.analysis.TimeOfDayBucket
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomTypes
import com.example.data.model.SymptomTypesJson
import com.example.data.model.SymptomWithDetails
import com.example.util.DateTimeUtils
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Immutable input for the doctor report PDF.
 *
 * Everything in here is derived from documented entries. Values that cannot be
 * calculated stay `null` and are shown as "nicht erfasst"/"nicht berechenbar"
 * in the PDF. The report never estimates a missing measurement.
 */
data class DoctorReportData(
    val generatedAt: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val timeWindowDays: Int,
    val userName: String?,
    val symptoms: ReportSymptomSummary,
    val medication: ReportMedicationSummary,
    val course: ReportCourseSummary,
    val associations: List<ReportAssociation>,
    /** GerdQ is not part of the data model; always null until it is tracked. */
    val gerdQ: ReportGerdQ? = null,
    val dailyBurden: List<DailyBurden>,
    val timeOfDayBuckets: List<TimeOfDayBucket>,
    val insights: List<String>
)

data class ReportSymptomSummary(
    val totalSymptoms: Int,
    val documentedDays: Int,
    val daysWithHeartburn: Int,
    val nightsWithReflux: Int,
    val averageIntensity: Double?,
    val symptomFreeDays: Int,
    val peakStartHour: Int,
    val peakEndHourExclusive: Int
)

data class ReportMedicationSummary(
    val documentedIntakes: Int,
    val distinctDays: Int,
    val firstIntakeAt: Long?,
    val lastIntakeAt: Long?,
    /** Names of the matched remedy entries (e.g. "Medikament", "Pantoprazol"). */
    val names: List<String> = emptyList(),
    /** Documented intake timestamps, oldest first. */
    val intakeAtTimes: List<Long> = emptyList(),
    /** Documented intakes per week across the documented span; null if < 1 day. */
    val documentedIntakesPerWeek: Double?,
    /** The app does not track a prescribed dose/frequency, so these stay null. */
    val dose: String? = null,
    val plannedFrequency: String? = null,
    /** Adherence needs a plan; without one it cannot be calculated. */
    val adherencePercent: Double? = null
)

data class ReportCourseSummary(
    val trend: BurdenTrend,
    /** `(second half - first half) / first half * 100`; null if not comparable. */
    val changePercent: Double?
) {
    val changeAbsolute: Double? get() = trend.changeAbsolute
}

data class ReportAssociation(
    val label: String,
    val exposedSymptomMeals: Int,
    val exposedMeals: Int,
    val unexposedSymptomMeals: Int,
    val unexposedMeals: Int,
    val probabilityWith: Double,
    val probabilityWithout: Double,
    val relativeChangePercent: Double?,
    val dataQuality: DataQuality,
    val isCategory: Boolean
)

data class ReportGerdQ(
    val startValue: Int,
    val startDate: Long,
    val currentValue: Int,
    val currentDate: Long
)

/**
 * Builds the report model from the same fixed-window analytics that powers the
 * analysis screen.
 *
 * Definition of the course comparison: the reporting window is split into a
 * first and a second half by calendar day; the daily GERD burden (documented
 * earlier) is averaged over documented days in each half. Days without entries
 * are excluded, and the percentage change is `(second - first) / first * 100`.
 */
object DoctorReportBuilder {

    /** Night window for "nights with reflux": 22:00 (inclusive) to 06:00. */
    private const val NIGHT_START_HOUR = 22
    private const val NIGHT_END_HOUR = 6

    fun build(
        meals: List<MealWithFoods>,
        symptoms: List<SymptomWithDetails>,
        summary: AnalyticsSummary,
        userName: String?,
        generatedAt: Long = System.currentTimeMillis()
    ): DoctorReportData {
        val periodStart = summary.periodStartMillis
        val periodEnd = summary.periodEndMillis
        val filtered = symptoms.filter { it.symptom.timestamp in periodStart..periodEnd }

        val heartburnDays = filtered
            .filter { details ->
                SymptomTypesJson
                    .decodeOrPrimary(details.symptom.symptomTypesJson, details.symptom.symptomType)
                    .any { it == SymptomTypes.SODBRENNEN }
            }
            .map { DateTimeUtils.startOfLocalDay(it.symptom.timestamp) }
            .distinct()
            .size

        val refluxNights = filtered
            .filter { details ->
                val types = SymptomTypesJson.decodeOrPrimary(
                    details.symptom.symptomTypesJson,
                    details.symptom.symptomType
                )
                val isReflux = types.any {
                    it == SymptomTypes.SODBRENNEN || it == SymptomTypes.SAEURERUECKFLUSS
                }
                val hour = Calendar.getInstance().apply {
                    timeInMillis = details.symptom.timestamp
                }.get(Calendar.HOUR_OF_DAY)
                isReflux && (hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR)
            }
            .map { DateTimeUtils.startOfLocalDay(it.symptom.timestamp) }
            .distinct()
            .size

        val medication = buildMedication(filtered, periodStart, periodEnd)

        return DoctorReportData(
            generatedAt = generatedAt,
            periodStart = periodStart,
            periodEnd = periodEnd,
            timeWindowDays = summary.timeWindowDays,
            userName = userName?.trim()?.takeIf { it.isNotEmpty() },
            symptoms = ReportSymptomSummary(
                totalSymptoms = summary.symptomCount,
                documentedDays = summary.trend.documentedDayCount,
                daysWithHeartburn = heartburnDays,
                nightsWithReflux = refluxNights,
                averageIntensity = summary.averageSymptomSeverity,
                symptomFreeDays = summary.trend.symptomFreeDayCount,
                peakStartHour = summary.timeOfDay.peakStartHour,
                peakEndHourExclusive = summary.timeOfDay.peakEndHourExclusive
            ),
            medication = medication,
            course = ReportCourseSummary(
                trend = summary.trend,
                changePercent = summary.trend.changePercent
            ),
            associations = summary.factorAssociations
                .filter { it.isReliable && it.relativeChangePercent != null }
                .sortedByDescending { abs(it.relativeChangePercent ?: 0.0) }
                .take(8)
                .map { it.toReportAssociation() },
            gerdQ = null,
            dailyBurden = summary.dailyBurden,
            timeOfDayBuckets = summary.timeOfDay.buckets,
            insights = summary.insights
        )
    }

    private fun buildMedication(
        symptoms: List<SymptomWithDetails>,
        periodStart: Long,
        periodEnd: Long
    ): ReportMedicationSummary {
        val intakes = symptoms.filter { details ->
            details.remedies.any { remedy ->
                remedy.name.trim().lowercase(Locale.GERMAN).contains("medikament")
            }
        }
        val names = intakes
            .flatMap { details -> details.remedies.map { it.name } }
            .map { it.trim() }
            .filter { it.lowercase(Locale.GERMAN).contains("medikament") }
            .distinct()
            .sorted()
        val timestamps = intakes.map { it.symptom.timestamp }.sorted()
        val first = timestamps.minOrNull()
        val last = timestamps.maxOrNull()
        val distinctDays = timestamps
            .map { DateTimeUtils.startOfLocalDay(it) }
            .distinct()
            .size
        val perWeek = if (first != null && last != null && last > first) {
            val spanDays = (last - first).toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)
            if (spanDays >= 1.0) intakes.size / (spanDays / 7.0) else null
        } else {
            null
        }

        return ReportMedicationSummary(
            documentedIntakes = intakes.size,
            distinctDays = distinctDays,
            firstIntakeAt = first?.takeIf { it in periodStart..periodEnd },
            lastIntakeAt = last?.takeIf { it in periodStart..periodEnd },
            names = names,
            intakeAtTimes = timestamps,
            documentedIntakesPerWeek = perWeek
        )
    }

    private fun FactorAssociation.toReportAssociation(): ReportAssociation =
        ReportAssociation(
            label = label,
            exposedSymptomMeals = exposedSymptomMealCount,
            exposedMeals = exposedMealCount,
            unexposedSymptomMeals = unexposedSymptomMealCount,
            unexposedMeals = unexposedMealCount,
            probabilityWith = probabilityWith,
            probabilityWithout = probabilityWithout,
            relativeChangePercent = relativeChangePercent,
            dataQuality = dataQuality,
            isCategory = kind == com.example.analysis.FactorKind.CATEGORY
        )
}
