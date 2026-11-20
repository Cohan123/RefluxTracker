package com.example.report

import com.example.analysis.DataQuality
import com.example.analysis.RefluxAnalyticsService
import com.example.data.model.FoodCategories
import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealTypes
import com.example.data.model.MealWithFoods
import com.example.data.model.RemedyEntity
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomTypes
import com.example.data.model.SymptomTypesJson
import com.example.data.model.SymptomWithDetails
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The PDF itself is rendered by Android's `PdfDocument`; its content model is
 * built here and verified independently of the canvas.
 */
class DoctorReportBuilderTest {

    private companion object {
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        const val HOUR = 60L * 60L * 1000L
        const val NOW: Long = 1_699_963_200_000L
    }

    private lateinit var previousZone: TimeZone

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(UTC)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    @Test
    fun `report uses documented values and never invents medication data`() {
        val fattyFood = FoodEntity(
            id = 1,
            name = "Pizza",
            category = "Test",
            categories = FoodCategories.encode(listOf(FoodCategories.FAT))
        )
        val neutralFood = FoodEntity(id = 2, name = "Reis", category = "Test")

        val meals = listOf(
            meal(1, NOW - 6 * 24 * HOUR, fattyFood),
            meal(2, NOW - 5 * 24 * HOUR, fattyFood),
            meal(3, NOW - 4 * 24 * HOUR, fattyFood),
            meal(4, NOW - 3 * 24 * HOUR, neutralFood),
            meal(5, NOW - 2 * 24 * HOUR, neutralFood),
            meal(6, NOW - 1 * 24 * HOUR, neutralFood)
        )

        val symptoms = listOf(
            symptom(1, NOW - 6 * 24 * HOUR + HOUR, intensity = 5, types = listOf(SymptomTypes.SODBRENNEN)),
            symptom(2, NOW - 5 * 24 * HOUR + HOUR, intensity = 7, types = listOf(SymptomTypes.SODBRENNEN)),
            symptom(
                3,
                NOW - 4 * 24 * HOUR + HOUR,
                intensity = 6,
                types = listOf(SymptomTypes.SAEURERUECKFLUSS),
                night = true,
                remedy = "Medikament"
            ),
            symptom(4, NOW - 3 * 24 * HOUR + HOUR, intensity = 4, types = listOf(SymptomTypes.AUFSTOSSEN)),
            symptom(
                5,
                NOW - 1 * 24 * HOUR + HOUR,
                intensity = 8,
                types = listOf(SymptomTypes.SODBRENNEN),
                night = true,
                remedy = "Medikament"
            )
        )

        val summary = RefluxAnalyticsService.getAnalyticsSummary(
            meals = meals,
            symptoms = symptoms,
            timeWindowDays = 7,
            nowMillis = NOW,
            timeZone = UTC
        )
        val report = DoctorReportBuilder.build(
            meals = meals,
            symptoms = symptoms,
            summary = summary,
            userName = "  Alex Test  ",
            generatedAt = NOW
        )

        assertEquals("Alex Test", report.userName)
        assertEquals(NOW, report.generatedAt)
        assertEquals(summary.periodStartMillis, report.periodStart)
        assertEquals(summary.periodEndMillis, report.periodEnd)
        assertEquals(7, report.timeWindowDays)

        assertEquals(5, report.symptoms.totalSymptoms)
        assertEquals(3, report.symptoms.daysWithHeartburn)
        assertEquals(2, report.symptoms.nightsWithReflux)
        assertEquals(summary.averageSymptomSeverity!!, report.symptoms.averageIntensity!!, 1e-9)
        assertEquals(summary.trend.documentedDayCount, report.symptoms.documentedDays)

        assertEquals(2, report.medication.documentedIntakes)
        assertEquals(2, report.medication.distinctDays)
        assertEquals(listOf("Medikament"), report.medication.names)
        assertEquals(2, report.medication.intakeAtTimes.size)
        assertNull("dose is not modelled and must not be guessed", report.medication.dose)
        assertNull(report.medication.plannedFrequency)
        assertNull(
            "adherence needs a documented plan",
            report.medication.adherencePercent
        )
        assertTrue(report.medication.documentedIntakesPerWeek!! > 0.0)

        val fat = report.associations.single { it.label == "Fettig" }
        assertEquals(3, fat.exposedMeals)
        assertEquals(3, fat.unexposedMeals)
        assertEquals(2, fat.exposedSymptomMeals)
        assertEquals(1, fat.unexposedSymptomMeals)
        assertEquals(100.0, fat.relativeChangePercent!!, 1e-9)
        assertEquals(DataQuality.LOW, fat.dataQuality)
        assertTrue(fat.isCategory)

        assertNull("GerdQ is not tracked yet", report.gerdQ)
        assertEquals(7, report.dailyBurden.size)
        assertEquals(summary.insights, report.insights)
        assertEquals(24, report.timeOfDayBuckets.size)
    }

    @Test
    fun `blank name stays absent`() {
        val summary = RefluxAnalyticsService.getAnalyticsSummary(
            meals = emptyList(),
            symptoms = emptyList(),
            timeWindowDays = 7,
            nowMillis = NOW,
            timeZone = UTC
        )

        val report = DoctorReportBuilder.build(
            meals = emptyList(),
            symptoms = emptyList(),
            summary = summary,
            userName = "   "
        )

        assertNull(report.userName)
        assertEquals(0, report.symptoms.totalSymptoms)
        assertEquals(0, report.medication.documentedIntakes)
        assertNull(report.medication.documentedIntakesPerWeek)
        assertTrue(report.associations.isEmpty())
        assertNull(report.gerdQ)
    }

    private fun meal(id: Long, timestamp: Long, food: FoodEntity): MealWithFoods =
        MealWithFoods(
            meal = MealEntity(
                id = id,
                timestamp = timestamp,
                mealType = MealTypes.MITTAGESSEN,
                portion = "Normal",
                notes = ""
            ),
            foods = listOf(food.copy(id = id * 100))
        )

    private fun symptom(
        id: Long,
        timestamp: Long,
        intensity: Int,
        types: List<String>,
        night: Boolean = false,
        remedy: String? = null
    ): SymptomWithDetails {
        val actualTimestamp = if (night) {
            // 23:00 on the same local day, still inside the reporting period.
            Calendar.getInstance(UTC).apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 0)
            }.timeInMillis
        } else {
            timestamp
        }
        return SymptomWithDetails(
            symptom = SymptomEntity(
                id = id,
                timestamp = actualTimestamp,
                symptomType = types.first(),
                intensity = intensity,
                symptomTypesJson = SymptomTypesJson.encode(types)
            ),
            activities = emptyList(),
            remedies = remedy?.let { listOf(RemedyEntity(id = id * 100, name = it)) }
                ?: emptyList()
        )
    }
}
