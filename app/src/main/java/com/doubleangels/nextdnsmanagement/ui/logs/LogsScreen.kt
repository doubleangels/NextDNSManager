package com.doubleangels.nextdnsmanagement.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doubleangels.nextdnsmanagement.R
import com.doubleangels.nextdnsmanagement.api.EntryList
import com.doubleangels.nextdnsmanagement.api.LogEntry
import com.doubleangels.nextdnsmanagement.ui.components.EmptyState
import com.doubleangels.nextdnsmanagement.ui.components.ErrorState
import com.doubleangels.nextdnsmanagement.ui.theme.LocalStatusColors
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    profileId: String,
    pendingSearch: String?,
    onPendingSearchConsumed: () -> Unit,
    showMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: LogsViewModel = viewModel(),
) {
    LaunchedEffect(profileId) { viewModel.show(profileId) }
    LaunchedEffect(pendingSearch) {
        if (pendingSearch != null) {
            viewModel.searchNow(pendingSearch)
            onPendingSearchConsumed()
        }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var selected by remember { mutableStateOf<LogEntry?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 10
        }
    }
    LaunchedEffect(nearEnd, viewModel.entries.size) { if (nearEnd) viewModel.loadMore() }

    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        OutlinedTextField(
            value = viewModel.search,
            onValueChange = viewModel::updateSearch,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.logs_search)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (viewModel.search.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearch("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.logs_clear_search))
                    }
                }
            },
            singleLine = true,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusFilter.entries.forEach { filter ->
                FilterChip(
                    selected = viewModel.filter == filter,
                    onClick = { viewModel.updateFilter(filter) },
                    label = {
                        Text(
                            stringResource(
                                when (filter) {
                                    StatusFilter.ALL -> R.string.logs_filter_all
                                    StatusFilter.BLOCKED -> R.string.status_blocked
                                    StatusFilter.ALLOWED -> R.string.status_allowed
                                },
                            ),
                        )
                    },
                )
            }
            FilterChip(
                selected = viewModel.live,
                onClick = viewModel::toggleLive,
                label = { Text(stringResource(R.string.logs_live)) },
                leadingIcon = { Icon(Icons.Outlined.Sensors, contentDescription = null) },
            )
            IconButton(onClick = { confirmClear = true }) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.logs_clear))
            }
        }
        if (viewModel.loading && viewModel.entries.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        val error = viewModel.error
        when {
            error != null && viewModel.entries.isEmpty() -> ErrorState(error, viewModel::reload)
            !viewModel.loading && viewModel.entries.isEmpty() ->
                EmptyState(stringResource(R.string.logs_empty), Modifier.padding(16.dp))
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
            ) {
                // No item keys: log entries have no id, and timestamps can repeat.
                items(viewModel.entries) { entry ->
                    LogRow(entry) { selected = entry }
                    HorizontalDivider()
                }
                if (viewModel.loading) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
                }
                if (error != null) {
                    item { ErrorState(error, viewModel::loadMore) }
                }
            }
        }
    }

    selected?.let { entry ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            LogDetails(entry) { list, domain ->
                scope.launch {
                    val failure = viewModel.addTo(list, domain)
                    showMessage(
                        failure ?: context.getString(
                            if (list == EntryList.ALLOWLIST) R.string.logs_added_allowlist else R.string.logs_added_denylist,
                            domain,
                        ),
                    )
                    selected = null
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.logs_clear)) },
            text = { Text(stringResource(R.string.logs_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch { viewModel.clearLogs()?.let(showMessage) }
                }) { Text(stringResource(R.string.logs_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun LogRow(entry: LogEntry, onClick: () -> Unit) {
    val colors = LocalStatusColors.current
    val (icon, tint) = when {
        entry.isBlocked -> Icons.Outlined.Block to colors.blocked
        entry.isAllowed -> Icons.Outlined.CheckCircle to colors.allowed
        else -> Icons.Outlined.Language to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val time = DateUtils.getRelativeTimeSpanString(
        entry.timestamp.toEpochMilli(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS,
    ).toString()
    val reason = entry.reasons.firstOrNull()
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        headlineContent = { Text(entry.domain, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(time, entry.deviceName, entry.protocol).joinToString(" · "),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = if (reason == null) null else {
            {
                Text(
                    reason, style = MaterialTheme.typography.labelSmall, color = colors.blocked,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.35f),
                )
            }
        },
    )
}

@Composable
private fun LogDetails(entry: LogEntry, onAdd: (EntryList, String) -> Unit) {
    val context = LocalContext.current
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    val target = entry.root ?: entry.domain
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entry.domain, style = MaterialTheme.typography.titleLarge)
        Detail(R.string.logs_detail_time, formatter.format(entry.timestamp))
        Detail(
            R.string.logs_detail_status,
            stringResource(
                when {
                    entry.isBlocked -> R.string.status_blocked
                    entry.isAllowed -> R.string.status_allowed
                    else -> R.string.status_default
                },
            ),
        )
        if (entry.reasons.isNotEmpty()) Detail(R.string.logs_detail_reasons, entry.reasons.joinToString("\n"))
        entry.root?.let { Detail(R.string.logs_detail_root, it) }
        entry.deviceName?.let { Detail(R.string.logs_detail_device, it) }
        entry.clientIp?.let { Detail(R.string.logs_detail_ip, it) }
        entry.protocol?.let {
            val encrypted = stringResource(if (entry.encrypted == true) R.string.yes else R.string.no)
            Detail(R.string.logs_detail_protocol, "$it · ${stringResource(R.string.overview_encrypted)}: $encrypted")
        }
        entry.tracker?.let { Detail(R.string.logs_detail_tracker, it) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entry.isBlocked) {
                FilledTonalButton(onClick = { onAdd(EntryList.ALLOWLIST, target) }) {
                    Text(stringResource(R.string.logs_allow_domain, target))
                }
            } else {
                FilledTonalButton(onClick = { onAdd(EntryList.DENYLIST, target) }) {
                    Text(stringResource(R.string.logs_block_domain, target))
                }
            }
        }
        OutlinedButton(onClick = {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText(entry.domain, entry.domain))
        }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text(stringResource(R.string.logs_copy_domain))
        }
    }
}

@Composable
private fun Detail(label: Int, value: String) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
