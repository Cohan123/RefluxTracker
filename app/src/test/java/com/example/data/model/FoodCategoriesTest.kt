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
        assertEquals(18, FoodCategories.ALL.size)
        assertFalse(FoodCategories.ALL.map { it.key }.contains("stress"))
    }

    @Test
    fun `new categories are selectable, encodable and labelled`() {
        val expected = mapOf(
            FoodCategories.SWEET to "Süß",
            FoodCategories.SAVORY to "Deftig",
            FoodCategories.FRIED to "Frittiert",
            FoodCategories.STRONG_SPICY to "Stark würzig",
            FoodCategories.BLACK_GREEN_TEA to "Schwarz-/Grüntee",
            FoodCategories.DAIRY to "Milchprodukt",
            FoodCategories.VERY_HOT to "Sehr heiß"
        )
        for ((key, label) in expected) {
            assertTrue("missing category $key", FoodCategories.ALL.any { it.key == key })
            assertEquals(label, FoodCategories.label(key))
        }

        val encoded = FoodCategories.encode(
            listOf(FoodCategories.VERY_HOT, FoodCategories.SWEET, FoodCategories.DAIRY)
        )
        assertEquals("sweet,dairy,very_hot", encoded)
        assertEquals(
            listOf("Süß", "Milchprodukt", "Sehr heiß"),
            FoodCategories.labels(FoodCategories.decode(encoded))
        )
    }

    @Test
    fun `existing category keys stay stable`() {
        assertEquals("fat", FoodCategories.FAT)
        assertEquals("large_portion", FoodCategories.LARGE_PORTION)
        assertEquals(
            listOf(FoodCategories.FAT, FoodCategories.LARGE_PORTION),
            FoodCategories.decode("large_portion,fat")
        )
    }
}
