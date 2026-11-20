package com.example.analysis

import com.example.data.model.FoodCategories
import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealTypes
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomTypes
import com.example.data.model.SymptomWithDetails
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the category correlations, time-of-day heatmap and daily GERD
 * burden that were added on top of the fixed-window analytics.
 */
class RefluxAnalyticsExtensionsTest {

    private companion object {
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        const val MINUTE = 60_000L
        const val HOUR = 60L * MINUTE

        /** 2026-11-19T12:00:00Z; local noon. */
        val NOW: Long = Calendar.getInstance(UTC).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 19, 12, 0, 0)
        }.timeInMillis
    }

    @Test
    fun `daily burden separates documented symptom-free and missing days`() {
        val today = day(0)
        val yesterday = day(-1)
        val fiveDaysAgo = day(-5)

        val meals = listOf(
            meal(1, yesterday + 12 * HOUR, listOf("Reis"), "Normal")
        )
        val symptoms = listOf(
            symptom(1, today + 8 * HOUR, intensity = 10),
            symptom(2, today + 9 * HOUR, intensity = 10),
            symptom(3, today + 10 * HOUR, intensity = 10),
            symptom(4, today + 11 * HOUR, intensity = 10),
            symptom(5, fiveDaysAgo + 10 * HOUR, intensity = 5)
        )

        val summary = summary(meals, symptoms, days = 7)
        val byDay = summary.dailyBurden.associateBy { it.dayStartMillis }

        assertEquals(7, summary.dailyBurden.size)

        val todayBucket = byDay.getValue(day(0))
        assertEquals(100.0, todayBucket.burden!!, 1e-9)
        assertTrue(todayBucket.isSpike)

        val freeDay = byDay.getValue(day(-1))
        assertEquals(0.0, freeDay.burden!!, 1e-9)
        assertTrue(freeDay.isSymptomFree)
        assertFalse(freeDay.isMissing)

        val midDay = byDay.getValue(day(-5))
        assertEquals(40.0, midDay.burden!!, 1e-9)
        // 60 % of (5/10) + 40 % of (1/4).
        assertFalse(midDay.isSpike)

        val missing = byDay.getValue(day(-2))
        assertNull(missing.burden)
        assertTrue(missing.isMissing)
        assertFalse(missing.isSymptomFree)

        assertEquals(4, summary.trend.missingDayCount)
        assertEquals(1, summary.trend.symptomFreeDayCount)
        assertEquals(3, summary.trend.documentedDayCount)
        assertEquals(40.0, summary.trend.firstHalfAverageBurden!!, 1e-9)
        assertEquals(50.0, summary.trend.secondHalfAverageBurden!!, 1e-9)
        assertEquals(10.0, summary.trend.changeAbsolute!!, 1e-9)
        assertEquals(25.0, summary.trend.changePercent!!, 1e-9)
        assertEquals(listOf(day(0)), summary.trend.spikeDayStarts)
    }

    @Test
    fun `missing days never pull the trend towards zero`() {
        val meals = listOf(
            meal(1, day(-6) + 12 * HOUR, listOf("Reis")),
            meal(2, day(-2) + 12 * HOUR, listOf("Reis"))
        )
        val symptoms = listOf(
            symptom(1, day(-6) + 13 * HOUR, intensity = 6),
            symptom(2, day(-2) + 13 * HOUR, intensity = 6)
        )

        val trend = summary(meals, symptoms, days = 7).trend

        assertEquals(2, trend.documentedDayCount)
        assertEquals(5, trend.missingDayCount)
        assertNull("fewer than three documented days must not produce a slope", trend.slopePerDay)
        assertNull(trend.fittedStartBurden)
        assertNull(trend.fittedEndBurden)
    }

    @Test
    fun `time of day heatmap uses strong symptoms and names the peak window`() {
        val symptoms = listOf(
            symptom(1, day(-3) + 22 * HOUR, intensity = 8),
            symptom(2, day(-2) + 23 * HOUR, intensity = 7),
            symptom(3, day(-1) + 0 * HOUR + 30 * MINUTE, intensity = 9),
            symptom(4, day(-1) + 1 * HOUR, intensity = 6),
            symptom(5, day(-2) + 12 * HOUR, intensity = 3)
        )

        val summary = summary(emptyList(), symptoms, days = 7)
        val timeOfDay = summary.timeOfDay

        assertEquals(24, timeOfDay.buckets.size)
        assertEquals(4, timeOfDay.strongSymptomCount)
        assertEquals(21, timeOfDay.peakStartHour)
        assertEquals(2, timeOfDay.peakEndHourExclusive)
        assertEquals(4, timeOfDay.strongSymptomsInPeak)
        assertEquals(1.0, timeOfDay.strongShareInPeak, 1e-9)
        assertNotNull(timeOfDay.insight)
        assertTrue(timeOfDay.insight!!.contains("21:00"))
        assertTrue(timeOfDay.insight!!.contains("100 %"))

        assertEquals(1, timeOfDay.buckets[22].symptomCount)
        assertEquals(8.0, timeOfDay.buckets[22].averageIntensity!!, 1e-9)
        assertEquals(0, timeOfDay.buckets[5].symptomCount)
        assertNull(timeOfDay.buckets[5].averageIntensity)
    }

    @Test
    fun `trend slope uses calendar-day distance across gaps`() {
        val symptoms = listOf(
            symptom(1, day(-6) + 10 * HOUR, intensity = 5),
            symptom(2, day(-3) + 10 * HOUR, intensity = 10),
            symptom(6, day(-3) + 11 * HOUR, intensity = 10),
            symptom(3, day(0) + 8 * HOUR, intensity = 5),
            symptom(4, day(0) + 9 * HOUR, intensity = 5),
            symptom(5, day(0) + 10 * HOUR, intensity = 5)
        )

        val trend = summary(emptyList(), symptoms, days = 7).trend

        assertEquals(3, trend.documentedDayCount)
        assertEquals(3.333333333, trend.slopePerDay!!, 1e-6)
        assertEquals(50.0, trend.fittedStartBurden!!, 1e-6)
        assertEquals(70.0, trend.fittedEndBurden!!, 1e-6)
        assertEquals(day(-6), trend.fittedStartDayStartMillis)
        assertEquals(day(0), trend.fittedEndDayStartMillis)
        assertEquals(30.0, trend.changeAbsolute!!, 1e-9)
        assertEquals(75.0, trend.changePercent!!, 1e-9)
    }

    @Test
    fun `category association compares meals with and without the assigned category`() {
        val fattyFood = food(1, "Pizza", FoodCategories.FAT, FoodCategories.TOMATO)
        val neutralFood = food(2, "Reis")

        val meals = listOf(
            mealWithFoods(1, day(-6) + 12 * HOUR, fattyFood),
            mealWithFoods(2, day(-5) + 12 * HOUR, fattyFood),
            mealWithFoods(3, day(-4) + 12 * HOUR, fattyFood),
            mealWithFoods(4, day(-3) + 12 * HOUR, neutralFood),
            mealWithFoods(5, day(-2) + 12 * HOUR, neutralFood),
            mealWithFoods(6, day(-1) + 12 * HOUR, neutralFood)
        )
        val symptoms = listOf(
            symptom(1, day(-6) + 13 * HOUR, intensity = 5),
            symptom(2, day(-5) + 13 * HOUR, intensity = 7),
            symptom(3, day(-3) + 13 * HOUR, intensity = 4)
        )

        val summary = summary(meals, symptoms, days = 7)
        val fat = summary.factorAssociations.single { it.label == "Fettig" }

        assertEquals(FactorKind.CATEGORY, fat.kind)
        assertEquals(3, fat.exposedMealCount)
        assertEquals(3, fat.unexposedMealCount)
        assertEquals(2, fat.exposedSymptomMealCount)
        assertEquals(1, fat.unexposedSymptomMealCount)
        assertEquals(2.0 / 3.0, fat.probabilityWith, 1e-9)
        assertEquals(1.0 / 3.0, fat.probabilityWithout, 1e-9)
        assertEquals(1.0 / 3.0, fat.absoluteDifference, 1e-9)
        assertEquals(100.0, fat.relativeChangePercent!!, 1e-9)
        assertEquals(DataQuality.LOW, fat.dataQuality)
        assertTrue(fat.isReliable)

        // Tomato is evaluated separately, not merged into the fat row.
        val tomato = summary.factorAssociations.single { it.label == "Tomate" }
        assertEquals(3, tomato.exposedMealCount)
        assertEquals(3, tomato.unexposedMealCount)
    }

    @Test
    fun `large portion at meal level is a factor even without food categories`() {
        val neutralFood = food(1, "Reis")
        val meals = listOf(
            mealWithFoods(1, day(-6) + 12 * HOUR, neutralFood, portion = "Groß"),
            mealWithFoods(2, day(-5) + 12 * HOUR, neutralFood, portion = "Groß"),
            mealWithFoods(3, day(-4) + 12 * HOUR, neutralFood, portion = "Groß"),
            mealWithFoods(4, day(-3) + 12 * HOUR, neutralFood, portion = "Normal"),
            mealWithFoods(5, day(-2) + 12 * HOUR, neutralFood, portion = "Klein"),
            mealWithFoods(6, day(-1) + 12 * HOUR, neutralFood, portion = "Normal")
        )
        val symptoms = listOf(
            symptom(1, day(-6) + 13 * HOUR, intensity = 5),
            symptom(2, day(-5) + 13 * HOUR, intensity = 5),
            symptom(3, day(-4) + 13 * HOUR, intensity = 5)
        )

        val large = summary(meals, symptoms, days = 7)
            .factorAssociations
            .single { it.label == "Große Portion" }

        assertEquals(3, large.exposedMealCount)
        assertEquals(3, large.unexposedMealCount)
        assertEquals(3, large.exposedSymptomMealCount)
        assertEquals(0, large.unexposedSymptomMealCount)
        assertNull("zero baseline must not produce a misleading percentage", large.relativeChangePercent)
    }

    @Test
    fun `insight text is only emitted when data supports it`() {
        val summary = summary(emptyList(), emptyList(), days = 7)

        assertTrue(summary.insights.isEmpty())
        assertTrue(summary.factorAssociations.isEmpty())
        assertEquals(7, summary.dailyBurden.size)
        assertTrue(summary.dailyBurden.all { it.isMissing })
        assertEquals(0, summary.trend.documentedDayCount)
        assertNull(summary.timeOfDay.insight)
        assertEquals(0, summary.timeOfDay.strongSymptomCount)
        assertTrue(summary.unavailableFactors.isNotEmpty())
    }

    private fun summary(
        meals: List<MealWithFoods>,
        symptoms: List<SymptomWithDetails>,
        days: Int
    ): AnalyticsSummary = RefluxAnalyticsService.getAnalyticsSummary(
        meals = meals,
        symptoms = symptoms,
        timeWindowDays = days,
        nowMillis = NOW,
        timeZone = UTC
    )

    private fun meal(
        id: Long,
        timestamp: Long,
        foods: List<String>,
        portion: String = "Normal"
    ): MealWithFoods = MealWithFoods(
        meal = MealEntity(
            id = id,
            timestamp = timestamp,
            mealType = MealTypes.MITTAGESSEN,
            portion = portion,
            notes = ""
        ),
        foods = foods.mapIndexed { index, name ->
            FoodEntity(id = id * 100 + index, name = name, category = "Test")
        }
    )

    private fun mealWithFoods(
        id: Long,
        timestamp: Long,
        food: FoodEntity,
        portion: String = "Normal"
    ): MealWithFoods = MealWithFoods(
        meal = MealEntity(
            id = id,
            timestamp = timestamp,
            mealType = MealTypes.MITTAGESSEN,
            portion = portion,
            notes = ""
        ),
        foods = listOf(food.copy(id = id * 100 + food.id))
    )

    private fun food(id: Long, name: String, vararg categories: String): FoodEntity =
        FoodEntity(
            id = id,
            name = name,
            category = "Test",
            categories = FoodCategories.encode(categories.toList())
        )

    private fun symptom(
        id: Long,
        timestamp: Long,
        intensity: Int
    ): SymptomWithDetails = SymptomWithDetails(
        symptom = SymptomEntity(
            id = id,
            timestamp = timestamp,
            symptomType = SymptomTypes.SODBRENNEN,
            intensity = intensity
        ),
        activities = emptyList(),
        remedies = emptyList()
    )

    /** Local midnight [offset] days from today. */
    private fun day(offset: Int): Long = Calendar.getInstance(UTC).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 19, 0, 0, 0)
        add(Calendar.DAY_OF_YEAR, offset)
    }.timeInMillis
}
