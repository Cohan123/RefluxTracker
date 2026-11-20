package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY name ASC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY name ASC")
    suspend fun getActivities(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE name = :name LIMIT 1")
    suspend fun getActivityByName(name: String): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllActivities(activities: List<ActivityEntity>)

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getActivityCount(): Int
}
