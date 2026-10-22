package com.doubleangels.nextdnsmanagement.ui.overview

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doubleangels.nextdnsmanagement.R
import com.doubleangels.nextdnsmanagement.api.ConnectionStatus
import com.doubleangels.nextdnsmanagement.api.NextDnsRepository
import com.doubleangels.nextdnsmanagement.api.Series
import com.doubleangels.nextdnsmanagement.api.Stat
import com.doubleangels.nextdnsmanagement.api.TimeRange
import com.doubleangels.nextdnsmanagement.data.Load
import com.doubleangels.nextdnsmanagement.data.valueOrNull
import com.doubleangels.nextdnsmanagement.ui.components.BarList
import com.doubleangels.nextdnsmanagement.ui.components.BarRow
import com.doubleangels.nextdnsmanagement.ui.components.ChartLine
import com.doubleangels.nextdnsmanagement.ui.components.DonutChart
import com.doubleangels.nextdnsmanagement.ui.components.EmptyState
import com.doubleangels.nextdnsmanagement.ui.components.ErrorState
import com.doubleangels.nextdnsmanagement.ui.components.LineChart
import com.doubleangels.nextdnsmanagement.ui.components.LoadingState
import com.doubleangels.nextdnsmanagement.ui.components.SectionCard
import com.doubleangels.nextdnsmanagement.ui.components.Slice
import com.doubleangels.nextdnsmanagement.ui.components.StatTile
import com.doubleangels.nextdnsmanagement.ui.components.formatCount
import com.doubleangels.nextdnsmanagement.ui.components.formatPercent
import com.doubleangels.nextdnsmanagement.ui.theme.ChartPalette
import com.doubleangels.nextdnsmanagement.ui.theme.LocalStatusColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    profileId: String,
    connection: Load<ConnectionStatus>,
    onCheckConnection: () -> Unit,
    onOpenLogs: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: OverviewViewModel = viewModel(),
) {
    LaunchedEffect(profileId) { viewModel.show(profileId) }
    val state = viewModel.state
    val colors = LocalStatusColors.current

    PullToRefreshBox(
        isRefreshing = viewModel.refreshing,
        onRefresh = {
            viewModel.refresh(profileId, pullToRefresh = true)
            onCheckConnection()
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { RangeSelector(viewModel.range) { viewModel.selectRange(profileId, it) } }
            item { ConnectionCard(connection, profileId, onCheckConnection) }
            item { Headline(state.totals, state.encryption) }
            item {
                SectionCard(stringResource(R.string.overview_queries_over_time)) {
                    LoadContent(state.series, { viewModel.refresh(profileId) }) { series ->
                        QueriesChart(series, viewModel.range, colors.neutral, colors.blocked)
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.overview_status)) {
                    LoadContent(state.totals, { viewModel.refresh(profileId) }) { totals ->
                        val total = totals.values.sum()
                        DonutChart(
                            slices = listOf(
                                Slice(stringResource(R.string.status_default), totals["default"] ?: 0, colors.neutral),
                                Slice(stringResource(R.string.status_blocked), totals["blocked"] ?: 0, colors.blocked),
                                Slice(stringResource(R.string.status_allowed), totals["allowed"] ?: 0, colors.allowed),
                            ),
                            centerValue = formatCount(total),
                            centerLabel = stringResource(R.string.overview_queries),
                        )
                    }
                }
            }
            item { TopDomainsCard(state, onOpenLogs) { viewModel.refresh(profileId) } }
            breakdownCard(R.string.overview_block_reasons, state.reasons, colors.blocked) { viewModel.refresh(profileId) }
            breakdownCard(R.string.overview_devices, state.devices, Color.Unspecified) { viewModel.refresh(profileId) }
            item {
                SectionCard(stringResource(R.string.overview_protocols)) {
                    LoadContent(state.protocols, { viewModel.refresh(profileId) }) { stats ->
                        StatsDonut(stats, stringResource(R.string.overview_queries))
                    }
                }
            }
            item { HealthCard(state) { viewModel.refresh(profileId) } }
            breakdownCard(R.string.overview_query_types, state.queryTypes, ChartPalette[4]) { viewModel.refresh(profileId) }
            breakdownCard(R.string.overview_countries, state.countries, ChartPalette[3]) { viewModel.refresh(profileId) }
            item {
                SectionCard(stringResource(R.string.overview_companies), subtitle = stringResource(R.string.overview_companies_subtitle)) {
                    LoadContent(state.companies, { viewModel.refresh(profileId) }) { stats ->
                        StatsDonut(stats, stringResource(R.string.overview_queries))
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> LoadContent(load: Load<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (load) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(load.message, onRetry)
        is Load.Ready -> content(load.value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(selected: TimeRange, onSelect: (TimeRange) -> Unit) {
    val labels = mapOf(
        TimeRange.DAY to R.string.range_day,
        TimeRange.WEEK to R.string.range_week,
        TimeRange.MONTH to R.string.range_month,
        TimeRange.QUARTER to R.string.range_quarter,
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        TimeRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index, TimeRange.entries.size),
            ) { Text(stringResource(labels.getValue(range))) }
        }
    }
}

@Composable
private fun ConnectionCard(connection: Load<ConnectionStatus>, profileId: String, onRetry: () -> Unit) {
    val colors = LocalStatusColors.current
    val context = LocalContext.current
    SectionCard(
        stringResource(R.string.connection_title),
        action = {
            IconButton(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.connection_check_again))
            }
        },
    ) {
        when (connection) {
            Load.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.connection_checking))
            }
            is Load.Failed -> Text(connection.message, color = MaterialTheme.colorScheme.error)
            is Load.Ready -> {
                val status = connection.value
                val (icon, tint, title) = when {
                    !status.usingNextDns -> Triple(Icons.Outlined.ErrorOutline, colors.blocked, R.string.connection_not_using)
                    !status.encrypted -> Triple(Icons.Outlined.Warning, colors.warning, R.string.connection_unencrypted)
                    else -> Triple(Icons.Outlined.VerifiedUser, colors.allowed, R.string.connection_secure)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = tint)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                        if (status.usingNextDns) {
                            Text(
                                stringResource(
                                    R.string.connection_details,
                                    status.protocol ?: "?",
                                    status.profile ?: "?",
                                    status.server ?: "?",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (status.usingNextDns && status.profile != null && status.profile != profileId) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.connection_other_profile, status.profile),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.warning,
                    )
                }
                if (!status.usingNextDns || !status.encrypted) {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                    }) { Text(stringResource(R.string.connection_open_private_dns)) }
                }
            }
        }
    }
}

@Composable
private fun Headline(totals: Load<Map<String, Long>>, encryption: Load<List<Stat>>) {
    val colors = LocalStatusColors.current
    val values = totals.valueOrNull
    val total = values?.values?.sum() ?: 0L
    val blocked = values?.get("blocked") ?: 0L
    val allowed = values?.get("allowed") ?: 0L
    val encrypted = encryption.valueOrNull?.let { stats ->
        val sum = stats.sumOf { it.queries }
        if (sum == 0L) null else stats.filter { it.key == "true" }.sumOf { it.queries }.toDouble() / sum
    }
    val placeholder = "–"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                stringResource(R.string.overview_total_queries),
                if (values != null) formatCount(total) else placeholder,
                Icons.Outlined.Dns, MaterialTheme.colorScheme.primary, Modifier.weight(1f),
            )
            StatTile(
                stringResource(R.string.status_blocked),
                if (values != null) formatCount(blocked) else placeholder,
                Icons.Outlined.Block, colors.blocked, Modifier.weight(1f),
                supporting = if (total > 0) formatPercent(blocked.toDouble() / total) else null,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                stringResource(R.string.status_allowed),
                if (values != null) formatCount(allowed) else placeholder,
                Icons.Outlined.CheckCircle, colors.allowed, Modifier.weight(1f),
            )
            StatTile(
                stringResource(R.string.overview_encrypted),
                encrypted?.let { formatPercent(it) } ?: placeholder,
                Icons.Outlined.Lock, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QueriesChart(series: Series, range: TimeRange, totalColor: Color, blockedColor: Color) {
    val size = series.values.values.maxOfOrNull { it.size } ?: 0
    if (size < 2) {
        EmptyState(stringResource(R.string.overview_no_data))
        return
    }
    val totals = (0 until size).map { i -> series.values.values.sumOf { it.getOrElse(i) { 0L } } }
    val blocked = series.values["blocked"] ?: List(size) { 0L }
    val locale = LocalLocale.current.platformLocale
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(
        locale, if (range == TimeRange.DAY) "jm" else "MMMd",
    )
    val formatter = remember(pattern, locale) {
        DateTimeFormatter.ofPattern(pattern, locale).withZone(ZoneId.systemDefault())
    }
    fun label(i: Int): String = series.times.getOrNull(i)?.let { formatter.format(it) } ?: ""
    LineChart(
        lines = listOf(
            ChartLine(stringResource(R.string.overview_all_queries), totalColor, totals),
            ChartLine(stringResource(R.string.status_blocked), blockedColor, blocked),
        ),
        pointLabel = ::label,
        axisLabels = Triple(label(0), label(size / 2), label(size - 1)),
    )
}

@Composable
private fun TopDomainsCard(state: OverviewState, onOpenLogs: (String) -> Unit, onRetry: () -> Unit) {
    var showBlocked by remember { mutableStateOf(false) }
    val colors = LocalStatusColors.current
    SectionCard(stringResource(R.string.overview_top_domains), subtitle = stringResource(R.string.overview_tap_for_logs)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(false, true).forEachIndexed { index, blocked ->
                SegmentedButton(
                    selected = showBlocked == blocked,
                    onClick = { showBlocked = blocked },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) { Text(stringResource(if (blocked) R.string.status_blocked else R.string.overview_all_queries)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        LoadContent(if (showBlocked) state.blockedDomains else state.topDomains, onRetry) { stats ->
            if (stats.isEmpty()) {
                EmptyState(stringResource(R.string.overview_no_data))
            } else {
                BarList(
                    stats.toRows(),
                    if (showBlocked) colors.blocked else colors.neutral,
                    onClick = { onOpenLogs(it.key) },
                )
            }
        }
    }
}

/** [color] [Color.Unspecified] means the theme's primary color. */
private fun LazyListScope.breakdownCard(title: Int, load: Load<List<Stat>>, color: Color, onRetry: () -> Unit) {
    item {
        SectionCard(stringResource(title)) {
            LoadContent(load, onRetry) { stats ->
                if (stats.isEmpty()) {
                    EmptyState(stringResource(R.string.overview_no_data))
                } else {
                    val barColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
                    val unidentified = stringResource(R.string.overview_unidentified_device)
                    BarList(
                        stats.toRows().map {
                            if (it.key == NextDnsRepository.UNIDENTIFIED_DEVICE) it.copy(label = unidentified) else it
                        },
                        barColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsDonut(stats: List<Stat>, centerLabel: String) {
    if (stats.isEmpty()) {
        EmptyState(stringResource(R.string.overview_no_data))
        return
    }
    val top = stats.take(ChartPalette.size)
    DonutChart(
        slices = top.mapIndexed { i, stat -> Slice(stat.label, stat.queries, ChartPalette[i]) },
        centerValue = formatCount(stats.sumOf { it.queries }),
        centerLabel = centerLabel,
    )
}

/** Share of encrypted, DNSSEC-validated and IPv6 queries. */
@Composable
private fun HealthCard(state: OverviewState, onRetry: () -> Unit) {
    SectionCard(stringResource(R.string.overview_health)) {
        val rows = listOf(
            Triple(R.string.overview_encrypted, state.encryption, "true"),
            Triple(R.string.overview_dnssec, state.dnssec, "true"),
            Triple(R.string.overview_ipv6, state.ipVersions, "IPv6"),
        )
        if (rows.any { it.second is Load.Failed }) {
            ErrorState((rows.first { it.second is Load.Failed }.second as Load.Failed).message, onRetry)
            return@SectionCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { (label, load, key) ->
                val stats = load.valueOrNull
                val sum = stats?.sumOf { it.queries } ?: 0L
                val fraction = if (stats == null || sum == 0L) null else stats.filter { it.key == key }.sumOf { it.queries }.toDouble() / sum
                Column {
                    Row {
                        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(fraction?.let { formatPercent(it) } ?: "–", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (fraction ?: 0.0).toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun List<Stat>.toRows() = map { BarRow(it.key, it.label, it.queries, it.detail) }
