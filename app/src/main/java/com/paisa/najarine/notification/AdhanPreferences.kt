package com.paisa.najarine.notification

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isAdhanEnabledState.value = prefs.getBoolean(KEY_ADHAN_ENABLED, true)
        _isPrayerNotificationsEnabledState.value = prefs.getBoolean(KEY_PRAYER_NOTIFICATIONS_ENABLED, true)
        _selectedAdhanIdState.value = prefs.getString(KEY_SELECTED_ADHAN_ID, DEFAULT_ADHAN_ID) ?: DEFAULT_ADHAN_ID
        _selectedMadhabState.value = prefs.getString(KEY_SELECTED_MADHAB, DEFAULT_MADHAB) ?: DEFAULT_MADHAB
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
}
