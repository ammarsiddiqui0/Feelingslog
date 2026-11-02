package com.example.feelingslog

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logs WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllLogs(): Flow<List<FeelingLog>>

    @Transaction
    @Query("SELECT * FROM logs WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllLogsWithDetails(): Flow<List<FeelingLogWithDetails>>

    @Query("SELECT * FROM logs ORDER BY createdAt DESC")
    fun getAllActiveAndDeletedLogs(): Flow<List<FeelingLog>>

    @Transaction
    @Query("SELECT * FROM logs ORDER BY createdAt DESC")
    fun getAllActiveAndDeletedLogsWithDetails(): Flow<List<FeelingLogWithDetails>>

    @Transaction
    @Query("SELECT * FROM logs")
    suspend fun getAllActiveAndDeletedLogsWithDetailsList(): List<FeelingLogWithDetails>

    @Query("SELECT * FROM logs WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedLogs(): Flow<List<FeelingLog>>

    @Transaction
    @Query("SELECT * FROM logs WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedLogsWithDetails(): Flow<List<FeelingLogWithDetails>>

    @Query("SELECT * FROM logs WHERE deletedAt IS NOT NULL AND deletedAt <= :thresholdTimestamp")
    suspend fun getExpiredDeletedLogs(thresholdTimestamp: Long): List<FeelingLog>

    @Transaction
    @Query("SELECT * FROM logs WHERE deletedAt IS NOT NULL AND deletedAt <= :thresholdTimestamp")
    suspend fun getExpiredDeletedLogsWithDetails(thresholdTimestamp: Long): List<FeelingLogWithDetails>

    @Query("SELECT * FROM logs")
    suspend fun getAllLogsList(): List<FeelingLog>

    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogById(id: String): FeelingLog?

    @Transaction
    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogWithDetailsById(id: String): FeelingLogWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FeelingLog)

    @Update
    suspend fun updateLog(log: FeelingLog)

    @Delete
    suspend fun deleteLog(log: FeelingLog)

    // Content Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentItem(item: LogContentItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentItems(items: List<LogContentItem>)

    @Update
    suspend fun updateContentItem(item: LogContentItem)

    @Delete
    suspend fun deleteContentItem(item: LogContentItem)

    @Query("DELETE FROM log_content_items WHERE logId = :logId")
    suspend fun deleteContentItemsForLog(logId: String)

    // Containers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContainer(container: LogContainer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContainers(containers: List<LogContainer>)

    @Update
    suspend fun updateContainer(container: LogContainer)

    @Delete
    suspend fun deleteContainer(container: LogContainer)

    // Tags & Cross Refs
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsList(): List<Tag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<Tag>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogTagCrossRef(crossRef: LogTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogTagCrossRefs(crossRefs: List<LogTagCrossRef>)

    @Query("DELETE FROM log_tag_cross_ref WHERE logId = :logId")
    suspend fun deleteLogTagCrossRefsForLog(logId: String)

    @Transaction
    suspend fun saveFullLog(
        log: FeelingLog,
        containers: List<LogContainer>,
        items: List<LogContentItem>,
        tags: List<Tag>
    ) {
        insertLog(log)
        deleteContentItemsForLog(log.id)
        insertContainers(containers)
        insertContentItems(items)
        deleteLogTagCrossRefsForLog(log.id)
        insertTags(tags)
        val crossRefs = tags.map { LogTagCrossRef(log.id, it.id) }
        insertLogTagCrossRefs(crossRefs)
    }
}
