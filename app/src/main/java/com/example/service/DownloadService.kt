package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.DownloadEntity
import com.example.data.SettingsManager
import com.example.model.DownloadState
import com.example.parser.MoodleCodeParser
import com.example.util.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    companion object {
        const val CHANNEL_PROGRESS_ID = "download_progress_channel"
        const val CHANNEL_COMPLETED_ID = "download_completed_channel"
        const val CHANNEL_ERROR_ID = "download_error_channel"

        private const val NOTIFICATION_PROGRESS_ID = 1001
        private const val BUFFER_SIZE = 1024 * 512 // 512 KiB buffer

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_CANCEL = "com.example.service.ACTION_CANCEL"
        const val ACTION_RETRY = "com.example.service.ACTION_RETRY"

        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun startDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (_: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }

        fun pauseDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }

        fun resumeDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (_: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }

        fun cancelDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager
    private lateinit var database: AppDatabase
    private lateinit var settingsManager: SettingsManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        database = AppDatabase.getDatabase(this)
        settingsManager = SettingsManager(this)

        createNotificationChannels()
        initLocks()
        registerNetworkCallback()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadId = intent?.getStringExtra(EXTRA_DOWNLOAD_ID)

        when (intent?.action) {
            ACTION_START, ACTION_RESUME -> {
                if (downloadId != null) {
                    startOrResumeDownload(downloadId)
                }
            }
            ACTION_PAUSE -> {
                if (downloadId != null) {
                    pauseDownloadInternal(downloadId)
                }
            }
            ACTION_CANCEL -> {
                if (downloadId != null) {
                    cancelDownloadInternal(downloadId)
                }
            }
            ACTION_RETRY -> {
                if (downloadId != null) {
                    startOrResumeDownload(downloadId)
                }
            }
            else -> {
                // Si el servicio se reinició por el sistema, retomar descargas activas en DB
                checkAndResumePendingDownloads()
            }
        }

        return START_STICKY
    }

    private fun checkAndResumePendingDownloads() {
        serviceScope.launch {
            val unfinished = database.downloadDao().getUnfinishedDownloads()
            for (item in unfinished) {
                startOrResumeDownload(item.id)
            }
            if (unfinished.isEmpty()) {
                stopSelfIfIdle()
            }
        }
    }

    private fun startOrResumeDownload(downloadId: String) {
        if (activeJobs[downloadId]?.isActive == true) return

        val job = serviceScope.launch {
            val entity = database.downloadDao().getDownloadById(downloadId) ?: return@launch
            val settings = settingsManager.settings.value

            // Comprobación de Wi-Fi si wifiOnly está activo
            if (settings.wifiOnly && !isWifiConnected()) {
                database.downloadDao().updateStatus(downloadId, DownloadState.PAUSED.name, "Pausada: Solo con Wi-Fi está activado.")
                updateNotification()
                return@launch
            }

            acquireLocks()
            startForegroundNotification(entity.fileName)

            try {
                database.downloadDao().updateStatus(downloadId, DownloadState.DOWNLOADING.name, null)
                executeDownloadPipeline(entity)
            } catch (e: CancellationException) {
                // Cancelado o pausado explícitamente
            } catch (e: Exception) {
                handleDownloadError(downloadId, e)
            } finally {
                activeJobs.remove(downloadId)
                releaseLocksIfIdle()
                updateNotification()
                stopSelfIfIdle()
            }
        }

        activeJobs[downloadId] = job
    }

    private fun pauseDownloadInternal(downloadId: String) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        serviceScope.launch {
            database.downloadDao().updateStatus(downloadId, DownloadState.PAUSED.name, null)
            releaseLocksIfIdle()
            updateNotification()
            stopSelfIfIdle()
        }
    }

    private fun cancelDownloadInternal(downloadId: String) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        serviceScope.launch {
            database.downloadDao().updateStatus(downloadId, DownloadState.CANCELLED.name, null)
            releaseLocksIfIdle()
            updateNotification()
            stopSelfIfIdle()
        }
    }

    private suspend fun executeDownloadPipeline(entity: DownloadEntity) {
        val parseResult = MoodleCodeParser.parse(entity.code)
        if (parseResult !is MoodleCodeParser.ParseResult.Success) {
            val errorMsg = (parseResult as? MoodleCodeParser.ParseResult.Error)?.message ?: "Código de descarga inválido."
            database.downloadDao().markFailed(entity.id, DownloadState.ERROR.name, errorMsg, System.currentTimeMillis())
            showErrorNotification(entity.id, entity.fileName, errorMsg)
            return
        }

        val manifest = parseResult.manifest
        val codeFingerprint = MoodleCodeParser.getCodeFingerprint(entity.code)
        val partsDirectory = File(filesDir, "chunks_$codeFingerprint")
        if (!partsDirectory.exists()) {
            partsDirectory.mkdirs()
        }

        val orderedParts = manifest.parts
        val totalParts = orderedParts.size
        val totalManifestSize = manifest.size

        var lastSpeedCalcTime = System.currentTimeMillis()
        var lastBytesForSpeed = entity.downloadedBytes

        // Descarga de partes con HTTP Range para reanudar exactamente donde se quedó
        for ((idx, part) in orderedParts.withIndex()) {
            val partFile = File(partsDirectory, "%05d.part".format(part.index))
            val tempFile = File(partsDirectory, "%05d.tmp".format(part.index))

            if (partFile.exists() && partFile.length() > 0L) {
                // Parte ya completada
                val completedCount = idx + 1
                val downloadedSoFar = calculateDownloadedBytes(partsDirectory, orderedParts, part.index)
                database.downloadDao().updateProgress(
                    entity.id,
                    DownloadState.DOWNLOADING.name,
                    part.index,
                    completedCount,
                    downloadedSoFar,
                    0L,
                    0L
                )
                continue
            }

            // Descargar parte con reanudación HTTP Range
            downloadPartWithRange(
                partUrl = part.url,
                tempFile = tempFile,
                partFile = partFile,
                onProgress = { bytesInPart ->
                    val now = System.currentTimeMillis()
                    val durationSec = (now - lastSpeedCalcTime) / 1000.0
                    var speed = 0L
                    if (durationSec >= 1.0) {
                        val downloadedTotal = calculateDownloadedBytes(partsDirectory, orderedParts, part.index - 1) + bytesInPart
                        val delta = downloadedTotal - lastBytesForSpeed
                        if (delta >= 0) {
                            speed = (delta / durationSec).toLong()
                        }
                        lastBytesForSpeed = downloadedTotal
                        lastSpeedCalcTime = now
                    }

                    val downloadedTotal = calculateDownloadedBytes(partsDirectory, orderedParts, part.index - 1) + bytesInPart
                    val eta = if (speed > 0 && totalManifestSize > downloadedTotal) {
                        (totalManifestSize - downloadedTotal) / speed
                    } else 0L

                    serviceScope.launch {
                        database.downloadDao().updateProgress(
                            entity.id,
                            DownloadState.DOWNLOADING.name,
                            part.index,
                            idx,
                            downloadedTotal,
                            speed,
                            eta
                        )
                    }

                    updateProgressNotification(entity.fileName, downloadedTotal, totalManifestSize, speed, part.index, totalParts, entity.id)
                }
            )

            // Parte terminada
            val completedCount = idx + 1
            val downloadedSoFar = calculateDownloadedBytes(partsDirectory, orderedParts, part.index)
            database.downloadDao().updateProgress(
                entity.id,
                DownloadState.DOWNLOADING.name,
                part.index,
                completedCount,
                downloadedSoFar,
                0L,
                0L
            )
        }

        // Reconstrucción del archivo uniendo físicamente las partes
        val mediaStoreUri = assembleFinalFile(partsDirectory, orderedParts, entity.fileName)

        // Limpieza de partes temporales
        partsDirectory.listFiles()?.forEach { it.delete() }
        partsDirectory.delete()

        // Notificación de éxito
        val completedAt = System.currentTimeMillis()
        database.downloadDao().markCompleted(
            id = entity.id,
            status = DownloadState.COMPLETED.name,
            uri = mediaStoreUri.toString(),
            path = "Download/Chunk/${entity.fileName}",
            sha256 = manifest.sha256,
            bytes = totalManifestSize,
            completedAt = completedAt
        )

        showCompletedNotification(entity.fileName, mediaStoreUri)
    }

    private fun downloadPartWithRange(
        partUrl: String,
        tempFile: File,
        partFile: File,
        onProgress: (Long) -> Unit
    ) {
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        val requestBuilder = Request.Builder().url(partUrl)
        if (existingBytes > 0L) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful && response.code != 416) {
            response.close()
            throw IOException("Error del servidor HTTP ${response.code} descargando fragmento.")
        }

        // Si el servidor responde 416 (Range Not Satisfiable), el archivo temporal ya estaba completo o es inválido
        if (response.code == 416) {
            response.close()
            if (tempFile.exists()) {
                tempFile.renameTo(partFile)
                return
            }
        }

        val isAppend = response.code == 206 && existingBytes > 0L
        val body = response.body ?: throw IOException("Cuerpo de respuesta vacío.")

        var totalWritten = if (isAppend) existingBytes else 0L

        body.byteStream().use { input ->
            FileOutputStream(tempFile, isAppend).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    totalWritten += read
                    onProgress(totalWritten)
                }
                output.flush()
            }
        }
        response.close()

        // Renombrar a archivo de parte permanente
        if (partFile.exists()) partFile.delete()
        tempFile.renameTo(partFile)
    }

    private fun assembleFinalFile(
        partsDirectory: File,
        orderedParts: List<com.example.model.ChunkPart>,
        fileName: String
    ): Uri {
        val resolver = contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, FileUtils.getMimeType(fileName))
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Chunk")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collectionUri, contentValues)
            ?: throw IOException("No se pudo registrar el archivo en MediaStore.")

        val digest = MessageDigest.getInstance("SHA-256")
        resolver.openOutputStream(uri, "w")?.use { rawOut ->
            val digestOut = DigestOutputStream(rawOut, digest)
            val buffer = ByteArray(BUFFER_SIZE)
            for (part in orderedParts) {
                val f = File(partsDirectory, "%05d.part".format(part.index))
                if (!f.exists()) throw IOException("Falta fragmento ${part.index} para reconstruir.")
                FileInputStream(f).use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        digestOut.write(buffer, 0, read)
                    }
                }
            }
            digestOut.flush()
        } ?: throw IOException("No se pudo escribir en el destino final.")

        // Publicar archivo
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        return uri
    }

    private fun calculateDownloadedBytes(
        partsDirectory: File,
        orderedParts: List<com.example.model.ChunkPart>,
        upToPartIndex: Int
    ): Long {
        var sum = 0L
        for (part in orderedParts) {
            if (part.index <= upToPartIndex) {
                val f = File(partsDirectory, "%05d.part".format(part.index))
                if (f.exists()) {
                    sum += f.length()
                }
            }
        }
        return sum
    }

    private fun handleDownloadError(downloadId: String, e: Exception) {
        val settings = settingsManager.settings.value
        val isNetworkIssue = e is IOException || !isNetworkAvailable()

        serviceScope.launch {
            if (isNetworkIssue) {
                if (settings.autoRetry) {
                    database.downloadDao().updateStatus(
                        downloadId,
                        DownloadState.PAUSED.name,
                        "Red interrumpida. Reintentando automáticamente al volver la conexión..."
                    )
                } else {
                    database.downloadDao().updateStatus(
                        downloadId,
                        DownloadState.PAUSED.name,
                        "Descarga pausada por pérdida de red."
                    )
                }
            } else {
                val msg = e.localizedMessage ?: "Error desconocido durante la descarga."
                database.downloadDao().markFailed(downloadId, DownloadState.ERROR.name, msg, System.currentTimeMillis())
                showErrorNotification(downloadId, "Descarga fallida", msg)
            }
        }
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val settings = settingsManager.settings.value
                if (settings.autoRetry) {
                    serviceScope.launch {
                        delay(1200) // Pequeña espera para estabilización del socket
                        val pending = database.downloadDao().getUnfinishedDownloads()
                        for (item in pending) {
                            if (item.status == DownloadState.PAUSED.name && item.errorMessage?.contains("red", ignoreCase = true) == true) {
                                startOrResumeDownload(item.id)
                            }
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                // Pausar descargas activas sin perder las partes
                for ((id, job) in activeJobs) {
                    job.cancel()
                    serviceScope.launch {
                        database.downloadDao().updateStatus(id, DownloadState.PAUSED.name, "Sin conexión a internet.")
                    }
                }
                activeJobs.clear()
                releaseLocksIfIdle()
                updateNotification()
            }
        }

        try {
            cm.registerNetworkCallback(builder.build(), networkCallback!!)
        } catch (_: Exception) {}
    }

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun initLocks() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DownloadChunk:WakeLock")

            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DownloadChunk:WifiLock")
        } catch (_: Exception) {}
    }

    private fun acquireLocks() {
        try {
            wakeLock?.let { if (!it.isHeld) it.acquire(10 * 60 * 1000L) }
            wifiLock?.let { if (!it.isHeld) it.acquire() }
        } catch (_: Exception) {}
    }

    private fun releaseLocksIfIdle() {
        if (activeJobs.isEmpty()) {
            try {
                wakeLock?.let { if (it.isHeld) it.release() }
                wifiLock?.let { if (it.isHeld) it.release() }
            } catch (_: Exception) {}
        }
    }

    private fun startForegroundNotification(title: String) {
        val notification = buildProgressNotification(
            title = title,
            content = "Iniciando descarga...",
            progress = 0,
            speed = 0L,
            downloadId = null,
            isPaused = false
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_PROGRESS_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_PROGRESS_ID, notification)
            }
        } catch (_: Exception) {}
    }

    private fun updateProgressNotification(
        fileName: String,
        downloaded: Long,
        total: Long,
        speed: Long,
        partIndex: Int,
        totalParts: Int,
        downloadId: String
    ) {
        if (!settingsManager.settings.value.showProgressNotifications) return

        val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 99) else 0
        val speedStr = if (speed > 0) " • ${FileUtils.formatBytes(speed)}/s" else ""
        val content = "Parte $partIndex de $totalParts • $percent%$speedStr"

        val notification = buildProgressNotification(
            title = fileName,
            content = content,
            progress = percent,
            speed = speed,
            downloadId = downloadId,
            isPaused = false
        )

        try {
            notificationManager.notify(NOTIFICATION_PROGRESS_ID, notification)
        } catch (_: Exception) {}
    }

    private fun updateNotification() {
        if (activeJobs.isEmpty()) {
            try {
                notificationManager.cancel(NOTIFICATION_PROGRESS_ID)
            } catch (_: Exception) {}
        }
    }

    private fun buildProgressNotification(
        title: String,
        content: String,
        progress: Int,
        speed: Long,
        downloadId: String?,
        isPaused: Boolean
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_PROGRESS_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setProgress(100, progress, progress == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (downloadId != null) {
            if (isPaused) {
                val resumeIntent = PendingIntent.getService(
                    this,
                    downloadId.hashCode() + 1,
                    Intent(this, DownloadService::class.java).apply {
                        action = ACTION_RESUME
                        putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(android.R.drawable.ic_media_play, "Reanudar", resumeIntent)
            } else {
                val pauseIntent = PendingIntent.getService(
                    this,
                    downloadId.hashCode() + 2,
                    Intent(this, DownloadService::class.java).apply {
                        action = ACTION_PAUSE
                        putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(android.R.drawable.ic_media_pause, "Pausar", pauseIntent)
            }

            val cancelIntent = PendingIntent.getService(
                this,
                downloadId.hashCode() + 3,
                Intent(this, DownloadService::class.java).apply {
                    action = ACTION_CANCEL
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelIntent)
        }

        return builder.build()
    }

    private fun showCompletedNotification(fileName: String, uri: Uri) {
        val settings = settingsManager.settings.value

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            fileName.hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isApk = fileName.endsWith(".apk", ignoreCase = true)
        val actionLabel = if (isApk) "Instalar" else "Abrir"

        val builder = NotificationCompat.Builder(this, CHANNEL_COMPLETED_ID)
            .setContentTitle("Descarga completada")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_view, actionLabel, pendingOpen)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (settings.soundOnComplete) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        }

        try {
            notificationManager.notify(fileName.hashCode(), builder.build())
        } catch (_: Exception) {}

        if (settings.vibrateOnComplete) {
            triggerVibration()
        }
    }

    private fun showErrorNotification(downloadId: String, fileName: String, errorMsg: String) {
        val retryIntent = PendingIntent.getService(
            this,
            downloadId.hashCode() + 4,
            Intent(this, DownloadService::class.java).apply {
                action = ACTION_RETRY
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ERROR_ID)
            .setContentTitle("Fallo en descarga: $fileName")
            .setContentText(errorMsg)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_rotate, "Reintentar", retryIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        try {
            notificationManager.notify(downloadId.hashCode() + 10, builder.build())
        } catch (_: Exception) {}
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(200)
            }
        } catch (_: Exception) {}
    }

    private fun stopSelfIfIdle() {
        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal 1: Progreso (Baja prioridad, sin sonido)
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS_ID,
                "Descargas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de las descargas en segundo plano"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }

            // Canal 2: Completadas (Alta prioridad, con sonido)
            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED_ID,
                "Completadas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de descargas completadas con éxito"
                enableVibration(true)
                setShowBadge(true)
            }

            // Canal 3: Errores (Prioridad por defecto)
            val errorChannel = NotificationChannel(
                CHANNEL_ERROR_ID,
                "Errores",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos de fallos o interrupciones en descargas"
            }

            notificationManager.createNotificationChannels(listOf(progressChannel, completedChannel, errorChannel))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            try { cm?.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        releaseLocksIfIdle()
    }
}
