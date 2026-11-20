package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RemedyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemedyDao {
    @Query("SELECT * FROM remedies ORDER BY name ASC")
    fun getAllRemedies(): Flow<List<RemedyEntity>>

    @Query("SELECT * FROM remedies ORDER BY name ASC")
    suspend fun getRemedies(): List<RemedyEntity>

    @Query("SELECT * FROM remedies WHERE name = :name LIMIT 1")
    suspend fun getRemedyByName(name: String): RemedyEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemedy(remedy: RemedyEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllRemedies(remedies: List<RemedyEntity>)

    @Query("SELECT COUNT(*) FROM remedies")
    suspend fun getRemedyCount(): Int
}
