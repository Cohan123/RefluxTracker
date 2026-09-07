package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SymptomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    fun getAllSymptoms(): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms WHERE timestamp >= :startEpoch AND timestamp < :endEpoch ORDER BY timestamp ASC")
    fun getSymptomsBetween(startEpoch: Long, endEpoch: Long): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms WHERE id = :id")
    suspend fun getSymptomById(id: Long): SymptomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomEntity): Long

    @Update
    suspend fun updateSymptom(symptom: SymptomEntity)

    @Query("DELETE FROM symptoms WHERE id = :id")
    suspend fun deleteSymptomById(id: Long)

    @Query("SELECT COUNT(*) FROM symptoms")
    fun getSymptomCount(): Flow<Int>

    @Query("SELECT AVG(intensity) FROM symptoms WHERE timestamp >= :startEpoch AND timestamp < :endEpoch")
    fun getAverageIntensityBetween(startEpoch: Long, endEpoch: Long): Flow<Double?>

    @Query("SELECT MIN(timestamp) FROM symptoms")
    suspend fun getFirstSymptomTimestamp(): Long?
}
