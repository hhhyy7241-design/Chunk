package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED', 'ERROR') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('COMPLETED', 'COMPLETED_WARN_HASH') ORDER BY completedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING', 'QUEUED')")
    suspend fun getUnfinishedDownloads(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, currentPart = :currentPart, completedParts = :completedParts, downloadedBytes = :downloadedBytes, speedBps = :speedBps, etaSeconds = :etaSeconds WHERE id = :id")
    suspend fun updateProgress(
        id: String,
        status: String,
        currentPart: Int,
        completedParts: Int,
        downloadedBytes: Long,
        speedBps: Long,
        etaSeconds: Long
    )

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String? = null)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error, completedAt = :completedAt, speedBps = 0, etaSeconds = 0 WHERE id = :id")
    suspend fun markFailed(id: String, status: String, error: String, completedAt: Long)

    @Query("UPDATE downloads SET status = :status, mediaStoreUri = :uri, savedPath = :path, sha256Calculated = :sha256, downloadedBytes = :bytes, completedParts = totalParts, currentPart = totalParts, speedBps = 0, etaSeconds = 0, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(
        id: String,
        status: String,
        uri: String?,
        path: String?,
        sha256: String?,
        bytes: Long,
        completedAt: Long
    )

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'COMPLETED_WARN_HASH')")
    suspend fun deleteCompleted()
}
