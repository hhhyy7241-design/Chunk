package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val wifiOnly: Boolean = false,
    val autoRetry: Boolean = true,
    val vibrateOnComplete: Boolean = true,
    val autoClearOnStart: Boolean = true,
    val showProgressNotifications: Boolean = true,
    val soundOnComplete: Boolean = true,
    val hasSeenWelcome: Boolean = false,
    val dontShowWelcomeAgain: Boolean = false,
    val lastWelcomeShownTime: Long = 0L,
    val autoStartAcknowledged: Boolean = false,
    val hasWarnedBatteryRestriction: Boolean = false
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("download_chunk_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val theme = try { ThemeMode.valueOf(themeStr) } catch (_: Exception) { ThemeMode.SYSTEM }
        val dynamic = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        val wifi = prefs.getBoolean(KEY_WIFI_ONLY, false)
        val retry = prefs.getBoolean(KEY_AUTO_RETRY, true)
        val vibrate = prefs.getBoolean(KEY_VIBRATE, true)
        val autoClear = prefs.getBoolean(KEY_AUTO_CLEAR, true)
        val showNotifs = prefs.getBoolean(KEY_SHOW_PROGRESS_NOTIFS, true)
        val sound = prefs.getBoolean(KEY_SOUND_ON_COMPLETE, true)
        val seenWelcome = prefs.getBoolean(KEY_SEEN_WELCOME, false)
        val dontShowWelcome = prefs.getBoolean(KEY_DONT_SHOW_WELCOME, false)
        val lastWelcome = prefs.getLong(KEY_LAST_WELCOME_TIME, 0L)
        val autoStartAck = prefs.getBoolean(KEY_AUTO_START_ACK, false)
        val warnedBattery = prefs.getBoolean(KEY_WARNED_BATTERY, false)

        return AppSettings(
            themeMode = theme,
            dynamicColor = dynamic,
            wifiOnly = wifi,
            autoRetry = retry,
            vibrateOnComplete = vibrate,
            autoClearOnStart = autoClear,
            showProgressNotifications = showNotifs,
            soundOnComplete = sound,
            hasSeenWelcome = seenWelcome,
            dontShowWelcomeAgain = dontShowWelcome,
            lastWelcomeShownTime = lastWelcome,
            autoStartAcknowledged = autoStartAck,
            hasWarnedBatteryRestriction = warnedBattery
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _settings.value = _settings.value.copy(dynamicColor = enabled)
    }

    fun setWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()
        _settings.value = _settings.value.copy(wifiOnly = enabled)
    }

    fun setAutoRetry(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RETRY, enabled).apply()
        _settings.value = _settings.value.copy(autoRetry = enabled)
    }

    fun setVibrateOnComplete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        _settings.value = _settings.value.copy(vibrateOnComplete = enabled)
    }

    fun setAutoClearOnStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CLEAR, enabled).apply()
        _settings.value = _settings.value.copy(autoClearOnStart = enabled)
    }

    fun setShowProgressNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_PROGRESS_NOTIFS, enabled).apply()
        _settings.value = _settings.value.copy(showProgressNotifications = enabled)
    }

    fun setSoundOnComplete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ON_COMPLETE, enabled).apply()
        _settings.value = _settings.value.copy(soundOnComplete = enabled)
    }

    fun setHasSeenWelcome(seen: Boolean) {
        prefs.edit().putBoolean(KEY_SEEN_WELCOME, seen).apply()
        _settings.value = _settings.value.copy(hasSeenWelcome = seen)
    }

    fun setDontShowWelcomeAgain(dontShow: Boolean) {
        prefs.edit().putBoolean(KEY_DONT_SHOW_WELCOME, dontShow).apply()
        _settings.value = _settings.value.copy(dontShowWelcomeAgain = dontShow)
    }

    fun updateLastWelcomeShownTime(time: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_WELCOME_TIME, time).apply()
        _settings.value = _settings.value.copy(lastWelcomeShownTime = time)
    }

    fun setAutoStartAcknowledged(acknowledged: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_ACK, acknowledged).apply()
        _settings.value = _settings.value.copy(autoStartAcknowledged = acknowledged)
    }

    fun setHasWarnedBatteryRestriction(warned: Boolean) {
        prefs.edit().putBoolean(KEY_WARNED_BATTERY, warned).apply()
        _settings.value = _settings.value.copy(hasWarnedBatteryRestriction = warned)
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_AUTO_RETRY = "key_auto_retry"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_AUTO_CLEAR = "key_auto_clear"
        private const val KEY_SHOW_PROGRESS_NOTIFS = "key_show_progress_notifs"
        private const val KEY_SOUND_ON_COMPLETE = "key_sound_on_complete"
        private const val KEY_SEEN_WELCOME = "key_seen_welcome"
        private const val KEY_DONT_SHOW_WELCOME = "key_dont_show_welcome"
        private const val KEY_LAST_WELCOME_TIME = "key_last_welcome_time"
        private const val KEY_AUTO_START_ACK = "key_auto_start_ack"
        private const val KEY_WARNED_BATTERY = "key_warned_battery"
    }
}
