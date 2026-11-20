package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCategoriesTest {

    @Test
    fun `encode keeps catalogue order, removes duplicates and unknown keys`() {
        val encoded = FoodCategories.encode(
            listOf(
                FoodCategories.TOMATO,
                "unknown_key",
                FoodCategories.FAT,
                FoodCategories.TOMATO,
                "  "
            )
        )
        assertEquals("fat,tomato", encoded)
    }

    @Test
    fun `decode tolerates blanks and legacy empty values`() {
        assertEquals(emptyList<String>(), FoodCategories.decode(""))
        assertEquals(
            listOf(FoodCategories.CAFFEINE, FoodCategories.CARBONATED),
            FoodCategories.decode("carbonated,caffeine")
        )
    }

    @Test
    fun `labels are display-only and fall back to the key`() {
        assertEquals(
            listOf("Fettig", "Große Portion"),
            FoodCategories.labels(listOf(FoodCategories.LARGE_PORTION, FoodCategories.FAT))
        )
        assertEquals("legacy", FoodCategories.label("legacy"))
    }

    @Test
    fun `large portion category is part of the catalogue`() {
        assertTrue(FoodCategories.ALL.any { it.key == FoodCategories.LARGE_PORTION })
        assertEquals(11, FoodCategories.ALL.size)
        assertFalse(FoodCategories.ALL.map { it.key }.contains("stress"))
    }
}
