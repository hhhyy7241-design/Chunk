package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.DownloadEntity
import com.example.data.DownloadRepository
import com.example.data.SettingsManager
import com.example.data.ThemeMode
import com.example.model.MoodleManifest
import com.example.parser.MoodleCodeParser
import com.example.util.BatteryUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ParseUiState {
    data object Idle : ParseUiState
    data object Validating : ParseUiState
    data class Valid(val manifest: MoodleManifest) : ParseUiState
    data class Invalid(val message: String, val detail: String? = null) : ParseUiState
}

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    private val settingsManager = SettingsManager(application)

    val settings: StateFlow<AppSettings> = settingsManager.settings

    // Room como única fuente de verdad reactiva
    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeDownloads: StateFlow<List<DownloadEntity>> = repository.activeDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedDownloads: StateFlow<List<DownloadEntity>> = repository.completedDownloads
        .stateIn(
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

    private val _showWelcomeSheet = MutableStateFlow(false)
    val showWelcomeSheet: StateFlow<Boolean> = _showWelcomeSheet.asStateFlow()

    private var parseDebounceJob: Job? = null

    init {
        // Al reabrir la app o tras cierre forzado, retoma descargas que estaban activas
        viewModelScope.launch {
            repository.resumePendingDownloadsOnStartup()
        }

        checkWelcomeSheetEligibility()
    }

    fun onCodeChanged(newCode: String) {
        _codeText.value = newCode
        parseDebounceJob?.cancel()

        val trimmed = newCode.trim()
        if (trimmed.isBlank()) {
            _parseState.value = ParseUiState.Idle
            return
        }

        // Validación automática con pequeña espera (debounce) tras escribir o pegar
        parseDebounceJob = viewModelScope.launch {
            _parseState.value = ParseUiState.Validating
            delay(350)

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

        // Aviso breve no bloqueante si la batería tiene restricciones
        checkBatteryOptimizationNotice()

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

    fun pauseDownload(id: String) {
        viewModelScope.launch {
            repository.pauseDownload(id)
        }
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch {
            repository.resumeDownload(id)
        }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch {
            repository.retryDownload(id)
        }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            repository.cancelDownload(id)
            _snackbarMessage.emit("Descarga cancelada.")
        }
    }

    fun deleteCompletedItem(id: String) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun clearAllCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
            _snackbarMessage.emit("Archivos completados eliminados.")
        }
    }

    // Lógica de Hoja de Bienvenida (Permisos y Batería)
    fun checkWelcomeSheetEligibility() {
        val s = settingsManager.settings.value
        if (s.dontShowWelcomeAgain) {
            _showWelcomeSheet.value = false
            return
        }

        if (!s.hasSeenWelcome) {
            _showWelcomeSheet.value = true
            return
        }

        // Si ya la vio, pero falta permiso y pasaron más de 3 días y NO hay descarga activa:
        val hasActive = activeDownloads.value.isNotEmpty()
        val now = System.currentTimeMillis()
        val threeDaysMs = 3 * 24 * 3600 * 1000L
        val canShowPeriodic = (now - s.lastWelcomeShownTime) > threeDaysMs

        val batteryRestricted = !BatteryUtils.isIgnoringBatteryOptimizations(getApplication())

        if (!hasActive && canShowPeriodic && batteryRestricted) {
            _showWelcomeSheet.value = true
            settingsManager.updateLastWelcomeShownTime(now)
        }
    }

    fun openWelcomeSheetManually() {
        _showWelcomeSheet.value = true
    }

    fun dismissWelcomeSheet(dontShowAgain: Boolean = false) {
        _showWelcomeSheet.value = false
        settingsManager.setHasSeenWelcome(true)
        settingsManager.updateLastWelcomeShownTime()
        if (dontShowAgain) {
            settingsManager.setDontShowWelcomeAgain(true)
        }
    }

    fun markAutoStartAcknowledged() {
        settingsManager.setAutoStartAcknowledged(true)
    }

    private fun checkBatteryOptimizationNotice() {
        val context = getApplication<Application>()
        val s = settingsManager.settings.value
        if (!s.hasWarnedBatteryRestriction && !BatteryUtils.isIgnoringBatteryOptimizations(context)) {
            settingsManager.setHasWarnedBatteryRestriction(true)
            viewModelScope.launch {
                _snackbarMessage.emit("Sugerencia: Permite la batería sin restricciones para no pausar con la pantalla apagada.")
            }
        }
    }

    // Ajustes
    fun setThemeMode(mode: ThemeMode) = settingsManager.setThemeMode(mode)
    fun setDynamicColor(enabled: Boolean) = settingsManager.setDynamicColor(enabled)
    fun setWifiOnly(enabled: Boolean) = settingsManager.setWifiOnly(enabled)
    fun setAutoRetry(enabled: Boolean) = settingsManager.setAutoRetry(enabled)
    fun setVibrateOnComplete(enabled: Boolean) = settingsManager.setVibrateOnComplete(enabled)
    fun setAutoClearOnStart(enabled: Boolean) = settingsManager.setAutoClearOnStart(enabled)
    fun setShowProgressNotifications(enabled: Boolean) = settingsManager.setShowProgressNotifications(enabled)
    fun setSoundOnComplete(enabled: Boolean) = settingsManager.setSoundOnComplete(enabled)
}
