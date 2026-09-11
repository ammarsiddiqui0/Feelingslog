package com.example.feelingslog

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY updatedAt DESC")
    fun getAllLogs(): Flow<List<FeelingLog>>

    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogById(id: String): FeelingLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FeelingLog)

    @Update
    suspend fun updateLog(log: FeelingLog)

    @Delete
    suspend fun deleteLog(log: FeelingLog)
}
