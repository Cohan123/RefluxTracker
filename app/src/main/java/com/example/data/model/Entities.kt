package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
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
    val isCustom: Boolean = false,
    /**
     * Comma-separated stable keys from [FoodCategories]; empty for entries
     * created before schema v3 and for foods the user has not categorised yet.
     */
    @ColumnInfo(defaultValue = "''")
    val categories: String = ""
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
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * JSON array with every symptom selected for this entry, e.g.
     * `["Sodbrennen","Aufstoßen"]`. [symptomType] keeps the first/primary
     * selection so existing analysis and CSV export stay compatible.
     */
    @ColumnInfo(defaultValue = "'[]'")
    val symptomTypesJson: String = "[]"
)

@Entity(
    tableName = "activities",
    indices = [Index(value = ["name"], unique = true)]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "remedies",
    indices = [Index(value = ["name"], unique = true)]
)
data class RemedyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "symptom_activity_cross_ref",
    primaryKeys = ["symptomId", "activityId"],
    indices = [Index("activityId")],
    foreignKeys = [
        ForeignKey(
            entity = SymptomEntity::class,
            parentColumns = ["id"],
            childColumns = ["symptomId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SymptomActivityCrossRef(
    val symptomId: Long,
    val activityId: Long
)

@Entity(
    tableName = "symptom_remedy_cross_ref",
    primaryKeys = ["symptomId", "remedyId"],
    indices = [Index("remedyId")],
    foreignKeys = [
        ForeignKey(
            entity = SymptomEntity::class,
            parentColumns = ["id"],
            childColumns = ["symptomId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RemedyEntity::class,
            parentColumns = ["id"],
            childColumns = ["remedyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SymptomRemedyCrossRef(
    val symptomId: Long,
    val remedyId: Long
)

/**
 * User-specific adjustments to automatically detected meal habits.
 *
 * Habits themselves are recomputed deterministically from the meal history so
 * that no stale copy of health data is kept. Overrides only store a custom
 * name, a custom food selection or a deleted marker, keyed by the stable
 * [signature] produced by the detector.
 */
@Entity(tableName = "habit_overrides")
data class HabitOverrideEntity(
    @PrimaryKey val signature: String,
    val customName: String? = null,
    /** Comma-separated food names; null keeps the detected selection. */
    val customFoods: String? = null,
    val isDeleted: Boolean = false
)

/**
 * A symptom together with the activities (possible triggers) and remedies
 * selected for it. Symptom types themselves are stored as a JSON list in
 * [SymptomEntity.symptomTypesJson] because they are a fixed, non-extensible set.
 */
data class SymptomWithDetails(
    @Embedded val symptom: SymptomEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SymptomActivityCrossRef::class,
            parentColumn = "symptomId",
            entityColumn = "activityId"
        )
    )
    val activities: List<ActivityEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SymptomRemedyCrossRef::class,
            parentColumn = "symptomId",
            entityColumn = "remedyId"
        )
    )
    val remedies: List<RemedyEntity>
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
    const val GETRAENK = "Getränk"
    const val SONSTIGES = "Sonstiges"

    val ALL = listOf(
        FRUEHSTUECK,
        MITTAGESSEN,
        ABENDESSEN,
        SNACK,
        GETRAENK,
        SONSTIGES
    )
}
