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

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE workId = :workId")
    suspend fun getDownloadByWorkId(workId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, currentPart = :currentPart, downloadedBytes = :downloadedBytes WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, currentPart: Int, downloadedBytes: Long)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error, completedAt = :completedAt WHERE id = :id")
    suspend fun markFailed(id: String, status: String, error: String, completedAt: Long)

    @Query("UPDATE downloads SET status = :status, mediaStoreUri = :uri, savedPath = :path, sha256Calculated = :sha256, downloadedBytes = :bytes, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: String, status: String, uri: String, path: String, sha256: String, bytes: Long, completedAt: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}
