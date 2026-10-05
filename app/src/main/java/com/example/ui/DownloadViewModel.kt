package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.data.AppSettings
import com.example.data.DownloadEntity
import com.example.data.DownloadRepository
import com.example.data.SettingsManager
import com.example.data.ThemeMode
import com.example.model.ChunkPart
import com.example.model.DownloadState
import com.example.model.MoodleManifest
import com.example.parser.MoodleCodeParser
import com.example.worker.MoodleDownloadWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ParseUiState {
    data object Idle : ParseUiState
    data object Validating : ParseUiState
    data class Valid(val manifest: MoodleManifest) : ParseUiState
    data class Invalid(val message: String, val detail: String? = null) : ParseUiState
}

data class ActiveDownloadUi(
    val id: String,
    val workId: String?,
    val fileName: String,
    val totalParts: Int,
    val currentPartIndex: Int,
    val percent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBps: Long,
    val etaSeconds: Long,
    val state: DownloadState,
    val statusMessage: String
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    private val settingsManager = SettingsManager(application)

    val settings: StateFlow<AppSettings> = settingsManager.settings

    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeWorkInfos: StateFlow<List<WorkInfo>> = repository.getWorkInfosByTagFlow("moodle_download")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeDownloadsUi: StateFlow<List<ActiveDownloadUi>> = combine(allDownloads, activeWorkInfos) { downloads, workInfos ->
        val workInfoMap = workInfos.associateBy { it.id.toString() }

        downloads.filter { entity ->
            entity.status !in listOf(DownloadState.COMPLETED.name, DownloadState.FAILED.name, DownloadState.CANCELLED.name)
        }.map { entity ->
            val workInfo = entity.workId?.let { workInfoMap[it] }
            val progress = workInfo?.progress

            val percent = progress?.getInt(MoodleDownloadWorker.PROGRESS_PERCENT, 0)
                ?: if (entity.totalBytes > 0) ((entity.downloadedBytes * 100) / entity.totalBytes).toInt() else 0

            val downloadedBytes = progress?.getLong(MoodleDownloadWorker.PROGRESS_BYTES, entity.downloadedBytes) ?: entity.downloadedBytes
            val totalBytes = progress?.getLong(MoodleDownloadWorker.PROGRESS_TOTAL_BYTES, entity.totalBytes) ?: entity.totalBytes
            val partIndex = progress?.getInt(MoodleDownloadWorker.PROGRESS_PART_INDEX, entity.currentPart) ?: entity.currentPart
            val totalParts = progress?.getInt(MoodleDownloadWorker.PROGRESS_TOTAL_PARTS, entity.totalParts) ?: entity.totalParts
            val speedBps = progress?.getLong(MoodleDownloadWorker.PROGRESS_SPEED, 0L) ?: 0L
            val etaSeconds = progress?.getLong(MoodleDownloadWorker.PROGRESS_ETA_SECONDS, 0L) ?: 0L
            val stateName = progress?.getString(MoodleDownloadWorker.PROGRESS_STATE) ?: entity.status
            val statusMsg = progress?.getString(MoodleDownloadWorker.PROGRESS_STATUS_MSG) ?: "En progreso..."

            val parsedState = try {
                DownloadState.valueOf(stateName)
            } catch (_: Exception) {
                DownloadState.DOWNLOADING_PART
            }

            ActiveDownloadUi(
                id = entity.id,
                workId = entity.workId,
                fileName = entity.fileName,
                totalParts = totalParts,
                currentPartIndex = partIndex,
                percent = percent,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                speedBps = speedBps,
                etaSeconds = etaSeconds,
                state = parsedState,
                statusMessage = statusMsg
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _codeText = MutableStateFlow("")
    val codeText: StateFlow<String> = _codeText.asStateFlow()

    private val _parseState = MutableStateFlow<ParseUiState>(ParseUiState.Idle)
    val parseState: StateFlow<ParseUiState> = _parseState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun onCodeChanged(newCode: String) {
        _codeText.value = newCode
        val trimmed = newCode.trim()
        if (trimmed.isBlank()) {
            _parseState.value = ParseUiState.Idle
        } else if (trimmed.startsWith("https://5.4.3.2.1:", ignoreCase = true)) {
            // Auto-validar instantáneamente en cuanto se detecta el prefijo
            when (val result = MoodleCodeParser.parse(trimmed)) {
                is MoodleCodeParser.ParseResult.Success -> {
                    _parseState.value = ParseUiState.Valid(result.manifest)
                }
                is MoodleCodeParser.ParseResult.Error -> {
                    _parseState.value = ParseUiState.Invalid(result.message, result.detail)
                }
            }
        }
    }

    fun validateCurrentCode() {
        val trimmed = _codeText.value.trim()
        if (trimmed.isEmpty()) {
            _parseState.value = ParseUiState.Invalid("El código está vacío.")
            return
        }

        _parseState.value = ParseUiState.Validating
        when (val result = MoodleCodeParser.parse(trimmed)) {
            is MoodleCodeParser.ParseResult.Success -> {
                _parseState.value = ParseUiState.Valid(result.manifest)
            }
            is MoodleCodeParser.ParseResult.Error -> {
                _parseState.value = ParseUiState.Invalid(result.message, result.detail)
            }
        }
    }

    fun startDownload(customFileName: String? = null) {
        val code = _codeText.value.trim()
        if (code.isEmpty()) {
            viewModelScope.launch {
                _snackbarMessage.emit("Introduce o pega un código de Moodle primero.")
            }
            return
        }

        var currentParse = _parseState.value
        if (currentParse !is ParseUiState.Valid) {
            when (val result = MoodleCodeParser.parse(code)) {
                is MoodleCodeParser.ParseResult.Success -> {
                    _parseState.value = ParseUiState.Valid(result.manifest)
                    currentParse = ParseUiState.Valid(result.manifest)
                }
                is MoodleCodeParser.ParseResult.Error -> {
                    _parseState.value = ParseUiState.Invalid(result.message, result.detail)
                    viewModelScope.launch {
                        _snackbarMessage.emit(result.message)
                    }
                    return
                }
            }
        }

        val manifest = (currentParse as ParseUiState.Valid).manifest

        viewModelScope.launch {
            try {
                repository.enqueueDownload(code, manifest, customFileName)
                _snackbarMessage.emit("Descarga iniciada: ${manifest.filename}")
                if (settingsManager.settings.value.autoClearOnStart) {
                    _codeText.value = ""
                    _parseState.value = ParseUiState.Idle
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Error al iniciar la descarga: ${e.message}")
            }
        }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            repository.cancelDownload(id)
            _snackbarMessage.emit("Descarga cancelada.")
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _snackbarMessage.emit("Historial limpiado.")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsManager.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        settingsManager.setDynamicColor(enabled)
    }

    fun setVibrateOnComplete(enabled: Boolean) {
        settingsManager.setVibrateOnComplete(enabled)
    }

    fun setAutoClearOnStart(enabled: Boolean) {
        settingsManager.setAutoClearOnStart(enabled)
    }

    fun setWifiOnly(enabled: Boolean) {
        settingsManager.setWifiOnly(enabled)
    }

    fun setAutoRetry(enabled: Boolean) {
        settingsManager.setAutoRetry(enabled)
    }
}
