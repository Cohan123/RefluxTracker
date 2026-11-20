package com.example.data.model

/**
 * Analyses categories that can be assigned to a food.
 *
 * The stable [key] values are persisted in the database; the German [label] is
 * only used for display. Categories are metadata for the analysis, **not**
 * independent triggers: a category is only evaluated when the user assigned it
 * to a food (or selected "large portion" at meal level).
 *
 * Assignment is deliberately manual. The default catalogue ships with sensible
 * initial values, but the analysis never derives a category from a food name.
 */
object FoodCategories {

    const val FAT = "fat"
    const val SPICY = "spicy"
    const val SOUR = "sour"
    const val TOMATO = "tomato"
    const val CITRUS = "citrus"
    const val CHOCOLATE = "chocolate"
    const val CAFFEINE = "caffeine"
    const val ALCOHOL = "alcohol"
    const val CARBONATED = "carbonated"
    const val ONION_GARLIC = "onion_garlic"
    const val LARGE_PORTION = "large_portion"

    data class Definition(val key: String, val label: String)

    val ALL: List<Definition> = listOf(
        Definition(FAT, "Fettig"),
        Definition(SPICY, "Scharf"),
        Definition(SOUR, "Sauer"),
        Definition(TOMATO, "Tomate"),
        Definition(CITRUS, "Zitrus"),
        Definition(CHOCOLATE, "Schokolade"),
        Definition(CAFFEINE, "Kaffee/Koffein"),
        Definition(ALCOHOL, "Alkohol"),
        Definition(CARBONATED, "Kohlensäure"),
        Definition(ONION_GARLIC, "Zwiebel/Knoblauch"),
        Definition(LARGE_PORTION, "Große Portion")
    )

    private val byKey: Map<String, Definition> = ALL.associateBy { it.key }

    fun label(key: String): String = byKey[key]?.label ?: key

    /** Keeps only known keys, removes duplicates and preserves catalogue order. */
    fun normalize(keys: Iterable<String>): List<String> {
        val selected = keys.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        return ALL.map { it.key }.filter { it in selected }
    }

    /** Decodes the comma-separated keys stored in `foods.categories`. */
    fun decode(stored: String): List<String> =
        normalize(stored.split(','))

    fun encode(keys: Iterable<String>): String = normalize(keys).joinToString(",")

    fun labels(keys: Iterable<String>): List<String> = normalize(keys).map(::label)
}
