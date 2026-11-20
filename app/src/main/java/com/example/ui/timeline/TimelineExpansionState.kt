package com.example.ui.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember

/**
 * Tracks which timeline rows are currently expanded.
 *
 * Rows are keyed by their `timestamp` (Long) — for the same day there can be
 * many rows, but two rows in the same day have distinct timestamps, so this
 * avoids any key-collision risk while staying trivial to reason about.
 *
 * Default state for every row is collapsed (the user only sees the summary
 * line and taps to reveal the details).
 *
 * The backing map is a Compose snapshot state map, so mutating it triggers
 * recomposition of the timeline rows. Use [rememberTimelineExpansionState] in
 * a Composable to get a remembered instance; the class itself is still
 * unit-testable without any Compose UI machinery.
 */
class TimelineExpansionState {
    private val expanded = mutableStateMapOf<Long, Unit>()

    fun isExpanded(timestamp: Long): Boolean = expanded.containsKey(timestamp)

    fun toggle(timestamp: Long) {
        if (expanded.containsKey(timestamp)) {
            expanded.remove(timestamp)
        } else {
            expanded[timestamp] = Unit
        }
    }

    fun collapseAll() {
        expanded.clear()
    }

    fun expandedCount(): Int = expanded.size
}

/**
 * Composable helper that remembers a [TimelineExpansionState] across
 * recompositions. Use inside [TimelineScreen] (or any Composable that needs
 * to know which rows are expanded).
 */
@Composable
fun rememberTimelineExpansionState(): TimelineExpansionState {
    return remember { TimelineExpansionState() }
}
