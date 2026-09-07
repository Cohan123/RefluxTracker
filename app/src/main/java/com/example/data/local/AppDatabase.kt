package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FoodEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealFoodCrossRef
import com.example.data.model.SymptomEntity
import com.example.data.security.DatabaseKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Database(
    entities = [
        FoodEntity::class,
        MealEntity::class,
        MealFoodCrossRef::class,
        SymptomEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun symptomDao(): SymptomDao

    companion object {
        private const val DB_NAME = "refluxtrack_database"
        private const val KEY_PREFS_FILE = "dbkey.prefs"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = build(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        /** Test-only: close the singleton so the next [getInstance] rebuilds it. */
        fun closeForTest() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        /**
         * Test-only: open a Room database file with an explicit raw key, bypassing
         * the [DatabaseKeyProvider]. Used to prove SQLCipher is wired up (opening
         * with the wrong key must throw if SQLCipher is active).
         */
        fun openWithRawKeyForTest(
            context: Context,
            absolutePath: String,
            rawKey: ByteArray
        ): AppDatabase {
            val supportFactoryClass = try {
                Class.forName("net.zetetic.database.sqlcipher.SupportOpenHelperFactory")
            } catch (_: ClassNotFoundException) {
                throw IllegalStateException("SQLCipher not on classpath")
            }
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: UnsatisfiedLinkError) {
                throw IllegalStateException("SQLCipher native library not loadable")
            }
            val factory = supportFactoryClass
                .getDeclaredConstructor(ByteArray::class.java)
                .newInstance(rawKey)
            @Suppress("UNCHECKED_CAST")
            return Room.databaseBuilder(context, AppDatabase::class.java, absolutePath)
                .openHelperFactory(factory as androidx.sqlite.db.SupportSQLiteOpenHelper.Factory)
                .build()
        }

        private fun build(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration(false)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).foodDao()
                                .insertAllFoods(InitialFoodData.PREPOPULATED_FOODS)
                        }
                    }
                })

            attachSqlcipherIfAvailable(context, builder)
            return builder.build()
        }

        /**
         * If the SQLCipher native library is on the classpath, load it and
         * configure Room to use a SupportOpenHelperFactory with a key derived
         * from the Android Keystore (or, on legacy devices / Robolectric, the
         * obfuscated file-backed key — see [DatabaseKeyProvider]).
         *
         * If SQLCipher is absent (e.g. build was assembled with
         * -PenableSqlcipher=false), this is a no-op and Room uses plain SQLite.
         */
        private fun attachSqlcipherIfAvailable(context: Context, builder: RoomDatabase.Builder<AppDatabase>) {
            val supportFactoryClass = try {
                Class.forName("net.zetetic.database.sqlcipher.SupportOpenHelperFactory")
            } catch (_: ClassNotFoundException) {
                return
            }
            // Load the native SQLCipher core library bundled in the AAR.
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: UnsatisfiedLinkError) {
                // Library not present (Robolectric test runs in-process). Skip.
                return
            }

            val prefsFile = File(context.filesDir, KEY_PREFS_FILE)
            val passphrase: ByteArray = DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile)
            try {
                val factory = supportFactoryClass
                    .getDeclaredConstructor(ByteArray::class.java)
                    .newInstance(passphrase)
                @Suppress("UNCHECKED_CAST")
                builder.openHelperFactory(factory as androidx.sqlite.db.SupportSQLiteOpenHelper.Factory)
                // The passphrase byte[] is consumed by SQLCipher's factory; zero it
                // out so it doesn't linger in heap.
                passphrase.fill(0)
            } catch (_: Throwable) {
                // If construction fails for any reason, fall back to plain SQLite
                // rather than crash the app. The DB will be created without
                // encryption — caller's threat model is degraded for this run.
                passphrase.fill(0)
            }
        }
    }
}
