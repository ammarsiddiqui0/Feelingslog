package com.example.feelingslog

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logs WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllLogs(): Flow<List<FeelingLog>>

    @Query("SELECT * FROM logs ORDER BY createdAt DESC")
    fun getAllActiveAndDeletedLogs(): Flow<List<FeelingLog>>

    @Query("SELECT * FROM logs WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedLogs(): Flow<List<FeelingLog>>

    @Query("SELECT * FROM logs WHERE deletedAt IS NOT NULL AND deletedAt <= :thresholdTimestamp")
    suspend fun getExpiredDeletedLogs(thresholdTimestamp: Long): List<FeelingLog>

    @Query("SELECT * FROM logs")
    suspend fun getAllLogsList(): List<FeelingLog>

    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogById(id: String): FeelingLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FeelingLog)

    @Update
    suspend fun updateLog(log: FeelingLog)

    @Delete
    suspend fun deleteLog(log: FeelingLog)
}
