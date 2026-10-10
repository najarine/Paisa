package com.paisa.najarine.notification

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object AdhanPreferences {
    private const val PREFS_NAME = "paisa_adhan_preferences"
    private const val KEY_ADHAN_ENABLED = "key_adhan_audio_enabled"
    private const val KEY_PRAYER_NOTIFICATIONS_ENABLED = "key_prayer_notifications_enabled"
    private const val KEY_SELECTED_ADHAN_ID = "key_selected_adhan_id"
    private const val KEY_SELECTED_ADHAN_NAME = "key_selected_adhan_name"
    private const val KEY_SELECTED_ADHAN_URL = "key_selected_adhan_url"
    private const val KEY_SELECTED_MADHAB = "key_selected_madhab"

    const val DEFAULT_ADHAN_ID = "makkah"
    const val DEFAULT_ADHAN_NAME = "মক্কা শরিফ (Makkah Al-Mukarramah)"
    const val DEFAULT_ADHAN_URL = "https://cdn.aladhan.com/audio/adhans/a1.mp3"
    const val DEFAULT_MADHAB = "Hanafi"
    private const val KEY_LAST_LATITUDE = "key_last_latitude"
    private const val KEY_LAST_LONGITUDE = "key_last_longitude"
    private const val KEY_LAST_LOCATION_NAME = "key_last_location_name"
    private const val KEY_CLOCK_FORMAT_12H = "key_clock_format_12h"
    private const val KEY_HOURLY_QURAN_NOTIF_ENABLED = "key_hourly_quran_notif_enabled"
    private const val KEY_HOURLY_HADITH_NOTIF_ENABLED = "key_hourly_hadith_notif_enabled"

    fun saveLastKnownLocation(context: Context, latitude: Double, longitude: Double, displayName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_LAST_LATITUDE, latitude.toString())
            putString(KEY_LAST_LONGITUDE, longitude.toString())
            putString(KEY_LAST_LOCATION_NAME, displayName)
        }
    }

    fun getLastKnownCoordinates(context: Context): Triple<Double, Double, String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val latStr = prefs.getString(KEY_LAST_LATITUDE, null) ?: return null
        val lonStr = prefs.getString(KEY_LAST_LONGITUDE, null) ?: return null
        val lat = latStr.toDoubleOrNull() ?: return null
        val lon = lonStr.toDoubleOrNull() ?: return null
        val name = prefs.getString(KEY_LAST_LOCATION_NAME, "") ?: ""
        return Triple(lat, lon, name)
    }

    private val _isAdhanEnabledState = MutableStateFlow(true)
    val isAdhanEnabledState: StateFlow<Boolean> = _isAdhanEnabledState.asStateFlow()

    private val _isPrayerNotificationsEnabledState = MutableStateFlow(true)
    val isPrayerNotificationsEnabledState: StateFlow<Boolean> = _isPrayerNotificationsEnabledState.asStateFlow()

    private val _selectedAdhanIdState = MutableStateFlow(DEFAULT_ADHAN_ID)
    val selectedAdhanIdState: StateFlow<String> = _selectedAdhanIdState.asStateFlow()

    private val _selectedMadhabState = MutableStateFlow(DEFAULT_MADHAB)
    val selectedMadhabState: StateFlow<String> = _selectedMadhabState.asStateFlow()

    private val _is12HourFormatState = MutableStateFlow(true)
    val is12HourFormatState: StateFlow<Boolean> = _is12HourFormatState.asStateFlow()

    private val _isHourlyQuranNotificationEnabledState = MutableStateFlow(true)
    val isHourlyQuranNotificationEnabledState: StateFlow<Boolean> = _isHourlyQuranNotificationEnabledState.asStateFlow()

    private val _isHourlyHadithNotificationEnabledState = MutableStateFlow(true)
    val isHourlyHadithNotificationEnabledState: StateFlow<Boolean> = _isHourlyHadithNotificationEnabledState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isAdhanEnabledState.value = prefs.getBoolean(KEY_ADHAN_ENABLED, true)
        _isPrayerNotificationsEnabledState.value = prefs.getBoolean(KEY_PRAYER_NOTIFICATIONS_ENABLED, true)
        _selectedAdhanIdState.value = prefs.getString(KEY_SELECTED_ADHAN_ID, DEFAULT_ADHAN_ID) ?: DEFAULT_ADHAN_ID
        _selectedMadhabState.value = prefs.getString(KEY_SELECTED_MADHAB, DEFAULT_MADHAB) ?: DEFAULT_MADHAB
        _is12HourFormatState.value = prefs.getBoolean(KEY_CLOCK_FORMAT_12H, true)
        _isHourlyQuranNotificationEnabledState.value = prefs.getBoolean(KEY_HOURLY_QURAN_NOTIF_ENABLED, true)
        _isHourlyHadithNotificationEnabledState.value = prefs.getBoolean(KEY_HOURLY_HADITH_NOTIF_ENABLED, true)
    }

    fun isHourlyQuranNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_HOURLY_QURAN_NOTIF_ENABLED, true)
        _isHourlyQuranNotificationEnabledState.value = enabled
        return enabled
    }

    fun setHourlyQuranNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOURLY_QURAN_NOTIF_ENABLED, enabled) }
        _isHourlyQuranNotificationEnabledState.value = enabled
    }

    fun isHourlyHadithNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_HOURLY_HADITH_NOTIF_ENABLED, true)
        _isHourlyHadithNotificationEnabledState.value = enabled
        return enabled
    }

    fun setHourlyHadithNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOURLY_HADITH_NOTIF_ENABLED, enabled) }
        _isHourlyHadithNotificationEnabledState.value = enabled
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }
    }

    fun getBatteryOptimizationSettingsIntent(context: Context): android.content.Intent {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        }
    }

    fun isPrayerNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_PRAYER_NOTIFICATIONS_ENABLED, true)
        _isPrayerNotificationsEnabledState.value = enabled
        return enabled
    }

    fun setPrayerNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_PRAYER_NOTIFICATIONS_ENABLED, enabled) }
        _isPrayerNotificationsEnabledState.value = enabled
    }

    fun isAdhanEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ADHAN_ENABLED, true)
        _isAdhanEnabledState.value = enabled
        return enabled
    }

    fun setAdhanEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_ADHAN_ENABLED, enabled) }
        _isAdhanEnabledState.value = enabled
    }

    fun getSelectedAdhanId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_SELECTED_ADHAN_ID, DEFAULT_ADHAN_ID) ?: DEFAULT_ADHAN_ID
        _selectedAdhanIdState.value = id
        return id
    }

    fun getSelectedAdhanUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_ADHAN_URL, DEFAULT_ADHAN_URL) ?: DEFAULT_ADHAN_URL
    }

    fun getSelectedAdhanName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_ADHAN_NAME, DEFAULT_ADHAN_NAME) ?: DEFAULT_ADHAN_NAME
    }

    fun setSelectedAdhan(context: Context, id: String, name: String, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_SELECTED_ADHAN_ID, id)
            putString(KEY_SELECTED_ADHAN_NAME, name)
            putString(KEY_SELECTED_ADHAN_URL, url)
        }
        _selectedAdhanIdState.value = id
    }

    fun getSelectedMadhab(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val madhab = prefs.getString(KEY_SELECTED_MADHAB, DEFAULT_MADHAB) ?: DEFAULT_MADHAB
        _selectedMadhabState.value = madhab
        return madhab
    }

    fun setSelectedMadhab(context: Context, madhab: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_SELECTED_MADHAB, madhab) }
        _selectedMadhabState.value = madhab
    }

    fun is12HourFormat(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val is12h = prefs.getBoolean(KEY_CLOCK_FORMAT_12H, true)
        _is12HourFormatState.value = is12h
        return is12h
    }

    fun set12HourFormat(context: Context, is12h: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_CLOCK_FORMAT_12H, is12h) }
        _is12HourFormatState.value = is12h
    }

    fun formatTo12Hour(time24: String, is12Hour: Boolean): String {
        if (!is12Hour) return time24
        return try {
            val clean = time24.split(" ")[0].trim()
            val parts = clean.split(":")
            if (parts.size >= 2) {
                var hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1]
                val ampm = if (hour >= 12) "PM" else "AM"
                if (hour > 12) hour -= 12
                if (hour == 0) hour = 12
                String.format(Locale.US, "%02d:%s %s", hour, minute, ampm)
            } else {
                time24
            }
        } catch (_: Exception) {
            time24
        }
    }
}
