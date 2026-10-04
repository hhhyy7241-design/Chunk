package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val workId: String? = null,
    val code: String,
    val fileName: String,
    val totalParts: Int,
    val currentPart: Int = 0,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String, // QUEUED, DOWNLOADING, RECONSTRUCTING, COMPLETED, FAILED, CANCELLED
    val errorMessage: String? = null,
    val sha256Calculated: String? = null,
    val sha256Expected: String? = null,
    val mediaStoreUri: String? = null,
    val savedPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
