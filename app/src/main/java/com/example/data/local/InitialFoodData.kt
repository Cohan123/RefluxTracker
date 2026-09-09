package com.example.data.local

import com.example.data.model.FoodEntity

object InitialFoodData {
    val PREPOPULATED_FOODS = listOf(
        // Getränke
        FoodEntity(name = "Kaffee", category = "Getränke"),
        FoodEntity(name = "Espresso", category = "Getränke"),
        FoodEntity(name = "Tee (Schwarz/Grün)", category = "Getränke"),
        FoodEntity(name = "Kräutertee (z.B. Kamille)", category = "Getränke"),
        FoodEntity(name = "Cola", category = "Getränke"),
        FoodEntity(name = "Energy Drinks", category = "Getränke"),
        FoodEntity(name = "Alkohol", category = "Getränke"),
        FoodEntity(name = "Bier", category = "Getränke"),
        FoodEntity(name = "Wein", category = "Getränke"),
        FoodEntity(name = "Mineralwasser (mit Kohlensäure)", category = "Getränke"),
        FoodEntity(name = "Stilles Wasser", category = "Getränke"),

        // Obst & Gemüse
        FoodEntity(name = "Tomaten", category = "Obst & Gemüse"),
        FoodEntity(name = "Zitrusfrüchte", category = "Obst & Gemüse"),
        FoodEntity(name = "Orange", category = "Obst & Gemüse"),
        FoodEntity(name = "Zitrone", category = "Obst & Gemüse"),
        FoodEntity(name = "Grapefruit", category = "Obst & Gemüse"),
        FoodEntity(name = "Zwiebeln", category = "Obst & Gemüse"),
        FoodEntity(name = "Knoblauch", category = "Obst & Gemüse"),
        FoodEntity(name = "Minze / Pfefferminze", category = "Obst & Gemüse"),
        FoodEntity(name = "Banane", category = "Obst & Gemüse"),
        FoodEntity(name = "Apfel", category = "Obst & Gemüse"),
        FoodEntity(name = "Gurke", category = "Obst & Gemüse"),
        FoodEntity(name = "Kartoffeln", category = "Obst & Gemüse"),

        // Mahlzeiten & Zubereitung
        FoodEntity(name = "Pizza", category = "Hauptspeisen"),
        FoodEntity(name = "Fettreiche Lebensmittel", category = "Hauptspeisen"),
        FoodEntity(name = "Scharfes Essen", category = "Hauptspeisen"),
        FoodEntity(name = "Burger / Fast Food", category = "Hauptspeisen"),
        FoodEntity(name = "Pasta mit Tomatensauce", category = "Hauptspeisen"),
        FoodEntity(name = "Reis", category = "Hauptspeisen"),
        FoodEntity(name = "Hähnchen / Geflügel", category = "Hauptspeisen"),
        FoodEntity(name = "Fisch", category = "Hauptspeisen"),

        // Süßes & Snacks
        FoodEntity(name = "Schokolade", category = "Süßwaren & Snacks"),
        FoodEntity(name = "Gebäck / Kuchen", category = "Süßwaren & Snacks"),
        FoodEntity(name = "Chips / Salziges", category = "Süßwaren & Snacks"),

        // Milchprodukte & Getreide
        FoodEntity(name = "Milchprodukte (Vollmilch/Sahne)", category = "Milch & Getreide"),
        FoodEntity(name = "Käse", category = "Milch & Getreide"),
        FoodEntity(name = "Joghurt", category = "Milch & Getreide"),
        FoodEntity(name = "Brot / Toast", category = "Milch & Getreide"),
        FoodEntity(name = "Haferflocken / Porridge", category = "Milch & Getreide")
    )
}
