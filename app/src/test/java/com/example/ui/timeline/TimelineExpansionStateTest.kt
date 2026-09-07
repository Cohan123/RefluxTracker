package com.example.ui.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour spec for [TimelineExpansionState].
 *
 * The Timeline screen now lets the user tap a row to expand it and see
 * the full details (foods, portion, notes for a meal; intensity, duration,
 * notes for a symptom). Default state is collapsed for every row.
 *
 * The state is keyed by the entry's `timestamp` because two entries in the
 * same day could theoretically collide — using the timestamp avoids that
 * ambiguity while still being deterministic for the same row.
 */
class TimelineExpansionStateTest {

    @Test
    fun `isExpanded returns false by default for any timestamp`() {
        val state = TimelineExpansionState()
        assertFalse(state.isExpanded(1700000000000L))
        assertFalse(state.isExpanded(1700003600000L))
    }

    @Test
    fun `toggle expands a previously collapsed entry`() {
        val state = TimelineExpansionState()
        state.toggle(1700000000000L)
        assertTrue(state.isExpanded(1700000000000L))
    }

    @Test
    fun `toggle on an already-expanded entry collapses it`() {
        val state = TimelineExpansionState()
        state.toggle(1700000000000L)
        state.toggle(1700000000000L)
        assertFalse(state.isExpanded(1700000000000L))
    }

    @Test
    fun `toggle of one timestamp does not affect another`() {
        val state = TimelineExpansionState()
        state.toggle(1700000000000L)
        assertTrue(state.isExpanded(1700000000000L))
        assertFalse(state.isExpanded(1700003600000L))
    }

    @Test
    fun `collapseAll clears every expansion but leaves the state object usable`() {
        val state = TimelineExpansionState()
        state.toggle(1L)
        state.toggle(2L)
        state.toggle(3L)
        state.collapseAll()
        assertFalse(state.isExpanded(1L))
        assertFalse(state.isExpanded(2L))
        assertFalse(state.isExpanded(3L))
        // still usable:
        state.toggle(1L)
        assertTrue(state.isExpanded(1L))
    }

    @Test
    fun `expandedCount reports the number of currently-expanded rows`() {
        val state = TimelineExpansionState()
        assertEquals(0, state.expandedCount())
        state.toggle(1L)
        state.toggle(2L)
        assertEquals(2, state.expandedCount())
        state.toggle(1L)
        assertEquals(1, state.expandedCount())
    }
}
