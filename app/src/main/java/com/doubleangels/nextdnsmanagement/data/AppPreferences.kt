package com.doubleangels.nextdnsmanagement.data

import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager

enum class ThemeMode(val value: String) {
    SYSTEM("match"),
    LIGHT("off"),
    DARK("on");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

/**
 * Typed access to the encrypted app preferences. Keys and values match the WebView
 * versions of the app (`dark_mode`, `app_lock_enable`, `sentry_enable`,
 * `last_authenticated_time`), so existing settings carry over on upgrade.
 */
object AppPreferences {
    private const val API_KEY = "api_key"
    private const val PROFILE_ID = "selected_profile"
    private const val DARK_MODE = "dark_mode"
    private const val DYNAMIC_COLOR = "dynamic_color"
    const val APP_LOCK = "app_lock_enable"
    const val SENTRY = "sentry_enable"
    private const val LAST_AUTH = "last_authenticated_time"

    val isReady: Boolean get() = SharedPreferencesManager.isInitialized()

    var apiKey: String?
        get() = SharedPreferencesManager.getString(API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) = SharedPreferencesManager.putString(API_KEY, value)

    var profileId: String?
        get() = SharedPreferencesManager.getString(PROFILE_ID, null)
        set(value) = SharedPreferencesManager.putString(PROFILE_ID, value)

    var themeMode: ThemeMode
        get() = ThemeMode.from(SharedPreferencesManager.getString(DARK_MODE, ThemeMode.SYSTEM.value))
        set(value) = SharedPreferencesManager.putString(DARK_MODE, value.value)

    var dynamicColor: Boolean
        get() = SharedPreferencesManager.getBoolean(DYNAMIC_COLOR, true)
        set(value) = SharedPreferencesManager.putBoolean(DYNAMIC_COLOR, value)

    var appLock: Boolean
        get() = SharedPreferencesManager.getBoolean(APP_LOCK, false)
        set(value) = SharedPreferencesManager.putBoolean(APP_LOCK, value)

    var errorReporting: Boolean
        get() = SharedPreferencesManager.getBoolean(SENTRY, false)
        set(value) = SharedPreferencesManager.putBoolean(SENTRY, value)

    var lastAuthenticated: Long
        get() = SharedPreferencesManager.getLong(LAST_AUTH, 0L)
        set(value) = SharedPreferencesManager.putLong(LAST_AUTH, value)

    /** App lock is only offered when settings are stored encrypted. */
    val appLockAvailable: Boolean get() = SharedPreferencesManager.isUsingEncryptedStorage()
}
