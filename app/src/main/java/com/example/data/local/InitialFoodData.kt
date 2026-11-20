package com.example.data.local

import com.example.data.model.FoodCategories
import com.example.data.model.FoodEntity

object InitialFoodData {

    /**
     * Initial catalogue.
     *
     * The category assignment is *initial metadata* for well-known foods (for
     * example coffee contains caffeine). It is not inferred from the name at
     * analysis time, and the user can change every assignment.
     */
    private fun cats(vararg keys: String): String = keys.joinToString(",")

    val PREPOPULATED_FOODS = listOf(
        // Getränke
        FoodEntity(
            name = "Kaffee",
            category = "Getränke",
            iconName = "local_cafe",
            categories = cats(FoodCategories.CAFFEINE)
        ),
        FoodEntity(
            name = "Espresso",
            category = "Getränke",
            iconName = "local_cafe",
            categories = cats(FoodCategories.CAFFEINE)
        ),
        FoodEntity(
            name = "Tee (Schwarz/Grün)",
            category = "Getränke",
            iconName = "emoji_food_beverage",
            categories = cats(FoodCategories.CAFFEINE)
        ),
        FoodEntity(name = "Kräutertee (z.B. Kamille)", category = "Getränke", iconName = "emoji_food_beverage"),
        FoodEntity(
            name = "Cola",
            category = "Getränke",
            iconName = "local_drink",
            categories = cats(FoodCategories.CAFFEINE, FoodCategories.CARBONATED)
        ),
        FoodEntity(
            name = "Energy Drinks",
            category = "Getränke",
            iconName = "bolt",
            categories = cats(FoodCategories.CAFFEINE, FoodCategories.CARBONATED)
        ),
        FoodEntity(
            name = "Alkohol",
            category = "Getränke",
            iconName = "wine_bar",
            categories = cats(FoodCategories.ALCOHOL)
        ),
        FoodEntity(
            name = "Bier",
            category = "Getränke",
            iconName = "sports_bar",
            categories = cats(FoodCategories.ALCOHOL, FoodCategories.CARBONATED)
        ),
        FoodEntity(
            name = "Wein",
            category = "Getränke",
            iconName = "wine_bar",
            categories = cats(FoodCategories.ALCOHOL)
        ),
        FoodEntity(
            name = "Mineralwasser (mit Kohlensäure)",
            category = "Getränke",
            iconName = "water_drop",
            categories = cats(FoodCategories.CARBONATED)
        ),
        FoodEntity(name = "Stilles Wasser", category = "Getränke", iconName = "water_drop"),

        // Obst & Gemüse
        FoodEntity(
            name = "Tomaten",
            category = "Obst & Gemüse",
            iconName = "restaurant",
            categories = cats(FoodCategories.TOMATO)
        ),
        FoodEntity(
            name = "Zitrusfrüchte",
            category = "Obst & Gemüse",
            iconName = "eco",
            categories = cats(FoodCategories.CITRUS, FoodCategories.SOUR)
        ),
        FoodEntity(
            name = "Orange",
            category = "Obst & Gemüse",
            iconName = "eco",
            categories = cats(FoodCategories.CITRUS, FoodCategories.SOUR)
        ),
        FoodEntity(
            name = "Zitrone",
            category = "Obst & Gemüse",
            iconName = "eco",
            categories = cats(FoodCategories.CITRUS, FoodCategories.SOUR)
        ),
        FoodEntity(
            name = "Grapefruit",
            category = "Obst & Gemüse",
            iconName = "eco",
            categories = cats(FoodCategories.CITRUS, FoodCategories.SOUR)
        ),
        FoodEntity(
            name = "Zwiebeln",
            category = "Obst & Gemüse",
            iconName = "restaurant",
            categories = cats(FoodCategories.ONION_GARLIC)
        ),
        FoodEntity(
            name = "Knoblauch",
            category = "Obst & Gemüse",
            iconName = "restaurant",
            categories = cats(FoodCategories.ONION_GARLIC)
        ),
        FoodEntity(name = "Minze / Pfefferminze", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Banane", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Apfel", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Gurke", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Kartoffeln", category = "Obst & Gemüse", iconName = "restaurant"),

        // Mahlzeiten & Zubereitung
        FoodEntity(
            name = "Pizza",
            category = "Hauptspeisen",
            iconName = "local_pizza",
            categories = cats(FoodCategories.FAT, FoodCategories.TOMATO)
        ),
        FoodEntity(
            name = "Fettreiche Lebensmittel",
            category = "Hauptspeisen",
            iconName = "lunch_dining",
            categories = cats(FoodCategories.FAT)
        ),
        FoodEntity(
            name = "Scharfes Essen",
            category = "Hauptspeisen",
            iconName = "whatshot",
            categories = cats(FoodCategories.SPICY)
        ),
        FoodEntity(
            name = "Burger / Fast Food",
            category = "Hauptspeisen",
            iconName = "fastfood",
            categories = cats(FoodCategories.FAT)
        ),
        FoodEntity(
            name = "Pasta mit Tomatensauce",
            category = "Hauptspeisen",
            iconName = "dinner_dining",
            categories = cats(FoodCategories.TOMATO, FoodCategories.SOUR)
        ),
        FoodEntity(name = "Reis", category = "Hauptspeisen", iconName = "rice_bowl"),
        FoodEntity(name = "Hähnchen / Geflügel", category = "Hauptspeisen", iconName = "restaurant"),
        FoodEntity(name = "Fisch", category = "Hauptspeisen", iconName = "set_meal"),

        // Süßes & Snacks
        FoodEntity(
            name = "Schokolade",
            category = "Süßwaren & Snacks",
            iconName = "cookie",
            categories = cats(FoodCategories.CHOCOLATE)
        ),
        FoodEntity(
            name = "Gebäck / Kuchen",
            category = "Süßwaren & Snacks",
            iconName = "cake",
            categories = cats(FoodCategories.FAT)
        ),
        FoodEntity(
            name = "Chips / Salziges",
            category = "Süßwaren & Snacks",
            iconName = "fastfood",
            categories = cats(FoodCategories.FAT)
        ),

        // Milchprodukte & Getreide
        FoodEntity(
            name = "Milchprodukte (Vollmilch/Sahne)",
            category = "Milch & Getreide",
            iconName = "egg_alt",
            categories = cats(FoodCategories.FAT)
        ),
        FoodEntity(
            name = "Käse",
            category = "Milch & Getreide",
            iconName = "restaurant",
            categories = cats(FoodCategories.FAT)
        ),
        FoodEntity(name = "Joghurt", category = "Milch & Getreide", iconName = "restaurant"),
        FoodEntity(name = "Brot / Toast", category = "Milch & Getreide", iconName = "bakery_dining"),
        FoodEntity(name = "Haferflocken / Porridge", category = "Milch & Getreide", iconName = "breakfast_dining")
    )
}
