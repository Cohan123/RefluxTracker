package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.data.model.ActivityEntity
import com.example.data.model.FoodEntity
import com.example.data.model.HabitOverrideEntity
import com.example.data.model.MealEntity
import com.example.data.model.MealFoodCrossRef
import com.example.data.model.RemedyEntity
import com.example.data.model.SymptomActivityCrossRef
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomRemedyCrossRef
import com.example.data.security.DatabaseKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

@Database(
    entities = [
        FoodEntity::class,
        MealEntity::class,
        MealFoodCrossRef::class,
        SymptomEntity::class,
        ActivityEntity::class,
        RemedyEntity::class,
        SymptomActivityCrossRef::class,
        SymptomRemedyCrossRef::class,
        HabitOverrideEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun symptomDao(): SymptomDao
    abstract fun activityDao(): ActivityDao
    abstract fun remedyDao(): RemedyDao
    abstract fun habitOverrideDao(): HabitOverrideDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DB_NAME = "refluxtrack_database"
        private const val KEY_PREFS_FILE = "dbkey.prefs"
        private const val SQLCIPHER_FACTORY_CLASS =
            "net.zetetic.database.sqlcipher.SupportOpenHelperFactory"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Coroutines launched from Room's onCreate callback (initial seed of
        // default foods) run independently of the database lifecycle. Without
        // tracking, a quick close+reopen during tests triggers a
        // "re-open an already-closed object" exception from the still-running
        // coroutine. We keep a list so closeForTest can cancel them.
        private val pendingSeedJobs = CopyOnWriteArrayList<Job>()

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
                // Cancel any still-running seed jobs from previous onCreate
                // callbacks so they don't try to reopen the closed DB.
                pendingSeedJobs.forEach { it.cancel() }
                pendingSeedJobs.clear()
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
            if (!isSqlcipherOnClasspath()) {
                throw IllegalStateException("SQLCipher not on classpath")
            }
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: UnsatisfiedLinkError) {
                throw IllegalStateException("SQLCipher native library not loadable")
            }
            return Room.databaseBuilder(context, AppDatabase::class.java, absolutePath)
                .openHelperFactory(createSqlcipherFactory(rawKey))
                .build()
        }

        private fun isSqlcipherOnClasspath(): Boolean = try {
            Class.forName(SQLCIPHER_FACTORY_CLASS)
            true
        } catch (_: ClassNotFoundException) {
            false
        }

        private fun isRobolectric(): Boolean = try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (_: ClassNotFoundException) {
            false
        }

        /**
         * Builds the SQLCipher [SupportSQLiteOpenHelper.Factory] around [passphrase].
         *
         * The factory keeps the supplied array **by reference** (it is not
         * copied) and Room opens the database lazily on first access. The
         * caller must therefore leave the array untouched until the database
         * has been opened — clearing it here would make SQLCipher open the
         * database with an all-zero key.
         */
        internal fun createSqlcipherFactory(passphrase: ByteArray): SupportSQLiteOpenHelper.Factory {
            val factoryClass = Class.forName(SQLCIPHER_FACTORY_CLASS)
            return factoryClass
                .getDeclaredConstructor(ByteArray::class.java)
                .newInstance(passphrase) as SupportSQLiteOpenHelper.Factory
        }

        private fun build(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                        val job = seedScope.launch {
                            getInstance(context).foodDao()
                                .insertAllFoods(InitialFoodData.PREPOPULATED_FOODS)
                        }
                        pendingSeedJobs.add(job)
                        job.invokeOnCompletion { pendingSeedJobs.remove(job) }
                    }
                })

            attachSqlcipherIfAvailable(context, builder)
            return builder.build()
        }

        /**
         * If the SQLCipher native library is on the classpath, load it and
         * configure Room to use a SupportOpenHelperFactory with the key from
         * [DatabaseKeyProvider].
         *
         * If SQLCipher is absent (e.g. build was assembled with
         * -PenableSqlcipher=false), this is a no-op and Room uses plain SQLite.
         */
        private fun attachSqlcipherIfAvailable(context: Context, builder: RoomDatabase.Builder<AppDatabase>) {
            if (!isSqlcipherOnClasspath()) return
            // Load the native SQLCipher core library bundled in the AAR.
            try {
                System.loadLibrary("sqlcipher")
            } catch (error: UnsatisfiedLinkError) {
                // Robolectric runs on the JVM and cannot load the Android .so.
                // That is the only case where plain SQLite is acceptable: on a
                // real device a missing SQLCipher would silently create an
                // unencrypted health database.
                if (isRobolectric()) {
                    Log.w(
                        TAG,
                        "SQLCipher native library not loadable under Robolectric - " +
                            "using plain SQLite for tests"
                    )
                    return
                }
                throw IllegalStateException(
                    "SQLCipher is bundled but libsqlcipher.so could not be loaded. " +
                        "Refusing to open the database unencrypted.",
                    error
                )
            }

            val prefsFile = File(context.filesDir, KEY_PREFS_FILE)
            // Do NOT keep a local reference and clear it: the factory stores the
            // array by reference and Room opens the database only on first use.
            // Clearing it here would silently open the DB with an all-zero key.
            builder.openHelperFactory(
                createSqlcipherFactory(DatabaseKeyProvider.getOrCreateDatabaseKey(prefsFile))
            )
        }
    }
}
