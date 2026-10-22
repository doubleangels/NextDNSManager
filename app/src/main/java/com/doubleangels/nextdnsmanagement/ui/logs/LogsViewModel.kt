package com.doubleangels.nextdnsmanagement.ui.logs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doubleangels.nextdnsmanagement.api.EntryList
import com.doubleangels.nextdnsmanagement.api.LogEntry
import com.doubleangels.nextdnsmanagement.data.AppContainer
import com.doubleangels.nextdnsmanagement.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StatusFilter(val apiValue: String?) { ALL(null), BLOCKED("blocked"), ALLOWED("allowed") }

class LogsViewModel : ViewModel() {

    private val repository = AppContainer.repository
    private var profileId: String? = null
    private var cursor: String? = null
    private var streamId: String? = null
    private var pageJob: Job? = null
    private var streamJob: Job? = null
    private var searchJob: Job? = null

    val entries = mutableStateListOf<LogEntry>()
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var endReached by mutableStateOf(false)
        private set
    var search by mutableStateOf("")
        private set
    var filter by mutableStateOf(StatusFilter.ALL)
        private set
    var live by mutableStateOf(false)
        private set

    fun show(profileId: String) {
        if (this.profileId == profileId) return
        this.profileId = profileId
        reload()
    }

    fun updateSearch(text: String) {
        search = text
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            reload()
        }
    }

    /** Applies a search immediately, e.g. when opened from a domain on the Overview. */
    fun searchNow(text: String) {
        searchJob?.cancel()
        search = text
        reload()
    }

    fun updateFilter(newFilter: StatusFilter) {
        filter = newFilter
        reload()
    }

    fun reload() {
        val id = profileId ?: return
        pageJob?.cancel()
        streamJob?.cancel()
        entries.clear()
        cursor = null
        streamId = null
        endReached = false
        error = null
        loadPage(id)
    }

    fun loadMore() {
        val id = profileId ?: return
        if (loading || endReached || error != null) return
        loadPage(id)
    }

    private fun loadPage(id: String) {
        loading = true
        pageJob = viewModelScope.launch {
            try {
                val page = repository.logs(id, cursor, search, filter.apiValue)
                // The first page's stream id lets live mode continue right after it.
                if (cursor == null) streamId = page.streamId
                entries += page.entries
                cursor = page.cursor
                endReached = page.cursor == null
                if (live) startStream(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage()
            } finally {
                loading = false
            }
        }
    }

    fun toggleLive() {
        live = !live
        val id = profileId ?: return
        if (live) startStream(id) else streamJob?.cancel()
    }

    private fun startStream(id: String) {
        if (streamJob?.isActive == true) return
        streamJob = viewModelScope.launch {
            try {
                repository.streamLogs(id, streamId).collect { entry ->
                    if (matches(entry)) {
                        entries.add(0, entry)
                        if (entries.size > MAX_ENTRIES) entries.removeAt(entries.lastIndex)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage()
                live = false
            }
        }
    }

    private fun matches(entry: LogEntry): Boolean {
        val statusOk = filter.apiValue == null || entry.status == filter.apiValue
        val searchOk = search.isBlank() || entry.domain.contains(search.trim(), ignoreCase = true)
        return statusOk && searchOk
    }

    /** Adds [domain] to the allowlist or denylist; returns an error message or null. */
    suspend fun addTo(list: EntryList, domain: String): String? {
        val id = profileId ?: return null
        return try {
            repository.addEntry(id, list, domain)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.userMessage()
        }
    }

    suspend fun clearLogs(): String? {
        val id = profileId ?: return null
        return try {
            repository.clearLogs(id)
            reload()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.userMessage()
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
    }

    private companion object {
        const val MAX_ENTRIES = 2_000
    }
}
