package com.doubleangels.nextdnsmanagement.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.doubleangels.nextdnsmanagement.api.DomainEntry
import com.doubleangels.nextdnsmanagement.api.EntryList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Loads and edits one `{id, active}` list of a profile. Edits are applied to the UI
 * right away and rolled back if the API call fails.
 */
class EntryListController(
    private val scope: CoroutineScope,
    val list: EntryList,
    private val onError: (String) -> Unit,
) {
    private val repository = AppContainer.repository
    private var profileId: String? = null

    var entries by mutableStateOf<Load<List<DomainEntry>>>(Load.Loading)
        private set

    fun show(profileId: String, force: Boolean = false) {
        if (!force && this.profileId == profileId && entries is Load.Ready) return
        this.profileId = profileId
        entries = Load.Loading
        scope.launch { entries = load { repository.entries(profileId, list) } }
    }

    fun reload() {
        profileId?.let { show(it, force = true) }
    }

    fun add(rawId: String) {
        val id = rawId.trim().lowercase()
        val profile = profileId ?: return
        val current = entries.valueOrNull ?: return
        if (id.isEmpty() || current.any { it.id == id }) return
        entries = Load.Ready(listOf(DomainEntry(id, true)) + current)
        mutate({ repository.addEntry(profile, list, id) }) { entries = Load.Ready(current) }
    }

    fun setActive(id: String, active: Boolean) {
        val profile = profileId ?: return
        val current = entries.valueOrNull ?: return
        entries = Load.Ready(current.map { if (it.id == id) it.copy(active = active) else it })
        mutate({ repository.setEntryActive(profile, list, id, active) }) { entries = Load.Ready(current) }
    }

    fun remove(id: String) {
        val profile = profileId ?: return
        val current = entries.valueOrNull ?: return
        entries = Load.Ready(current.filterNot { it.id == id })
        mutate({ repository.removeEntry(profile, list, id) }) { entries = Load.Ready(current) }
    }

    private fun mutate(call: suspend () -> Unit, rollback: () -> Unit) {
        scope.launch {
            try {
                call()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                rollback()
                onError(e.userMessage())
            }
        }
    }
}
