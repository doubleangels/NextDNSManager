package com.doubleangels.nextdnsmanagement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.doubleangels.nextdnsmanagement.R
import com.doubleangels.nextdnsmanagement.api.Profile
import com.doubleangels.nextdnsmanagement.data.Load
import com.doubleangels.nextdnsmanagement.ui.components.ErrorState
import com.doubleangels.nextdnsmanagement.ui.lists.ListsScreen
import com.doubleangels.nextdnsmanagement.ui.logs.LogsScreen
import com.doubleangels.nextdnsmanagement.ui.onboarding.OnboardingScreen
import com.doubleangels.nextdnsmanagement.ui.overview.OverviewScreen
import com.doubleangels.nextdnsmanagement.ui.profile.ProfileScreen
import com.doubleangels.nextdnsmanagement.ui.settings.AppSettingsScreen
import com.doubleangels.nextdnsmanagement.ui.theme.NextDnsTheme
import kotlinx.coroutines.launch

private enum class Tab(val route: String, val label: Int, val icon: ImageVector) {
    OVERVIEW("overview", R.string.tab_overview, Icons.Outlined.Insights),
    LOGS("logs", R.string.tab_logs, Icons.AutoMirrored.Outlined.ReceiptLong),
    LISTS("lists", R.string.tab_lists, Icons.AutoMirrored.Outlined.List),
    PROFILE("profile", R.string.tab_profile, Icons.Outlined.Shield),
    SETTINGS("settings", R.string.settings, Icons.Outlined.Settings),
}

@Composable
fun NextDnsApp(
    session: SessionViewModel,
    locked: Boolean,
    onUnlock: () -> Unit,
    onReady: () -> Unit,
    onSignedIn: () -> Unit,
) {
    NextDnsTheme(session.themeMode, session.dynamicColor) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!session.ready) return@Surface
            LaunchedEffect(Unit) { onReady() }
            when {
                locked -> LockScreen(onUnlock)
                !session.signedIn -> OnboardingScreen(session::signIn)
                else -> {
                    LaunchedEffect(Unit) { onSignedIn() }
                    SignedIn(session)
                }
            }
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    // Ask right away; the button is for when the prompt was dismissed.
    LaunchedEffect(Unit) { onUnlock() }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.unlock_description), textAlign = TextAlign.Center)
        Spacer(Modifier.size(24.dp))
        Button(onClick = onUnlock) { Text(stringResource(R.string.unlock_title)) }
    }
}

@Composable
private fun SignedIn(session: SessionViewModel) {
    when (val profiles = session.profiles) {
        Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is Load.Failed -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ErrorState(profiles.message, session::loadProfiles)
            OutlinedButton(onClick = session::signOut) { Text(stringResource(R.string.settings_sign_out)) }
        }
        is Load.Ready -> {
            val profile = session.currentProfile
            if (profile == null) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.no_profiles), textAlign = TextAlign.Center)
                    TextButton(onClick = session::loadProfiles) { Text(stringResource(R.string.retry)) }
                }
            } else {
                MainScaffold(session, profiles.value, profile)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(session: SessionViewModel, profiles: List<Profile>, profile: Profile) {
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == route } ?: Tab.OVERVIEW
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    var pendingSearch by remember { mutableStateOf<String?>(null) }

    fun go(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { ProfileTitle(stringResource(currentTab.label), profiles, profile, session::selectProfile) })
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == currentTab,
                        onClick = { go(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(navController, startDestination = Tab.OVERVIEW.route) {
            composable(Tab.OVERVIEW.route) {
                OverviewScreen(
                    profileId = profile.id,
                    connection = session.connection,
                    onCheckConnection = session::checkConnection,
                    onOpenLogs = { domain ->
                        pendingSearch = domain
                        go(Tab.LOGS)
                    },
                    contentPadding = padding,
                )
            }
            composable(Tab.LOGS.route) {
                LogsScreen(
                    profileId = profile.id,
                    pendingSearch = pendingSearch,
                    onPendingSearchConsumed = { pendingSearch = null },
                    showMessage = showMessage,
                    contentPadding = padding,
                )
            }
            composable(Tab.LISTS.route) {
                ListsScreen(profileId = profile.id, showMessage = showMessage, contentPadding = padding)
            }
            composable(Tab.PROFILE.route) {
                ProfileScreen(
                    profile = profile,
                    onRenamed = { session.onProfileRenamed(profile.id, it) },
                    showMessage = showMessage,
                    contentPadding = padding,
                )
            }
            composable(Tab.SETTINGS.route) {
                AppSettingsScreen(session = session, contentPadding = padding)
            }
        }
    }
}

/** Screen title with the active profile underneath; tap to switch when there are several. */
@Composable
private fun ProfileTitle(title: String, profiles: List<Profile>, current: Profile, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    current.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (profiles.size > 1) {
                TextButton(onClick = { expanded = true }) {
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = stringResource(R.string.switch_profile))
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text("${profile.name} (${profile.id})") },
                    leadingIcon = if (profile.id == current.id) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelect(profile.id)
                    },
                )
            }
        }
    }
}
