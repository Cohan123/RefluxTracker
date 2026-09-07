package com.example.data.local

import com.example.data.model.FoodEntity

object InitialFoodData {
    val PREPOPULATED_FOODS = listOf(
        // Getränke
        FoodEntity(name = "Kaffee", category = "Getränke", iconName = "local_cafe"),
        FoodEntity(name = "Espresso", category = "Getränke", iconName = "local_cafe"),
        FoodEntity(name = "Tee (Schwarz/Grün)", category = "Getränke", iconName = "emoji_food_beverage"),
        FoodEntity(name = "Kräutertee (z.B. Kamille)", category = "Getränke", iconName = "emoji_food_beverage"),
        FoodEntity(name = "Cola", category = "Getränke", iconName = "local_drink"),
        FoodEntity(name = "Energy Drinks", category = "Getränke", iconName = "bolt"),
        FoodEntity(name = "Alkohol", category = "Getränke", iconName = "wine_bar"),
        FoodEntity(name = "Bier", category = "Getränke", iconName = "sports_bar"),
        FoodEntity(name = "Wein", category = "Getränke", iconName = "wine_bar"),
        FoodEntity(name = "Mineralwasser (mit Kohlensäure)", category = "Getränke", iconName = "water_drop"),
        FoodEntity(name = "Stilles Wasser", category = "Getränke", iconName = "water_drop"),

        // Obst & Gemüse
        FoodEntity(name = "Tomaten", category = "Obst & Gemüse", iconName = "restaurant"),
        FoodEntity(name = "Zitrusfrüchte", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Orange", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Zitrone", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Grapefruit", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Zwiebeln", category = "Obst & Gemüse", iconName = "restaurant"),
        FoodEntity(name = "Knoblauch", category = "Obst & Gemüse", iconName = "restaurant"),
        FoodEntity(name = "Minze / Pfefferminze", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Banane", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Apfel", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Gurke", category = "Obst & Gemüse", iconName = "eco"),
        FoodEntity(name = "Kartoffeln", category = "Obst & Gemüse", iconName = "restaurant"),

        // Mahlzeiten & Zubereitung
        FoodEntity(name = "Pizza", category = "Hauptspeisen", iconName = "local_pizza"),
        FoodEntity(name = "Fettreiche Lebensmittel", category = "Hauptspeisen", iconName = "lunch_dining"),
        FoodEntity(name = "Scharfes Essen", category = "Hauptspeisen", iconName = "whatshot"),
        FoodEntity(name = "Burger / Fast Food", category = "Hauptspeisen", iconName = "fastfood"),
        FoodEntity(name = "Pasta mit Tomatensauce", category = "Hauptspeisen", iconName = "dinner_dining"),
        FoodEntity(name = "Reis", category = "Hauptspeisen", iconName = "rice_bowl"),
        FoodEntity(name = "Hähnchen / Geflügel", category = "Hauptspeisen", iconName = "restaurant"),
        FoodEntity(name = "Fisch", category = "Hauptspeisen", iconName = "set_meal"),

        // Süßes & Snacks
        FoodEntity(name = "Schokolade", category = "Süßwaren & Snacks", iconName = "cookie"),
        FoodEntity(name = "Gebäck / Kuchen", category = "Süßwaren & Snacks", iconName = "cake"),
        FoodEntity(name = "Chips / Salziges", category = "Süßwaren & Snacks", iconName = "fastfood"),

        // Milchprodukte & Getreide
        FoodEntity(name = "Milchprodukte (Vollmilch/Sahne)", category = "Milch & Getreide", iconName = "egg_alt"),
        FoodEntity(name = "Käse", category = "Milch & Getreide", iconName = "restaurant"),
        FoodEntity(name = "Joghurt", category = "Milch & Getreide", iconName = "restaurant"),
        FoodEntity(name = "Brot / Toast", category = "Milch & Getreide", iconName = "bakery_dining"),
        FoodEntity(name = "Haferflocken / Porridge", category = "Milch & Getreide", iconName = "breakfast_dining")
    )
}
