package com.doubleangels.nextdnsmanagement.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doubleangels.nextdnsmanagement.api.ApiException
import com.doubleangels.nextdnsmanagement.api.ConnectionStatus
import com.doubleangels.nextdnsmanagement.api.Profile
import com.doubleangels.nextdnsmanagement.data.AppContainer
import com.doubleangels.nextdnsmanagement.data.AppPreferences
import com.doubleangels.nextdnsmanagement.data.Load
import com.doubleangels.nextdnsmanagement.data.ThemeMode
import com.doubleangels.nextdnsmanagement.data.load
import com.doubleangels.nextdnsmanagement.data.userMessage
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** App-wide state: storage readiness, sign-in, profiles and appearance. */
class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppContainer.repository

    var ready by mutableStateOf(false)
        private set
    var signedIn by mutableStateOf(false)
        private set
    var profiles by mutableStateOf<Load<List<Profile>>>(Load.Loading)
        private set
    var profileId by mutableStateOf<String?>(null)
        private set
    var connection by mutableStateOf<Load<ConnectionStatus>>(Load.Loading)
        private set
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var dynamicColor by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { SharedPreferencesManager.init(application) }
            themeMode = AppPreferences.themeMode
            dynamicColor = AppPreferences.dynamicColor
            signedIn = AppPreferences.apiKey != null
            if (AppPreferences.errorReporting) SentryInitializer.initialize(application)
            ready = true
            if (signedIn) loadProfiles()
            checkConnection()
        }
    }

    val currentProfile: Profile?
        get() = (profiles as? Load.Ready)?.value?.firstOrNull { it.id == profileId }

    /** Validates [apiKey] by listing profiles, and keeps it only if that works. */
    suspend fun signIn(apiKey: String): String? {
        val previous = AppPreferences.apiKey
        AppPreferences.apiKey = apiKey.trim()
        return try {
            val list = repository.profiles()
            applyProfiles(list)
            signedIn = true
            null
        } catch (e: Exception) {
            AppPreferences.apiKey = previous
            if (e is ApiException && e.isAuthError) "invalid_key" else e.userMessage()
        }
    }

    fun signOut() {
        AppPreferences.apiKey = null
        AppPreferences.profileId = null
        profiles = Load.Loading
        profileId = null
        signedIn = false
    }

    fun loadProfiles() {
        profiles = Load.Loading
        viewModelScope.launch {
            val result = load { repository.profiles() }
            if (result is Load.Ready) applyProfiles(result.value) else profiles = result
            if (result is Load.Failed && AppPreferences.apiKey == null) signedIn = false
        }
    }

    private fun applyProfiles(list: List<Profile>) {
        profiles = Load.Ready(list)
        val saved = AppPreferences.profileId
        profileId = list.firstOrNull { it.id == saved }?.id ?: list.firstOrNull()?.id
        AppPreferences.profileId = profileId
    }

    fun selectProfile(id: String) {
        profileId = id
        AppPreferences.profileId = id
    }

    fun onProfileRenamed(id: String, name: String) {
        val list = (profiles as? Load.Ready)?.value ?: return
        profiles = Load.Ready(list.map { if (it.id == id) it.copy(name = name) else it })
    }

    fun checkConnection() {
        connection = Load.Loading
        viewModelScope.launch { connection = load { repository.connectionStatus() } }
    }

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        AppPreferences.themeMode = mode
    }

    fun updateDynamicColor(enabled: Boolean) {
        dynamicColor = enabled
        AppPreferences.dynamicColor = enabled
    }
}
