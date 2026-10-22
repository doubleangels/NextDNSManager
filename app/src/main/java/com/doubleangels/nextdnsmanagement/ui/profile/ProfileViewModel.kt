package com.doubleangels.nextdnsmanagement.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doubleangels.nextdnsmanagement.api.EntryList
import com.doubleangels.nextdnsmanagement.api.NextDnsRepository
import com.doubleangels.nextdnsmanagement.api.Rewrite
import com.doubleangels.nextdnsmanagement.api.SettingsSection
import com.doubleangels.nextdnsmanagement.data.AppContainer
import com.doubleangels.nextdnsmanagement.data.EntryListController
import com.doubleangels.nextdnsmanagement.data.Load
import com.doubleangels.nextdnsmanagement.data.load
import com.doubleangels.nextdnsmanagement.data.userMessage
import com.doubleangels.nextdnsmanagement.data.valueOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ProfileViewModel : ViewModel() {

    private val repository = AppContainer.repository
    private var profileId: String? = null

    var message by mutableStateOf<String?>(null)

    var sections by mutableStateOf<Map<SettingsSection, Load<JsonObject>>>(emptyMap())
        private set
    var rewrites by mutableStateOf<Load<List<Rewrite>>>(Load.Loading)
        private set

    private val onError: (String) -> Unit = { message = it }
    val tlds = EntryListController(viewModelScope, EntryList.BLOCKED_TLDS, onError)
    val blocklists = EntryListController(viewModelScope, EntryList.BLOCKLISTS, onError)
    val natives = EntryListController(viewModelScope, EntryList.NATIVE_TRACKERS, onError)
    val services = EntryListController(viewModelScope, EntryList.SERVICES, onError)
    val categories = EntryListController(viewModelScope, EntryList.CATEGORIES, onError)
    private val lists = listOf(tlds, blocklists, natives, services, categories)

    fun show(profileId: String) {
        if (this.profileId == profileId) return
        this.profileId = profileId
        reload()
    }

    fun reload() {
        val id = profileId ?: return
        sections = SettingsSection.entries.associateWith { Load.Loading }
        SettingsSection.entries.forEach { section ->
            viewModelScope.launch {
                val result = load { repository.section(id, section) }
                sections = sections + (section to result)
            }
        }
        lists.forEach { it.show(id, force = true) }
        loadRewrites()
    }

    private fun loadRewrites() {
        val id = profileId ?: return
        rewrites = Load.Loading
        viewModelScope.launch { rewrites = load { repository.rewrites(id) } }
    }

    /** Sets the value at [path] (e.g. `logs.drop.ip`) in [section], optimistically. */
    fun update(section: SettingsSection, path: List<String>, value: JsonElement) {
        val id = profileId ?: return
        val current = sections[section]?.valueOrNull ?: return
        sections = sections + (section to Load.Ready(current.withValue(path, value)))
        viewModelScope.launch {
            try {
                repository.updateSection(id, section, NextDnsRepository.nested(path, value))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sections = sections + (section to Load.Ready(current))
                message = e.userMessage()
            }
        }
    }

    fun toggle(section: SettingsSection, path: List<String>, enabled: Boolean) =
        update(section, path, JsonPrimitive(enabled))

    fun addRewrite(name: String, content: String) = mutateRewrites { repository.addRewrite(it, name.trim(), content.trim()) }

    fun removeRewrite(rewrite: Rewrite) = mutateRewrites { repository.removeRewrite(it, rewrite.id) }

    private fun mutateRewrites(call: suspend (String) -> Unit) {
        val id = profileId ?: return
        viewModelScope.launch {
            try {
                call(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.userMessage()
            }
            loadRewrites()
        }
    }

    /** Renames the profile; returns the error message, or null on success. */
    suspend fun rename(name: String): String? {
        val id = profileId ?: return null
        return try {
            repository.renameProfile(id, name.trim())
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.userMessage()
        }
    }
}

/** Returns a copy with [value] at [path], creating intermediate objects as needed. */
private fun JsonObject.withValue(path: List<String>, value: JsonElement): JsonObject {
    val key = path.first()
    val newValue = if (path.size == 1) value else ((this[key] as? JsonObject) ?: JsonObject(emptyMap())).withValue(path.drop(1), value)
    return JsonObject(this + (key to newValue))
}
