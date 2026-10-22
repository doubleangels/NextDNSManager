package com.doubleangels.nextdnsmanagement.ui.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doubleangels.nextdnsmanagement.api.Breakdown
import com.doubleangels.nextdnsmanagement.api.Series
import com.doubleangels.nextdnsmanagement.api.Stat
import com.doubleangels.nextdnsmanagement.api.TimeRange
import com.doubleangels.nextdnsmanagement.data.AppContainer
import com.doubleangels.nextdnsmanagement.data.Load
import com.doubleangels.nextdnsmanagement.data.load
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Everything on the Overview screen. Each section loads, and can fail, on its own. */
data class OverviewState(
    val totals: Load<Map<String, Long>> = Load.Loading,
    val series: Load<Series> = Load.Loading,
    val topDomains: Load<List<Stat>> = Load.Loading,
    val blockedDomains: Load<List<Stat>> = Load.Loading,
    val reasons: Load<List<Stat>> = Load.Loading,
    val devices: Load<List<Stat>> = Load.Loading,
    val protocols: Load<List<Stat>> = Load.Loading,
    val encryption: Load<List<Stat>> = Load.Loading,
    val dnssec: Load<List<Stat>> = Load.Loading,
    val ipVersions: Load<List<Stat>> = Load.Loading,
    val queryTypes: Load<List<Stat>> = Load.Loading,
    val countries: Load<List<Stat>> = Load.Loading,
    val companies: Load<List<Stat>> = Load.Loading,
)

class OverviewViewModel : ViewModel() {

    private val repository = AppContainer.repository
    private var job: Job? = null
    private var loadedFor: Pair<String, TimeRange>? = null

    var range by mutableStateOf(TimeRange.WEEK)
        private set
    var state by mutableStateOf(OverviewState())
        private set
    var refreshing by mutableStateOf(false)
        private set

    fun show(profileId: String) {
        if (loadedFor != profileId to range) refresh(profileId)
    }

    fun selectRange(profileId: String, newRange: TimeRange) {
        range = newRange
        refresh(profileId)
    }

    fun refresh(profileId: String, pullToRefresh: Boolean = false) {
        loadedFor = profileId to range
        job?.cancel()
        refreshing = pullToRefresh
        if (!pullToRefresh) state = OverviewState()
        val r = range
        job = viewModelScope.launch {
            listOf(
                section({ repository.statusTotals(profileId, r) }) { copy(totals = it) },
                section({ repository.statusSeries(profileId, r) }) { copy(series = it) },
                section({ repository.breakdown(profileId, Breakdown.DOMAINS, r) }) { copy(topDomains = it) },
                section({ repository.breakdown(profileId, Breakdown.DOMAINS, r, status = "blocked") }) {
                    copy(blockedDomains = it)
                },
                section({ repository.breakdown(profileId, Breakdown.REASONS, r) }) { copy(reasons = it) },
                section({ repository.breakdown(profileId, Breakdown.DEVICES, r) }) { copy(devices = it) },
                section({ repository.breakdown(profileId, Breakdown.PROTOCOLS, r) }) { copy(protocols = it) },
                section({ repository.breakdown(profileId, Breakdown.ENCRYPTION, r) }) { copy(encryption = it) },
                section({ repository.breakdown(profileId, Breakdown.DNSSEC, r) }) { copy(dnssec = it) },
                section({ repository.breakdown(profileId, Breakdown.IP_VERSIONS, r) }) { copy(ipVersions = it) },
                section({ repository.breakdown(profileId, Breakdown.QUERY_TYPES, r) }) { copy(queryTypes = it) },
                section({ repository.breakdown(profileId, Breakdown.COUNTRIES, r) }) { copy(countries = it) },
                section({ repository.breakdown(profileId, Breakdown.COMPANIES, r) }) { copy(companies = it) },
            ).forEach { it.join() }
            refreshing = false
        }
    }

    /** Loads one section in parallel with the others and stores its result. */
    private fun <T> CoroutineScope.section(
        block: suspend () -> T,
        store: OverviewState.(Load<T>) -> OverviewState,
    ): Job = launch {
        val result = load(block)
        state = state.store(result)
    }
}
