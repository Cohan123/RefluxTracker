package com.example.analysis

import com.example.data.model.ActivityEntity
import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealTypes
import com.example.data.model.MealWithFoods
import com.example.data.model.RemedyEntity
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomTypes
import com.example.data.model.SymptomWithDetails
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefluxAnalyticsServiceTest {

    private companion object {
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        const val MINUTE = 60_000L
        const val HOUR = 60L * MINUTE
        const val DAY = 24L * HOUR

        /** 2023-11-14T12:00:00Z, a stable noon base for local-day grouping. */
        const val NOW = 1_699_963_200_000L
    }

    @Test
    fun `portion weights cover categorical numeric and unknown values`() {
        assertEquals(0.8, RefluxAnalyticsService.portionWeight("Klein"), 1e-9)
        assertEquals(1.0, RefluxAnalyticsService.portionWeight("Normal"), 1e-9)
        assertEquals(1.0, RefluxAnalyticsService.portionWeight(""), 1e-9)
        assertEquals(1.0, RefluxAnalyticsService.portionWeight("Unbekannt"), 1e-9)
        assertEquals(1.25, RefluxAnalyticsService.portionWeight("Groß"), 1e-9)
        assertEquals(1.25, RefluxAnalyticsService.portionWeight("gross"), 1e-9)
        assertEquals(2.5, RefluxAnalyticsService.portionWeight("2,5"), 1e-9)
        assertEquals(1.0, RefluxAnalyticsService.portionWeight("-2"), 1e-9)
    }

    @Test
    fun `matches symptoms from thirty minutes through four hours only`() {
        val mealTime = NOW - 2 * DAY
        val meals = listOf(meal(id = 1, timestamp = mealTime, foods = listOf("Kaffee")))
        val symptoms = listOf(
            symptom(id = 1, timestamp = mealTime + 29 * MINUTE, intensity = 3),
            symptom(id = 2, timestamp = mealTime + 30 * MINUTE, intensity = 4),
            symptom(id = 3, timestamp = mealTime + 4 * HOUR, intensity = 6),
            symptom(id = 4, timestamp = mealTime + 4 * HOUR + MINUTE, intensity = 8)
        )

        val summary = summary(meals = meals, symptoms = symptoms)
        val coffee = summary.triggers.single { it.itemName == "Kaffee" }

        assertEquals(1, coffee.consumptionCount)
        assertEquals(1, coffee.symptomMealCount)
        assertEquals(2, coffee.matchingSymptomCount)
        assertEquals(30.0, coffee.averageLatencyMinutes!!, 1e-9)
        assertEquals(5.0, coffee.averageSymptomSeverity!!, 1e-9)
        assertEquals(1.0, coffee.triggerProbability, 1e-9)
        assertEquals(4, summary.symptomCount)
    }

    @Test
    fun `portion weight changes the weighted probability`() {
        val firstMealTime = NOW - 2 * DAY
        val secondMealTime = firstMealTime + DAY

        val smallSymptomatic = summary(
            meals = listOf(
                meal(1, firstMealTime, listOf("Pizza"), portion = "Klein"),
                meal(2, secondMealTime, listOf("Pizza"), portion = "Groß")
            ),
            symptoms = listOf(symptom(1, firstMealTime + HOUR, intensity = 5))
        ).triggers.single { it.itemName == "Pizza" }

        val largeSymptomatic = summary(
            meals = listOf(
                meal(1, firstMealTime, listOf("Pizza"), portion = "Groß"),
                meal(2, secondMealTime, listOf("Pizza"), portion = "Klein")
            ),
            symptoms = listOf(symptom(1, firstMealTime + HOUR, intensity = 5))
        ).triggers.single { it.itemName == "Pizza" }

        assertEquals(0.5, smallSymptomatic.rawTriggerProbability, 1e-9)
        assertEquals(0.5, largeSymptomatic.rawTriggerProbability, 1e-9)
        assertEquals(0.8 / 2.05, smallSymptomatic.triggerProbability, 1e-9)
        assertEquals(1.25 / 2.05, largeSymptomatic.triggerProbability, 1e-9)
        assertTrue(smallSymptomatic.triggerProbability < 0.5)
        assertTrue(largeSymptomatic.triggerProbability > 0.5)
        assertTrue(largeSymptomatic.triggerProbability > smallSymptomatic.triggerProbability)
    }

    @Test
    fun `average latency uses the first symptom but average severity uses all matches`() {
        val mealTime = NOW - 2 * DAY
        val meals = listOf(meal(1, mealTime, listOf("Kaffee")))
        val symptoms = listOf(
            symptom(1, mealTime + 45 * MINUTE, intensity = 4),
            symptom(2, mealTime + 2 * HOUR, intensity = 6)
        )

        val coffee = summary(meals, symptoms)
            .triggers.single { it.itemName == "Kaffee" }

        assertEquals(1, coffee.symptomMealCount)
        assertEquals(45.0, coffee.averageLatencyMinutes!!, 1e-9)
        assertEquals(5.0, coffee.averageSymptomSeverity!!, 1e-9)
    }

    @Test
    fun `overlapping meal windows attribute a symptom to each food`() {
        val firstMealTime = NOW - 2 * DAY
        val secondMealTime = firstMealTime + HOUR
        val meals = listOf(
            meal(1, firstMealTime, listOf("A")),
            meal(2, secondMealTime, listOf("B"))
        )
        val symptoms = listOf(symptom(1, firstMealTime + 2 * HOUR, intensity = 5))

        val byName = summary(meals, symptoms).triggers.associateBy { it.itemName }

        assertEquals(1, byName.getValue("A").symptomMealCount)
        assertEquals(1, byName.getValue("B").symptomMealCount)
        assertEquals(60.0, byName.getValue("B").averageLatencyMinutes!!, 1e-9)
    }

    @Test
    fun `meal notes are used as text trigger when no food is selected`() {
        val mealTime = NOW - 2 * DAY
        val meals = listOf(
            meal(id = 1, timestamp = mealTime, foods = emptyList(), notes = "  Pizza  ")
        )

        val trigger = summary(
            meals = meals,
            symptoms = listOf(symptom(1, mealTime + HOUR, intensity = 5))
        ).triggers.single()

        assertEquals("Pizza", trigger.itemName)
        assertEquals(1, trigger.symptomMealCount)
    }

    @Test
    fun `entries outside the rolling window are excluded`() {
        val oldMealTime = NOW - 8 * DAY
        val recentMealTime = NOW - 6 * DAY
        val meals = listOf(
            meal(1, oldMealTime, listOf("Alt")),
            meal(2, recentMealTime, listOf("Neu"))
        )
        val symptoms = listOf(
            symptom(1, oldMealTime + HOUR, intensity = 8),
            symptom(2, recentMealTime + HOUR, intensity = 4)
        )

        val summary = summary(meals = meals, symptoms = symptoms, days = 7)

        assertEquals(1, summary.mealCount)
        assertEquals(1, summary.symptomCount)
        assertEquals("Neu", summary.triggers.single().itemName)
        assertEquals(NOW - 7 * DAY, summary.periodStartMillis)
    }

    @Test
    fun `activities are counted per symptom and durations use available values`() {
        val baseTime = NOW - 2 * DAY
        val symptoms = listOf(
            symptom(
                id = 1,
                timestamp = baseTime + HOUR,
                intensity = 8,
                durationMinutes = 60,
                activities = listOf("Liegen")
            ),
            symptom(
                id = 2,
                timestamp = baseTime + 2 * HOUR,
                intensity = 6,
                durationMinutes = 30,
                activities = listOf("Liegen", "Sitzen")
            ),
            symptom(
                id = 3,
                timestamp = baseTime + 3 * HOUR,
                intensity = 4,
                durationMinutes = null,
                activities = listOf("Sitzen")
            )
        )

        val summary = summary(symptoms = symptoms)
        val lying = summary.activities.single { it.activityName == "Liegen" }
        val sitting = summary.activities.single { it.activityName == "Sitzen" }

        assertEquals(2, lying.symptomCount)
        assertEquals(2.0 / 3.0, lying.shareOfAllSymptoms, 1e-9)
        assertEquals(2.0 / 3.0, lying.shareOfSymptomsWithActivity, 1e-9)
        assertEquals(7.0, lying.averageSeverity, 1e-9)
        assertEquals(45.0, lying.averageDurationMinutes!!, 1e-9)
        assertEquals(2, lying.durationSampleCount)

        assertEquals(2, sitting.symptomCount)
        assertEquals(5.0, sitting.averageSeverity, 1e-9)
        assertEquals(30.0, sitting.averageDurationMinutes!!, 1e-9)
        assertEquals(1, sitting.durationSampleCount)

        assertEquals("Liegen", summary.topActivities.first().activityName)
    }

    @Test
    fun `measures are compared with the no-measure baseline`() {
        val baseTime = NOW - 2 * DAY
        val symptoms = listOf(
            symptom(1, baseTime + HOUR, intensity = 8, durationMinutes = 120),
            symptom(2, baseTime + 2 * HOUR, intensity = 6, durationMinutes = 80),
            symptom(
                3,
                baseTime + 3 * HOUR,
                intensity = 4,
                durationMinutes = 60,
                remedies = listOf("Wasser")
            ),
            symptom(
                4,
                baseTime + 4 * HOUR,
                intensity = 2,
                durationMinutes = 40,
                remedies = listOf("Wasser")
            ),
            symptom(
                5,
                baseTime + 5 * HOUR,
                intensity = 5,
                durationMinutes = 90,
                remedies = listOf("Medikament")
            )
        )

        val summary = summary(symptoms = symptoms)
        val water = summary.measures.single { it.measureName == "Wasser" }

        assertEquals(2, water.withMeasureCount)
        assertEquals(3.0, water.withAverageSeverity!!, 1e-9)
        assertEquals(50.0, water.withAverageDurationMinutes!!, 1e-9)
        assertEquals(2, water.withoutMeasureCount)
        assertEquals(7.0, water.withoutAverageSeverity!!, 1e-9)
        assertEquals(100.0, water.withoutAverageDurationMinutes!!, 1e-9)
        assertEquals(-4.0, water.severityDelta!!, 1e-9)
        assertEquals(-50.0, water.durationDeltaMinutes!!, 1e-9)
        assertEquals(44.0, water.effectScore!!, 1e-9)
        assertFalse(water.isReliable)
        assertEquals("Wasser", summary.topEffectiveMeasures.first().measureName)
    }

    @Test
    fun `reliable measures are preferred for the top list`() {
        val baseTime = NOW - 2 * DAY
        val symptoms = mutableListOf<SymptomWithDetails>()

        // Three untreated symptoms: severe and long.
        repeat(3) { index ->
            symptoms += symptom(
                id = 10L + index,
                timestamp = baseTime + (index + 1) * HOUR,
                intensity = 9,
                durationMinutes = 120
            )
        }
        // Three treated symptoms: clear improvement.
        repeat(3) { index ->
            symptoms += symptom(
                id = 20L + index,
                timestamp = baseTime + (index + 4) * HOUR,
                intensity = 4,
                durationMinutes = 60,
                remedies = listOf("Wasser")
            )
        }
        // A single dramatic response is not considered reliable.
        symptoms += symptom(
            id = 30,
            timestamp = baseTime + 8 * HOUR,
            intensity = 1,
            durationMinutes = 10,
            remedies = listOf("Wunder")
        )

        val summary = summary(symptoms = symptoms)

        val water = summary.measures.single { it.measureName == "Wasser" }
        val miracle = summary.measures.single { it.measureName == "Wunder" }
        assertTrue(water.isReliable)
        assertFalse(miracle.isReliable)
        assertTrue(miracle.effectScore!! > water.effectScore!!)
        assertEquals(listOf("Wasser"), summary.topEffectiveMeasures.map { it.measureName })
    }

    @Test
    fun `no baseline produces null measure effects and no top measures`() {
        val baseTime = NOW - 2 * DAY
        val symptoms = listOf(
            symptom(
                1,
                baseTime + HOUR,
                intensity = 3,
                durationMinutes = 30,
                remedies = listOf("Wasser")
            )
        )

        val summary = summary(symptoms = symptoms)

        assertNull(summary.measures.single().effectScore)
        assertTrue(summary.topEffectiveMeasures.isEmpty())
    }

    @Test
    fun `daily average severity groups local days and omits empty days`() {
        val dayOne = NOW - 2 * DAY
        val dayTwo = NOW - DAY
        val symptoms = listOf(
            symptom(1, dayOne + HOUR, intensity = 4),
            symptom(2, dayOne + 2 * HOUR, intensity = 8),
            symptom(3, dayTwo + HOUR, intensity = 6)
        )

        val daily = summary(symptoms = symptoms).dailyAverageSeverity

        assertEquals(2, daily.size)
        assertEquals(2, daily[0].symptomCount)
        assertEquals(6.0, daily[0].averageSeverity, 1e-9)
        assertEquals(1, daily[1].symptomCount)
        assertEquals(6.0, daily[1].averageSeverity, 1e-9)
    }

    @Test
    fun `top three triggers are ranked by confidence adjusted score`() {
        val meals = mutableListOf<MealWithFoods>()
        val symptoms = mutableListOf<SymptomWithDetails>()

        addFoodSeries(meals, symptoms, "A", startDay = 40, consumptionCount = 10, symptomCount = 10, idBase = 1_000)
        addFoodSeries(meals, symptoms, "B", startDay = 30, consumptionCount = 10, symptomCount = 7, idBase = 2_000)
        addFoodSeries(meals, symptoms, "C", startDay = 20, consumptionCount = 10, symptomCount = 4, idBase = 3_000)
        addFoodSeries(meals, symptoms, "D", startDay = 10, consumptionCount = 10, symptomCount = 1, idBase = 4_000)

        val summary = summary(meals = meals, symptoms = symptoms, days = 365)

        assertEquals(listOf("A", "B", "C"), summary.topTriggers.map { it.itemName })
        assertEquals(100, summary.triggers.single { it.itemName == "A" }.score)
        assertEquals(70, summary.triggers.single { it.itemName == "B" }.score)
        assertEquals(40, summary.triggers.single { it.itemName == "C" }.score)
        assertEquals(10, summary.triggers.single { it.itemName == "D" }.score)
    }

    @Test
    fun `empty data returns a valid empty summary`() {
        val summary = summary()

        assertEquals(7, summary.timeWindowDays)
        assertEquals(0, summary.mealCount)
        assertEquals(0, summary.symptomCount)
        assertNull(summary.averageSymptomSeverity)
        assertNull(summary.averageSymptomDurationMinutes)
        assertTrue(summary.dailyAverageSeverity.isEmpty())
        assertTrue(summary.triggers.isEmpty())
        assertTrue(summary.topTriggers.isEmpty())
        assertTrue(summary.activities.isEmpty())
        assertTrue(summary.topActivities.isEmpty())
        assertTrue(summary.measures.isEmpty())
        assertTrue(summary.topEffectiveMeasures.isEmpty())
        assertEquals(NOW - 7 * DAY, summary.periodStartMillis)
        assertEquals(NOW, summary.periodEndMillis)
    }

    @Test
    fun `analytics window overload forwards the standard day count`() {
        val summary = RefluxAnalyticsService.getAnalyticsSummary(
            meals = emptyList(),
            symptoms = emptyList(),
            window = AnalyticsWindow.ONE_YEAR,
            nowMillis = NOW,
            timeZone = UTC
        )

        assertEquals(365, summary.timeWindowDays)
        assertEquals(NOW - 365 * DAY, summary.periodStartMillis)
    }

    private fun addFoodSeries(
        meals: MutableList<MealWithFoods>,
        symptoms: MutableList<SymptomWithDetails>,
        name: String,
        startDay: Int,
        consumptionCount: Int,
        symptomCount: Int,
        idBase: Long
    ) {
        for (index in 0 until consumptionCount) {
            val timestamp = NOW - (startDay + index).toLong() * DAY
            meals += meal(id = idBase + index, timestamp = timestamp, foods = listOf(name))
            if (index < symptomCount) {
                symptoms += symptom(
                    id = idBase * 10 + index,
                    timestamp = timestamp + HOUR,
                    intensity = 5
                )
            }
        }
    }

    private fun meal(
        id: Long,
        timestamp: Long,
        foods: List<String>,
        portion: String = "Normal",
        notes: String = ""
    ): MealWithFoods = MealWithFoods(
        meal = MealEntity(
            id = id,
            timestamp = timestamp,
            mealType = MealTypes.MITTAGESSEN,
            portion = portion,
            notes = notes
        ),
        foods = foods.mapIndexed { index, name ->
            FoodEntity(
                id = id * 100 + index,
                name = name,
                category = "Test"
            )
        }
    )

    private fun symptom(
        id: Long,
        timestamp: Long,
        intensity: Int,
        durationMinutes: Int? = null,
        activities: List<String> = emptyList(),
        remedies: List<String> = emptyList()
    ): SymptomWithDetails = SymptomWithDetails(
        symptom = SymptomEntity(
            id = id,
            timestamp = timestamp,
            symptomType = SymptomTypes.SODBRENNEN,
            intensity = intensity,
            durationMinutes = durationMinutes
        ),
        activities = activities.mapIndexed { index, name ->
            ActivityEntity(id = id * 100 + index, name = name)
        },
        remedies = remedies.mapIndexed { index, name ->
            RemedyEntity(id = id * 100 + index, name = name)
        }
    )

    private fun summary(
        meals: List<MealWithFoods> = emptyList(),
        symptoms: List<SymptomWithDetails> = emptyList(),
        days: Int = 7
    ): AnalyticsSummary = RefluxAnalyticsService.getAnalyticsSummary(
        meals = meals,
        symptoms = symptoms,
        timeWindowDays = days,
        nowMillis = NOW,
        timeZone = UTC
    )
}
