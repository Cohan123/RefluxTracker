package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SymptomTypesJsonTest {

    @Test
    fun `encode trims, removes blanks and duplicates`() {
        val json = SymptomTypesJson.encode(listOf(" Sodbrennen ", "Aufstoßen", "Sodbrennen", "  "))
        assertEquals("[\"Sodbrennen\",\"Aufstoßen\"]", json)
    }

    @Test
    fun `encode and decode keep quotes and backslashes intact`() {
        val types = listOf("Test \"quoted\"", "back\\slash")
        assertEquals(types, SymptomTypesJson.decode(SymptomTypesJson.encode(types)))
    }

    @Test
    fun `decode falls back to primary type for empty or corrupt values`() {
        assertEquals(listOf("Husten"), SymptomTypesJson.decodeOrPrimary("[]", "Husten"))
        assertEquals(listOf("Husten"), SymptomTypesJson.decodeOrPrimary("not json", "Husten"))
        assertTrue(SymptomTypesJson.decodeOrPrimary("[]", "").isEmpty())
    }

    @Test
    fun `decode ignores non-string and blank array entries`() {
        assertEquals(
            listOf("Husten"),
            SymptomTypesJson.decode("[\"Husten\", \"\", null]")
        )
    }
}
