package com.paisa.najarine.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralized Firebase Analytics & Crashlytics manager for Paisa.
 */
object PaisaAnalytics {

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            crashlytics = FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
            }
            Log.d("PaisaAnalytics", "Firebase Analytics & Crashlytics initialized successfully.")
        } catch (e: Exception) {
            Log.e("PaisaAnalytics", "Failed to initialize Firebase Analytics / Crashlytics", e)
        }
    }

    /**
     * Log user or app navigation events
     */
    fun logScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            crashlytics?.log("Screen viewed: $screenName")
        } catch (_: Exception) {}
    }

    /**
     * Log custom business events (e.g. transaction created, wallet added, tilawat played)
     */
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        try {
            val bundle = Bundle().apply {
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
            crashlytics?.log("Event: $eventName with params: $params")
        } catch (_: Exception) {}
    }

    /**
     * Set user ID for both Analytics and Crashlytics when authenticated
     */
    fun setUserId(userId: String?) {
        try {
            firebaseAnalytics?.setUserId(userId)
            if (!userId.isNullOrBlank()) {
                crashlytics?.setUserId(userId)
            }
        } catch (_: Exception) {}
    }

    /**
     * Log non-fatal handled exceptions to Firebase Crashlytics
     */
    fun recordNonFatalException(throwable: Throwable, contextTag: String = "AppException") {
        try {
            crashlytics?.setCustomKey("context_tag", contextTag)
            crashlytics?.recordException(throwable)
            Log.w("PaisaCrashlytics", "Recorded non-fatal exception [$contextTag]: ${throwable.message}")
        } catch (_: Exception) {}
    }

    /**
     * Set custom key-value for Crashlytics diagnostics
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (_: Exception) {}
    }
}
