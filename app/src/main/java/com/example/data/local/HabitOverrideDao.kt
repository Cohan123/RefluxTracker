package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HabitOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitOverrideDao {
    @Query("SELECT * FROM habit_overrides")
    fun getAllOverrides(): Flow<List<HabitOverrideEntity>>

    @Query("SELECT * FROM habit_overrides")
    suspend fun getOverrides(): List<HabitOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: HabitOverrideEntity)

    @Query("DELETE FROM habit_overrides WHERE signature = :signature")
    suspend fun delete(signature: String)
}
