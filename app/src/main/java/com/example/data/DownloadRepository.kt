package com.example.data

import android.content.Context
import com.example.model.DownloadState
import com.example.model.MoodleManifest
import com.example.service.DownloadService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DownloadRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val downloadDao = database.downloadDao()

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()

    suspend fun enqueueDownload(
        code: String,
        manifest: MoodleManifest,
        customFileName: String? = null
    ): String {
        val downloadId = UUID.randomUUID().toString()
        val finalFileName = customFileName?.ifBlank { null } ?: manifest.filename

        val entity = DownloadEntity(
            id = downloadId,
            fileName = finalFileName,
            totalBytes = manifest.size,
            totalParts = manifest.parts.size,
            completedParts = 0,
            currentPart = 0,
            downloadedBytes = 0L,
            status = DownloadState.QUEUED.name,
            savedPath = "Download/Chunk/$finalFileName",
            code = code,
            sha256Expected = manifest.sha256,
            createdAt = System.currentTimeMillis()
        )

        downloadDao.insert(entity)

        // Iniciar el Foreground Service nativo con WakeLock y WifiLock
        DownloadService.startDownload(context, downloadId)

        return downloadId
    }

    suspend fun pauseDownload(id: String) {
        DownloadService.pauseDownload(context, id)
        downloadDao.updateStatus(id, DownloadState.PAUSED.name, null)
    }

    suspend fun resumeDownload(id: String) {
        downloadDao.updateStatus(id, DownloadState.QUEUED.name, null)
        DownloadService.resumeDownload(context, id)
    }

    suspend fun retryDownload(id: String) {
        downloadDao.updateStatus(id, DownloadState.QUEUED.name, null)
        DownloadService.startDownload(context, id)
    }

    suspend fun cancelDownload(id: String) {
        DownloadService.cancelDownload(context, id)
        downloadDao.updateStatus(id, DownloadState.CANCELLED.name, null)
    }

    suspend fun deleteDownload(id: String) {
        DownloadService.cancelDownload(context, id)
        downloadDao.deleteById(id)
    }

    suspend fun clearCompleted() {
        downloadDao.deleteCompleted()
    }

    suspend fun clearHistory() {
        downloadDao.deleteAll()
    }

    suspend fun resumePendingDownloadsOnStartup() {
        val pending = downloadDao.getUnfinishedDownloads()
        for (item in pending) {
            DownloadService.startDownload(context, item.id)
        }
    }
}
