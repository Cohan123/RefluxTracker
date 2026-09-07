package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "foods",
    indices = [Index(value = ["name"], unique = true)]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val iconName: String = "restaurant",
    val isCustom: Boolean = false
)

@Entity(
    tableName = "meals",
    indices = [Index(value = ["timestamp"])]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mealType: String, // "Frühstück", "Mittagessen", "Abendessen", "Snack", "Sonstiges"
    val portion: String = "", // "Normal", "Groß", "Klein"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "meal_food_cross_ref",
    primaryKeys = ["mealId", "foodId"],
    indices = [Index("mealId"), Index("foodId")]
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

@Entity(
    tableName = "symptoms",
    indices = [Index(value = ["timestamp"])]
)
data class SymptomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val symptomType: String, // "Sodbrennen", "Säurerückfluss", "Aufstoßen", etc.
    val intensity: Int, // 0..10
    val durationMinutes: Int? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// Supported Symptom types as defined in specifications
object SymptomTypes {
    const val SODBRENNEN = "Sodbrennen"
    const val SAEURERUECKFLUSS = "Säurerückfluss"
    const val AUFSTOSSEN = "Aufstoßen"
    const val RAEUSPERN = "Räuspern"
    const val HUSTEN = "Husten"
    const val HALSBRENNEN = "Halsbrennen"
    const val DRUCKGEFUEHL = "Druckgefühl"
    const val UEBELKEIT = "Übelkeit"
    const val BLAEHUNGEN = "Blähungen"
    const val SCHLAFPROBLEME = "Schlafprobleme"

    val ALL = listOf(
        SODBRENNEN,
        SAEURERUECKFLUSS,
        AUFSTOSSEN,
        RAEUSPERN,
        HUSTEN,
        HALSBRENNEN,
        DRUCKGEFUEHL,
        UEBELKEIT,
        BLAEHUNGEN,
        SCHLAFPROBLEME
    )
}

// Supported Meal types
object MealTypes {
    const val FRUEHSTUECK = "Frühstück"
    const val MITTAGESSEN = "Mittagessen"
    const val ABENDESSEN = "Abendessen"
    const val SNACK = "Snack"
    const val SONSTIGES = "Sonstiges"

    val ALL = listOf(
        FRUEHSTUECK,
        MITTAGESSEN,
        ABENDESSEN,
        SNACK,
        SONSTIGES
    )
}
