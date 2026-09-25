package com.example.song.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Global utility for explicitly reporting caught exceptions, background failures,
 * screen states, and custom breadcrumbs to Firebase Crashlytics.
 */
object CrashTracker {

    private const val TAG = "CrashTracker"

    /**
     * Logs a breadcrumb message in Crashlytics safely.
     */
    fun log(message: String) {
        try {
            FirebaseCrashlytics.getInstance().log(message)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log breadcrumb to Crashlytics", e)
        }
    }

    /**
     * Sets a custom key-value pair in Crashlytics for user context.
     */
    fun setCustomKey(key: String, value: Any?) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            when (value) {
                is String -> crashlytics.setCustomKey(key, value)
                is Boolean -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Long -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                null -> crashlytics.setCustomKey(key, "null")
                else -> crashlytics.setCustomKey(key, value.toString())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set custom key '$key' in Crashlytics", e)
        }
    }

    /**
     * Explicitly records a non-fatal or caught exception to Crashlytics
     * with optional custom keys and breadcrumbs.
     */
    fun recordException(
        throwable: Throwable,
        breadcrumb: String? = null,
        customKeys: Map<String, Any?>? = null
    ) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()

            breadcrumb?.let { crashlytics.log(it) }

            customKeys?.forEach { (key, value) ->
                setCustomKey(key, value)
            }

            crashlytics.recordException(throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record exception to Crashlytics", e)
        }
    }

    /**
     * Tracks the current active screen / destination breadcrumb and custom key.
     */
    fun trackScreen(screenName: String) {
        setCustomKey("last_active_screen", screenName)
        log("Screen view: $screenName")
    }

    /**
     * Tracks a user action or event breadcrumb and custom key.
     */
    fun trackUserAction(action: String) {
        setCustomKey("last_user_action", action)
        log("User action: $action")
    }
}
