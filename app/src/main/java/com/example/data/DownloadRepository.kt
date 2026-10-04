package com.example.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.model.DownloadState
import com.example.model.MoodleManifest
import com.example.parser.MoodleCodeParser
import com.example.worker.MoodleDownloadWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DownloadRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val downloadDao = database.downloadDao()
    private val workManager = WorkManager.getInstance(context)

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getWorkInfosByTagFlow(tag: String = "moodle_download"): Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTagFlow(tag)

    suspend fun enqueueDownload(
        code: String,
        manifest: MoodleManifest,
        customFileName: String? = null
    ): String {
        val downloadId = UUID.randomUUID().toString()
        val finalFileName = customFileName?.ifBlank { null } ?: manifest.filename
        val codeFingerprint = MoodleCodeParser.getCodeFingerprint(code)

        val workRequest = OneTimeWorkRequestBuilder<MoodleDownloadWorker>()
            .setInputData(
                workDataOf(
                    MoodleDownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                    MoodleDownloadWorker.KEY_CODE to code,
                    MoodleDownloadWorker.KEY_CUSTOM_FILENAME to finalFileName
                )
            )
            .addTag("moodle_download")
            .addTag("id_$downloadId")
            .build()

        val entity = DownloadEntity(
            id = downloadId,
            workId = workRequest.id.toString(),
            code = code,
            fileName = finalFileName,
            totalParts = manifest.parts.size,
            currentPart = 0,
            totalBytes = manifest.size,
            downloadedBytes = 0L,
            status = DownloadState.PREPARING.name,
            sha256Expected = manifest.sha256,
            createdAt = System.currentTimeMillis()
        )

        downloadDao.insert(entity)

        // Política que evita dos descargas simultáneas del mismo flujo (mismo código)
        workManager.enqueueUniqueWork(
            "download_flow_$codeFingerprint",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        return downloadId
    }

    suspend fun cancelDownload(id: String) {
        val download = downloadDao.getDownloadById(id)
        if (download != null) {
            val codeFingerprint = MoodleCodeParser.getCodeFingerprint(download.code)
            workManager.cancelUniqueWork("download_flow_$codeFingerprint")
            if (download.workId != null) {
                try {
                    workManager.cancelWorkById(UUID.fromString(download.workId))
                } catch (_: Exception) {}
            }
            downloadDao.updateProgress(id, DownloadState.CANCELLED.name, download.currentPart, download.downloadedBytes)
        }
    }

    suspend fun deleteDownload(id: String) {
        downloadDao.deleteById(id)
    }

    suspend fun clearHistory() {
        downloadDao.deleteAll()
    }
}
