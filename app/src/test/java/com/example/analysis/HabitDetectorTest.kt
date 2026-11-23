package com.example.analysis

import com.example.data.model.FoodEntity
import com.example.data.model.HabitOverrideEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealTypes
import com.example.data.model.MealWithFoods
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HabitDetectorTest {

    private lateinit var previousZone: TimeZone

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    @Test
    fun `detects a breakfast repeated on three different days`() {
        val meals = listOf(
            breakfast(id = 1, day = 10, hour = 7, minute = 50, foods = listOf("Brot", "Käse", "Kaffee")),
            breakfast(id = 2, day = 11, hour = 8, minute = 10, foods = listOf("Brot", "Käse", "Kaffee")),
            breakfast(id = 3, day = 12, hour = 8, minute = 0, foods = listOf("Brot", "Käse", "Kaffee"))
        )

        val habit = HabitDetector.detect(meals).single()

        assertEquals(3, habit.occurrenceCount)
        assertEquals(3, habit.distinctDayCount)
        assertEquals(MealTypes.FRUEHSTUECK, habit.mealType)
        assertEquals(setOf("Brot", "Käse", "Kaffee"), habit.foodNames.toSet())
        assertEquals("Häufiges Frühstück", habit.name)
        assertTrue(habit.typicalMinutesOfDay in 479..481)
    }

    @Test
    fun `needs at least three occurrences on three days`() {
        val twoDays = listOf(
            breakfast(1, 10, 8, 0, listOf("Brot", "Käse")),
            breakfast(2, 11, 8, 0, listOf("Brot", "Käse"))
        )
        assertTrue(HabitDetector.detect(twoDays).isEmpty())

        val twiceOnOneDay = listOf(
            breakfast(1, 10, 8, 0, listOf("Brot", "Käse")),
            breakfast(2, 10, 9, 0, listOf("Brot", "Käse")),
            breakfast(3, 10, 10, 0, listOf("Brot", "Käse"))
        )
        assertTrue(HabitDetector.detect(twiceOnOneDay).isEmpty())
    }

    @Test
    fun `tolerates small deviations in foods and time`() {
        val meals = listOf(
            breakfast(1, 10, 7, 45, listOf("Brot", "Käse")),
            breakfast(2, 11, 8, 30, listOf("Brot", "Käse", "Kaffee")),
            breakfast(3, 12, 9, 0, listOf("Brot", "Käse"))
        )

        val habit = HabitDetector.detect(meals).single()

        assertEquals(3, habit.occurrenceCount)
        // Only foods that occur in at least 60 % of the cluster's meals.
        assertEquals(setOf("Brot", "Käse"), habit.foodNames.toSet())
    }

    @Test
    fun `meals far apart in time or with unrelated foods are not grouped`() {
        val differentTimes = listOf(
            breakfast(1, 10, 6, 0, listOf("Brot", "Käse")),
            breakfast(2, 11, 13, 0, listOf("Brot", "Käse")),
            breakfast(3, 12, 20, 0, listOf("Brot", "Käse"))
        )
        assertTrue(HabitDetector.detect(differentTimes).isEmpty())

        val unrelatedFoods = listOf(
            breakfast(1, 10, 8, 0, listOf("Brot", "Käse", "Apfel", "Banane")),
            breakfast(2, 11, 8, 0, listOf("Pizza", "Salat", "Wasser")),
            breakfast(3, 12, 8, 0, listOf("Reis", "Fisch", "Tee"))
        )
        assertTrue(HabitDetector.detect(unrelatedFoods).isEmpty())
    }

    @Test
    fun `suggestions match meal type and skip fully selected habits`() {
        val meals = listOf(
            breakfast(1, 10, 8, 0, listOf("Brot", "Käse")),
            breakfast(2, 11, 8, 0, listOf("Brot", "Käse")),
            breakfast(3, 12, 8, 0, listOf("Brot", "Käse"))
        )
        val habits = HabitDetector.detect(meals)

        assertEquals(1, HabitDetector.suggestions(habits, MealTypes.FRUEHSTUECK).size)
        assertTrue(HabitDetector.suggestions(habits, MealTypes.ABENDESSEN).isEmpty())
        assertTrue(
            HabitDetector.suggestions(
                habits = habits,
                mealType = MealTypes.FRUEHSTUECK,
                selectedFoodNames = listOf("Brot", "Käse")
            ).isEmpty()
        )
    }

    @Test
    fun `overrides rename edit and hide a habit without touching meals`() {
        val meals = listOf(
            breakfast(1, 10, 8, 0, listOf("Brot", "Käse")),
            breakfast(2, 11, 8, 0, listOf("Brot", "Käse")),
            breakfast(3, 12, 8, 0, listOf("Brot", "Käse"))
        )
        val signature = HabitDetector.detect(meals).single().signature

        val edited = HabitDetector.detect(
            meals,
            listOf(
                HabitOverrideEntity(
                    signature = signature,
                    customName = "Mein Standardfrühstück",
                    customFoods = "Brot"
                )
            )
        ).single()

        assertEquals("Mein Standardfrühstück", edited.name)
        assertEquals(listOf("Brot"), edited.foodNames)
        assertTrue(edited.isEdited)

        val hidden = HabitDetector.detect(
            meals,
            listOf(HabitOverrideEntity(signature = signature, isDeleted = true))
        )
        assertTrue(hidden.isEmpty())

        // The source meals are unchanged; only the override hides the habit.
        assertEquals(3, meals.size)
    }

    @Test
    fun `same foods at different times produce distinct stable signatures`() {
        val meals = listOf(
            breakfast(1, 10, 6, 0, listOf("Brot", "Käse")),
            breakfast(2, 11, 6, 0, listOf("Brot", "Käse")),
            breakfast(3, 12, 6, 0, listOf("Brot", "Käse")),
            breakfast(4, 13, 20, 0, listOf("Brot", "Käse")),
            breakfast(5, 14, 20, 0, listOf("Brot", "Käse")),
            breakfast(6, 15, 20, 0, listOf("Brot", "Käse"))
        )

        val habits = HabitDetector.detect(meals)

        assertEquals(2, habits.size)
        assertEquals(2, habits.map { it.signature }.distinct().size)
        assertTrue(habits.all { it.occurrenceCount == 3 })
        assertTrue(habits.any { it.typicalMinutesOfDay in 359..361 })
        assertTrue(habits.any { it.typicalMinutesOfDay in 1199..1201 })
    }

    @Test
    fun `legacy food-only overrides still apply to time-bucketed habits`() {
        val meals = listOf(
            breakfast(1, 10, 8, 0, listOf("Brot", "Käse")),
            breakfast(2, 11, 8, 0, listOf("Brot", "Käse")),
            breakfast(3, 12, 8, 0, listOf("Brot", "Käse"))
        )
        val legacySignature = HabitDetector.signatureFor(
            MealTypes.FRUEHSTUECK,
            listOf("Brot", "Käse")
        )

        val renamed = HabitDetector.detect(
            meals,
            listOf(HabitOverrideEntity(signature = legacySignature, customName = "Alt"))
        ).single()

        assertEquals("Alt", renamed.name)

        val hidden = HabitDetector.detect(
            meals,
            listOf(HabitOverrideEntity(signature = legacySignature, isDeleted = true))
        )
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun `signature is case and umlaut insensitive`() {
        val first = HabitDetector.signatureFor("Frühstück", listOf("Käse", "Brot"))
        val second = HabitDetector.signatureFor("fruehstueck", listOf("brot", "KAESE"))
        assertEquals(first, second)
    }

    private fun breakfast(
        id: Long,
        day: Int,
        hour: Int,
        minute: Int,
        foods: List<String>
    ): MealWithFoods {
        val timestamp = timestamp(day, hour, minute)
        return MealWithFoods(
            meal = MealEntity(
                id = id,
                timestamp = timestamp,
                mealType = MealTypes.FRUEHSTUECK,
                portion = "Normal",
                notes = ""
            ),
            foods = foods.mapIndexed { index, name ->
                FoodEntity(
                    id = id * 100 + index,
                    name = name,
                    category = "Test"
                )
            }
        )
    }

    private fun timestamp(day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, day, hour, minute, 0)
        }.timeInMillis
}
