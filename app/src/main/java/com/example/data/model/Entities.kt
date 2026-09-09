package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class MealType(val displayName: String) {
    FRUEHSTUECK("Frühstück"),
    MITTAGESSEN("Mittagessen"),
    ABENDESSEN("Abendessen"),
    SNACK("Snack"),
    GETRAENK("Getränk")
}

enum class PortionSize(val displayName: String) {
    KLEIN("Kleine Portion"),
    MITTEL("Mittlere Portion"),
    GROSS("Große Portion")
}

enum class SymptomType(val displayName: String) {
    SODBRENNEN("Sodbrennen"),
    SAURES_AUFSTOSSEN("Saures Aufstoßen"),
    MAGENDRUCK("Magendruck / Völlegefühl"),
    HALSREIZUNG("Halskratzen / Räusperzwang"),
    HEISERKEIT("Heiserkeit"),
    HUSTEN("Reizhusten"),
    BRUSTSCHMERZ("Druck in der Brust"),
    SCHLUCKRESCHWERDEN("Schluckbeschwerden"),
    SONSTIGES("Sonstiges Symptom")
}

enum class TriggerLikelihood(val label: String) {
    HOCH("Hoher Zusammenhang"),
    MITTEL("Mittlerer Zusammenhang"),
    GERING("Geringer Zusammenhang"),
    UNBEKANNT("Noch zu wenig Daten")
}

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Allgemein",
    val defaultPortion: PortionSize = PortionSize.MITTEL
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mealType: MealType,
    val portionSize: PortionSize = PortionSize.MITTEL,
    val notes: String = ""
)

@Entity(
    tableName = "meal_food_cross_ref",
    primaryKeys = ["mealId", "foodId"],
    indices = [Index("foodId")]
)
data class MealFoodCrossRef(
    val mealId: Long,
    val foodId: Long
)

data class MealWithFoods(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MealFoodCrossRef::class,
            parentColumn = "mealId",
            entityColumn = "foodId"
        )
    )
    val foods: List<FoodEntity>
)

object RefluxConstants {
    val STANDARD_SYMPTOMS = listOf(
        "Sodbrennen",
        "Saures Aufstoßen",
        "Magendruck / Völlegefühl",
        "Druckgefühl / Druck in der Brust",
        "Übelkeit",
        "Halskratzen / Räusperzwang",
        "Heiserkeit",
        "Reizhusten",
        "Schluckbeschwerden",
        "Brennen im Rachen",
        "Bitterer Geschmack",
        "Sonstiges Symptom"
    )

    val STANDARD_ACTIVITIES = listOf(
        "Liegen",
        "Sitzen",
        "Stehen",
        "Sport / Bewegung",
        "Bücken",
        "Schlafen",
        "Gehen",
        "Direkt nach dem Essen",
        "Stress / Hektik",
        "Enge Kleidung"
    )

    val STANDARD_REMEDIES = listOf(
        "Wasser getrunken",
        "Aufrecht hingesetzt",
        "Aufgestanden",
        "Etwas gegessen",
        "Bestimmte Lebensmittel vermieden",
        "Medikament eingenommen",
        "Spaziergang gemacht",
        "Schlafposition geändert / Oberkörper hoch",
        "Kleidung gelockert",
        "Kräutertee getrunken"
    )
}

@Entity(tableName = "symptoms")
data class SymptomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val symptomType: SymptomType = SymptomType.SODBRENNEN,
    val intensity: Int, // 1 bis 10
    val durationMinutes: Int = 30,
    val notes: String = "",
    val symptoms: String = "", // Kommagetrennte Liste der ausgewählten Symptome
    val activities: String = "", // Kommagetrennte Liste der Tätigkeiten/Situationen
    val remedies: String = "" // Kommagetrennte Liste der ergriffenen Maßnahmen
) {
    fun getSymptomList(): List<String> {
        val list = symptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return if (list.isNotEmpty()) list else listOf(symptomType.displayName)
    }

    fun getActivityList(): List<String> {
        return activities.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getRemedyList(): List<String> {
        return remedies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

data class FoodTriggerAnalysis(
    val foodId: Long,
    val foodName: String,
    val category: String,
    val timesConsumed: Int,
    val timesTriggered: Int,
    val triggerPercentage: Int, // 0 - 100
    val avgIntensity: Double,
    val avgDelayMinutes: Long,
    val likelihood: TriggerLikelihood
)

sealed class TimelineItem(val timestamp: Long) {
    data class MealItem(val mealWithFoods: MealWithFoods) : TimelineItem(mealWithFoods.meal.timestamp)
    data class SymptomItem(val symptom: SymptomEntity) : TimelineItem(symptom.timestamp)
}
