package com.paisa.najarine.notification

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdhanPreferences {
    private const val PREFS_NAME = "paisa_adhan_preferences"
    private const val KEY_ADHAN_ENABLED = "key_adhan_audio_enabled"
    private const val KEY_SELECTED_ADHAN_ID = "key_selected_adhan_id"
    private const val KEY_SELECTED_ADHAN_NAME = "key_selected_adhan_name"
    private const val KEY_SELECTED_ADHAN_URL = "key_selected_adhan_url"
    private const val KEY_SELECTED_MADHAB = "key_selected_madhab"

    const val DEFAULT_ADHAN_ID = "makkah"
    const val DEFAULT_ADHAN_NAME = "মক্কা শরিফ (Makkah Al-Mukarramah)"
    const val DEFAULT_ADHAN_URL = "https://cdn.aladhan.com/audio/adhans/a1.mp3"
    const val DEFAULT_MADHAB = "Hanafi"

    private val _isAdhanEnabledState = MutableStateFlow(true)
    val isAdhanEnabledState: StateFlow<Boolean> = _isAdhanEnabledState.asStateFlow()

    private val _selectedAdhanIdState = MutableStateFlow(DEFAULT_ADHAN_ID)
    val selectedAdhanIdState: StateFlow<String> = _selectedAdhanIdState.asStateFlow()

    private val _selectedMadhabState = MutableStateFlow(DEFAULT_MADHAB)
    val selectedMadhabState: StateFlow<String> = _selectedMadhabState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isAdhanEnabledState.value = prefs.getBoolean(KEY_ADHAN_ENABLED, true)
        _selectedAdhanIdState.value = prefs.getString(KEY_SELECTED_ADHAN_ID, DEFAULT_ADHAN_ID) ?: DEFAULT_ADHAN_ID
        _selectedMadhabState.value = prefs.getString(KEY_SELECTED_MADHAB, DEFAULT_MADHAB) ?: DEFAULT_MADHAB
    }

    fun isAdhanEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ADHAN_ENABLED, true)
        _isAdhanEnabledState.value = enabled
        return enabled
    }

    fun setAdhanEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ADHAN_ENABLED, enabled).apply()
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
        prefs.edit()
            .putString(KEY_SELECTED_ADHAN_ID, id)
            .putString(KEY_SELECTED_ADHAN_NAME, name)
            .putString(KEY_SELECTED_ADHAN_URL, url)
            .apply()
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
        prefs.edit().putString(KEY_SELECTED_MADHAB, madhab).apply()
        _selectedMadhabState.value = madhab
    }
}
