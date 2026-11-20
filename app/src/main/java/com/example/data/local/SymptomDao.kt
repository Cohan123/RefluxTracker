package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.SymptomActivityCrossRef
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomRemedyCrossRef
import com.example.data.model.SymptomWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SymptomDao {
    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    abstract fun getAllSymptoms(): Flow<List<SymptomEntity>>

    @Transaction
    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    abstract fun getAllSymptomsWithDetails(): Flow<List<SymptomWithDetails>>

    @Transaction
    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    abstract suspend fun getAllSymptomsWithDetailsOnce(): List<SymptomWithDetails>

    @Query("SELECT * FROM symptoms WHERE timestamp >= :startEpoch AND timestamp < :endEpoch ORDER BY timestamp ASC")
    abstract fun getSymptomsBetween(startEpoch: Long, endEpoch: Long): Flow<List<SymptomEntity>>

    @Transaction
    @Query("SELECT * FROM symptoms WHERE timestamp >= :startEpoch AND timestamp < :endEpoch ORDER BY timestamp ASC")
    abstract fun getSymptomsBetweenWithDetails(
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<SymptomWithDetails>>

    @Query("SELECT * FROM symptoms WHERE id = :id")
    abstract suspend fun getSymptomById(id: Long): SymptomEntity?

    @Transaction
    @Query("SELECT * FROM symptoms WHERE id = :id")
    abstract suspend fun getSymptomWithDetails(id: Long): SymptomWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSymptom(symptom: SymptomEntity): Long

    @Update
    abstract suspend fun updateSymptom(symptom: SymptomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivityCrossRefs(crossRefs: List<SymptomActivityCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRemedyCrossRefs(crossRefs: List<SymptomRemedyCrossRef>)

    @Query("DELETE FROM symptom_activity_cross_ref WHERE symptomId = :symptomId")
    abstract suspend fun deleteActivityCrossRefsForSymptom(symptomId: Long)

    @Query("DELETE FROM symptom_remedy_cross_ref WHERE symptomId = :symptomId")
    abstract suspend fun deleteRemedyCrossRefsForSymptom(symptomId: Long)

    @Query("DELETE FROM symptoms WHERE id = :id")
    abstract suspend fun deleteSymptomById(id: Long)

    @Query("SELECT COUNT(*) FROM symptoms")
    abstract fun getSymptomCount(): Flow<Int>

    @Query("SELECT AVG(intensity) FROM symptoms WHERE timestamp >= :startEpoch AND timestamp < :endEpoch")
    abstract fun getAverageIntensityBetween(startEpoch: Long, endEpoch: Long): Flow<Double?>

    @Query("SELECT MIN(timestamp) FROM symptoms")
    abstract suspend fun getFirstSymptomTimestamp(): Long?

    /**
     * Inserts a symptom and its activity/remedy selection atomically, so a
     * failure while writing the junctions cannot leave a symptom without its
     * details.
     */
    @Transaction
    open suspend fun insertSymptomWithDetails(
        symptom: SymptomEntity,
        activityIds: List<Long>,
        remedyIds: List<Long>
    ): Long {
        val symptomId = insertSymptom(symptom)
        insertDetails(symptomId, activityIds, remedyIds)
        return symptomId
    }

    /** Updates a symptom and replaces its activity/remedy selection atomically. */
    @Transaction
    open suspend fun updateSymptomWithDetails(
        symptom: SymptomEntity,
        activityIds: List<Long>,
        remedyIds: List<Long>
    ) {
        updateSymptom(symptom)
        deleteActivityCrossRefsForSymptom(symptom.id)
        deleteRemedyCrossRefsForSymptom(symptom.id)
        insertDetails(symptom.id, activityIds, remedyIds)
    }

    private suspend fun insertDetails(
        symptomId: Long,
        activityIds: List<Long>,
        remedyIds: List<Long>
    ) {
        val distinctActivityIds = activityIds.distinct()
        if (distinctActivityIds.isNotEmpty()) {
            insertActivityCrossRefs(
                distinctActivityIds.map { SymptomActivityCrossRef(symptomId, it) }
            )
        }
        val distinctRemedyIds = remedyIds.distinct()
        if (distinctRemedyIds.isNotEmpty()) {
            insertRemedyCrossRefs(
                distinctRemedyIds.map { SymptomRemedyCrossRef(symptomId, it) }
            )
        }
    }

    /** Deletes the symptom together with its junction rows. */
    @Transaction
    open suspend fun deleteSymptomWithDetails(symptomId: Long) {
        deleteActivityCrossRefsForSymptom(symptomId)
        deleteRemedyCrossRefsForSymptom(symptomId)
        deleteSymptomById(symptomId)
    }
}
