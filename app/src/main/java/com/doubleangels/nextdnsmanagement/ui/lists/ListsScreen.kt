package com.doubleangels.nextdnsmanagement.ui.lists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doubleangels.nextdnsmanagement.R
import com.doubleangels.nextdnsmanagement.data.Load
import com.doubleangels.nextdnsmanagement.ui.components.AddEntryDialog
import com.doubleangels.nextdnsmanagement.ui.components.EmptyState
import com.doubleangels.nextdnsmanagement.ui.components.EntryRow
import com.doubleangels.nextdnsmanagement.ui.components.ErrorState
import com.doubleangels.nextdnsmanagement.ui.components.LoadingState

@Composable
fun ListsScreen(
    profileId: String,
    showMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: ListsViewModel = viewModel(),
) {
    var tab by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    val controller = if (tab == 0) viewModel.denylist else viewModel.allowlist

    LaunchedEffect(profileId, tab) { controller.show(profileId) }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            showMessage(it)
            viewModel.message = null
        }
    }

    Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Column {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.lists_denylist)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.lists_allowlist)) })
            }
            Text(
                stringResource(if (tab == 0) R.string.lists_denylist_help else R.string.lists_allowlist_help),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (val entries = controller.entries) {
                Load.Loading -> LoadingState()
                is Load.Failed -> ErrorState(entries.message, controller::reload)
                is Load.Ready -> if (entries.value.isEmpty()) {
                    EmptyState(stringResource(R.string.lists_empty), Modifier.padding(horizontal = 16.dp))
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 88.dp)) {
                        items(entries.value, key = { it.id }) { entry ->
                            EntryRow(
                                entry = entry,
                                label = entry.id,
                                showSwitch = true,
                                onActiveChange = { controller.setActive(entry.id, it) },
                                onRemove = { controller.remove(entry.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.lists_add_domain)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }

    if (adding) {
        AddEntryDialog(
            title = stringResource(if (tab == 0) R.string.lists_add_to_denylist else R.string.lists_add_to_allowlist),
            fieldLabel = stringResource(R.string.lists_domain),
            onDismiss = { adding = false },
            onAdd = controller::add,
        )
    }
}
