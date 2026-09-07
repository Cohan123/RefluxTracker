package com.example.ui.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap

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
 * Use [rememberTimelineExpansionState] in a Composable to get a remembered
 * instance; the pure-Kotlin [TimelineExpansionState] class is unit-testable
 * without any Compose machinery.
 */
class TimelineExpansionState {
    private val expanded: MutableSet<Long> = mutableSetOf()

    fun isExpanded(timestamp: Long): Boolean = timestamp in expanded

    fun toggle(timestamp: Long) {
        if (timestamp in expanded) expanded.remove(timestamp)
        else expanded.add(timestamp)
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
