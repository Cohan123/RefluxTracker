package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealFoodCrossRef
import com.example.data.model.SymptomEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FoodEntity::class,
        MealEntity::class,
        MealFoodCrossRef::class,
        SymptomEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RefluxDatabase : RoomDatabase() {

    abstract fun refluxDao(): RefluxDao

    companion object {
        @Volatile
        private var INSTANCE: RefluxDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE symptoms ADD COLUMN symptoms TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE symptoms ADD COLUMN activities TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE symptoms ADD COLUMN remedies TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): RefluxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RefluxDatabase::class.java,
                    "reflux_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initial standard foods so the user can immediately pick items
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.refluxDao()?.insertFoods(initialFoods)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val initialFoods = listOf(
            FoodEntity(name = "Kaffee (schwarz)", category = "Getränke"),
            FoodEntity(name = "Cappuccino / Milchkaffee", category = "Getränke"),
            FoodEntity(name = "Orangensaft", category = "Getränke"),
            FoodEntity(name = "Pfefferminztee", category = "Getränke"),
            FoodEntity(name = "Mineralwasser (mit Kohlensäure)", category = "Getränke"),
            FoodEntity(name = "Stilles Wasser", category = "Getränke"),
            FoodEntity(name = "Kamillentee", category = "Getränke"),
            FoodEntity(name = "Schokolade (Vollmilch)", category = "Süßwaren"),
            FoodEntity(name = "Schokolade (Zartbitter)", category = "Süßwaren"),
            FoodEntity(name = "Tomatensoße / Pasta Pomodoro", category = "Hauptspeisen"),
            FoodEntity(name = "Pizza Salami", category = "Hauptspeisen"),
            FoodEntity(name = "Pommes Frites", category = "Beilagen"),
            FoodEntity(name = "Zwiebeln (roh)", category = "Gemüse"),
            FoodEntity(name = "Knoblauch", category = "Gewürze"),
            FoodEntity(name = "Haferbrei / Porridge", category = "Frühstück"),
            FoodEntity(name = "Banane", category = "Obst"),
            FoodEntity(name = "Apfel", category = "Obst"),
            FoodEntity(name = "Trauben", category = "Obst"),
            FoodEntity(name = "Vollkornbrot mit Käse", category = "Brotzeit"),
            FoodEntity(name = "Hähnchenbrust gegrillt", category = "Fleisch"),
            FoodEntity(name = "Chili / Scharfes Gericht", category = "Hauptspeisen"),
            FoodEntity(name = "Bier", category = "Alkohol"),
            FoodEntity(name = "Rotwein", category = "Alkohol")
        )
    }
}
