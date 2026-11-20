package com.example.data.local

import com.example.data.model.ActivityEntity
import com.example.data.model.RemedyEntity

/**
 * Standard catalogues offered as multi-select chips on the symptom screen.
 * Users can add their own entries; those are stored with `isCustom = true`.
 */
object InitialCatalogData {
    val ACTIVITIES: List<ActivityEntity> = listOf(
        "Liegen",
        "Sitzen",
        "Stehen",
        "Sport",
        "Bücken",
        "Schlafen",
        "Gehen",
        "direkt nach Essen"
    ).map { ActivityEntity(name = it, isCustom = false) }

    val REMEDIES: List<RemedyEntity> = listOf(
        "Wasser",
        "aufrecht",
        "aufgestanden",
        "gegessen",
        "Lebensmittel vermieden",
        "Medikament",
        "Spaziergang",
        "Schlafposition",
        "sonstige"
    ).map { RemedyEntity(name = it, isCustom = false) }
}
