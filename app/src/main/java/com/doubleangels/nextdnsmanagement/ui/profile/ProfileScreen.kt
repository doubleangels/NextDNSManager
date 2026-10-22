package com.doubleangels.nextdnsmanagement.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doubleangels.nextdnsmanagement.R
import com.doubleangels.nextdnsmanagement.api.Profile
import com.doubleangels.nextdnsmanagement.api.SettingsSection
import com.doubleangels.nextdnsmanagement.api.booleanEntries
import com.doubleangels.nextdnsmanagement.api.bool
import com.doubleangels.nextdnsmanagement.api.long
import com.doubleangels.nextdnsmanagement.api.obj
import com.doubleangels.nextdnsmanagement.api.str
import com.doubleangels.nextdnsmanagement.data.EntryListController
import com.doubleangels.nextdnsmanagement.data.Load
import com.doubleangels.nextdnsmanagement.ui.components.AddEntryDialog
import com.doubleangels.nextdnsmanagement.ui.components.EmptyState
import com.doubleangels.nextdnsmanagement.ui.components.EntryRow
import com.doubleangels.nextdnsmanagement.ui.components.ErrorState
import com.doubleangels.nextdnsmanagement.ui.components.LoadingState
import com.doubleangels.nextdnsmanagement.ui.components.SectionCard
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

@Composable
fun ProfileScreen(
    profile: Profile,
    onRenamed: (String) -> Unit,
    showMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: ProfileViewModel = viewModel(),
) {
    LaunchedEffect(profile.id) { viewModel.show(profile.id) }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            showMessage(it)
            viewModel.message = null
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ProfileCard(profile, viewModel, onRenamed, showMessage) }
        item {
            ToggleSection(viewModel, SettingsSection.SECURITY, R.string.profile_security, R.string.profile_security_help) {
                EntryListBlock(viewModel.tlds, R.string.profile_blocked_tlds, R.string.profile_tld, emptyList())
            }
        }
        item {
            ToggleSection(viewModel, SettingsSection.PRIVACY, R.string.profile_privacy, R.string.profile_privacy_help) {
                EntryListBlock(viewModel.blocklists, R.string.profile_blocklists, R.string.profile_blocklist_id, BLOCKLISTS)
                EntryListBlock(viewModel.natives, R.string.profile_native_tracking, R.string.profile_native_id, NATIVES)
            }
        }
        item {
            ToggleSection(
                viewModel, SettingsSection.PARENTAL_CONTROL, R.string.profile_parental_control, R.string.profile_parental_help,
            ) {
                EntryListBlock(viewModel.services, R.string.profile_services, R.string.profile_service_id, SERVICES)
                EntryListBlock(viewModel.categories, R.string.profile_categories, R.string.profile_category_id, CATEGORIES)
            }
        }
        item { SettingsCard(viewModel) }
        item { RewritesCard(viewModel) }
    }
}

@Composable
private fun ProfileCard(profile: Profile, viewModel: ProfileViewModel, onRenamed: (String) -> Unit, showMessage: (String) -> Unit) {
    var renaming by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    SectionCard(
        profile.name,
        subtitle = stringResource(R.string.profile_id, profile.id),
        action = {
            Row {
                IconButton(onClick = { renaming = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.profile_rename))
                }
                IconButton(onClick = { runCatching { uriHandler.openUri("https://my.nextdns.io/${profile.id}/setup") } }) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = stringResource(R.string.profile_open_dashboard))
                }
            }
        },
    ) {
        Text(
            stringResource(R.string.profile_setup_hint, "${profile.id}.dns.nextdns.io"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (renaming) {
        var name by remember { mutableStateOf(profile.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.profile_rename)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    renaming = false
                    scope.launch {
                        val error = viewModel.rename(name)
                        if (error == null) onRenamed(name.trim()) else showMessage(error)
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SectionLoad(load: Load<JsonObject>?, onRetry: () -> Unit, content: @Composable (JsonObject) -> Unit) {
    when (load) {
        null, Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(load.message, onRetry)
        is Load.Ready -> content(load.value)
    }
}

/** Every boolean the API returns for [section], plus section-specific lists. */
@Composable
private fun ToggleSection(
    viewModel: ProfileViewModel,
    section: SettingsSection,
    title: Int,
    help: Int,
    extra: @Composable ColumnScope.() -> Unit,
) {
    SectionCard(stringResource(title), subtitle = stringResource(help)) {
        SectionLoad(viewModel.sections[section], viewModel::reload) { obj ->
            Column {
                obj.booleanEntries().forEach { (key, enabled) ->
                    SettingSwitch(settingLabel(key), enabled) { viewModel.toggle(section, listOf(key), it) }
                }
            }
        }
        extra()
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(label) },
        supportingContent = if (description == null) null else {
            { Text(description) }
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun ColumnScope.EntryListBlock(
    controller: EntryListController,
    title: Int,
    fieldLabel: Int,
    catalog: List<Pair<String, String>>,
) {
    var adding by remember { mutableStateOf(false) }
    val labels = catalog.toMap()
    Spacer(Modifier.height(8.dp))
    HorizontalDivider()
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        IconButton(onClick = { adding = true }) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add))
        }
    }
    when (val entries = controller.entries) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(entries.message, controller::reload)
        is Load.Ready -> if (entries.value.isEmpty()) {
            EmptyState(stringResource(R.string.profile_list_empty))
        } else {
            entries.value.forEach { entry ->
                EntryRow(
                    entry = entry,
                    label = labels[entry.id] ?: entry.id,
                    showSwitch = controller.list.supportsActive,
                    onActiveChange = { controller.setActive(entry.id, it) },
                    onRemove = { controller.remove(entry.id) },
                )
            }
        }
    }
    if (adding) {
        val existing = (controller.entries as? Load.Ready)?.value?.map { it.id }?.toSet() ?: emptySet()
        AddEntryDialog(
            title = stringResource(title),
            fieldLabel = stringResource(fieldLabel),
            onDismiss = { adding = false },
            onAdd = controller::add,
            suggestions = catalog.filterNot { it.first in existing },
        )
    }
}

@Composable
private fun SettingsCard(viewModel: ProfileViewModel) {
    SectionCard(stringResource(R.string.profile_settings), subtitle = stringResource(R.string.profile_settings_help)) {
        SectionLoad(viewModel.sections[SettingsSection.SETTINGS], viewModel::reload) { settings ->
            val section = SettingsSection.SETTINGS
            Column {
                val logs = settings.obj("logs")
                if (logs != null) {
                    Text(stringResource(R.string.profile_logs), style = MaterialTheme.typography.titleSmall)
                    logs.bool("enabled")?.let { enabled ->
                        SettingSwitch(stringResource(R.string.profile_logs_enabled), enabled) {
                            viewModel.toggle(section, listOf("logs", "enabled"), it)
                        }
                    }
                    logs.obj("drop")?.let { drop ->
                        drop.bool("ip")?.let { value ->
                            SettingSwitch(stringResource(R.string.profile_logs_drop_ip), value) {
                                viewModel.toggle(section, listOf("logs", "drop", "ip"), it)
                            }
                        }
                        drop.bool("domain")?.let { value ->
                            SettingSwitch(stringResource(R.string.profile_logs_drop_domain), value) {
                                viewModel.toggle(section, listOf("logs", "drop", "domain"), it)
                            }
                        }
                    }
                    logs.long("retention")?.let { retention ->
                        RetentionPicker(retention) { viewModel.update(section, listOf("logs", "retention"), JsonPrimitive(it)) }
                    }
                    logs.str("location")?.let { location ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(stringResource(R.string.profile_logs_location)) },
                            supportingContent = { Text(locationLabel(location)) },
                        )
                    }
                }
                settings.obj("blockPage")?.bool("enabled")?.let { enabled ->
                    SettingSwitch(stringResource(R.string.setting_block_page), enabled) {
                        viewModel.toggle(section, listOf("blockPage", "enabled"), it)
                    }
                }
                settings.obj("performance")?.let { performance ->
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.profile_performance), style = MaterialTheme.typography.titleSmall)
                    performance.booleanEntries().forEach { (key, enabled) ->
                        SettingSwitch(settingLabel(key), enabled) { viewModel.toggle(section, listOf("performance", key), it) }
                    }
                }
                val otherToggles = settings.booleanEntries()
                if (otherToggles.isNotEmpty()) Spacer(Modifier.height(8.dp))
                otherToggles.forEach { (key, enabled) ->
                    SettingSwitch(settingLabel(key), enabled) { viewModel.toggle(section, listOf(key), it) }
                }
            }
        }
    }
}

/** Log retention in seconds. */
@Composable
private fun RetentionPicker(current: Long, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = (RETENTION_OPTIONS + current).distinct().sorted()
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(stringResource(R.string.profile_logs_retention)) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) { Text(retentionLabel(current)) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { seconds ->
                        DropdownMenuItem(text = { Text(retentionLabel(seconds)) }, onClick = {
                            expanded = false
                            if (seconds != current) onSelect(seconds)
                        })
                    }
                }
            }
        },
    )
}

@Composable
private fun retentionLabel(seconds: Long): String {
    val hours = seconds / 3600
    val days = seconds / 86_400
    return when {
        seconds < 86_400 -> pluralStringResource(R.plurals.duration_hours, hours.toInt(), hours)
        days % 365 == 0L -> pluralStringResource(R.plurals.duration_years, (days / 365).toInt(), days / 365)
        days % 30 == 0L && days >= 30 -> pluralStringResource(R.plurals.duration_months, (days / 30).toInt(), days / 30)
        else -> pluralStringResource(R.plurals.duration_days, days.toInt(), days)
    }
}

private fun locationLabel(code: String): String {
    val parts = code.split("-")
    val country = runCatching { Locale.Builder().setRegion(parts[0]).build().displayCountry }.getOrNull()
        ?.takeIf { it.isNotBlank() } ?: return code.uppercase()
    return if (parts.size > 1) "$country (${parts[1].uppercase()})" else country
}

@Composable
private fun RewritesCard(viewModel: ProfileViewModel) {
    var adding by remember { mutableStateOf(false) }
    SectionCard(
        stringResource(R.string.profile_rewrites),
        subtitle = stringResource(R.string.profile_rewrites_help),
        action = {
            IconButton(onClick = { adding = true }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add))
            }
        },
    ) {
        when (val rewrites = viewModel.rewrites) {
            Load.Loading -> LoadingState()
            is Load.Failed -> ErrorState(rewrites.message, viewModel::reload)
            is Load.Ready -> if (rewrites.value.isEmpty()) {
                EmptyState(stringResource(R.string.profile_list_empty))
            } else {
                rewrites.value.forEach { rewrite ->
                    EntryRow(
                        entry = com.doubleangels.nextdnsmanagement.api.DomainEntry(rewrite.id, true),
                        label = "${rewrite.name} → ${rewrite.content}",
                        showSwitch = false,
                        onActiveChange = {},
                        onRemove = { viewModel.removeRewrite(rewrite) },
                    )
                }
            }
        }
    }
    if (adding) {
        var name by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.profile_add_rewrite)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.lists_domain)) }, singleLine = true)
                    OutlinedTextField(
                        content, { content = it },
                        label = { Text(stringResource(R.string.profile_rewrite_answer)) }, singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && content.isNotBlank(), onClick = {
                    adding = false
                    viewModel.addRewrite(name, content)
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Human labels for setting keys; unknown keys are turned from camelCase into words. */
@Composable
private fun settingLabel(key: String): String {
    val res = SETTING_LABELS[key] ?: return key
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar { it.titlecase() }
    return stringResource(res)
}

private val SETTING_LABELS = mapOf(
    "threatIntelligenceFeeds" to R.string.setting_threat_intelligence,
    "aiThreatDetection" to R.string.setting_ai_threat_detection,
    "googleSafeBrowsing" to R.string.setting_safe_browsing,
    "cryptojacking" to R.string.setting_cryptojacking,
    "dnsRebinding" to R.string.setting_dns_rebinding,
    "idnHomographs" to R.string.setting_homographs,
    "homographs" to R.string.setting_homographs,
    "typosquatting" to R.string.setting_typosquatting,
    "dga" to R.string.setting_dga,
    "nrd" to R.string.setting_nrd,
    "ddns" to R.string.setting_ddns,
    "parking" to R.string.setting_parked,
    "parked" to R.string.setting_parked,
    "csam" to R.string.setting_csam,
    "disguisedTrackers" to R.string.setting_disguised_trackers,
    "allowAffiliate" to R.string.setting_allow_affiliate,
    "safeSearch" to R.string.setting_safe_search,
    "youtubeRestrictedMode" to R.string.setting_youtube_restricted,
    "blockBypass" to R.string.setting_block_bypass,
    "ecs" to R.string.setting_ecs,
    "cacheBoost" to R.string.setting_cache_boost,
    "cnameFlattening" to R.string.setting_cname_flattening,
    "web3" to R.string.setting_web3,
)

private val RETENTION_OPTIONS = listOf(3_600L, 21_600L, 86_400L, 604_800L, 2_592_000L, 7_776_000L, 15_552_000L, 31_536_000L, 63_072_000L)

// Ids accepted by the API, with display names. Anything else can still be typed in.
private val BLOCKLISTS = listOf(
    "nextdns-recommended" to "NextDNS Ads & Trackers",
    "oisd" to "OISD",
    "adguard" to "AdGuard DNS filter",
    "stevenblack" to "Steven Black",
    "energized" to "Energized",
)

private val NATIVES = listOf(
    "apple" to "Apple", "samsung" to "Samsung", "huawei" to "Huawei", "xiaomi" to "Xiaomi",
    "windows" to "Windows", "alexa" to "Alexa", "roku" to "Roku", "sonos" to "Sonos",
)

private val SERVICES = listOf(
    "tiktok" to "TikTok", "facebook" to "Facebook", "instagram" to "Instagram", "snapchat" to "Snapchat",
    "twitter" to "X (Twitter)", "youtube" to "YouTube", "twitch" to "Twitch", "discord" to "Discord",
    "reddit" to "Reddit", "whatsapp" to "WhatsApp", "telegram" to "Telegram", "fortnite" to "Fortnite",
    "roblox" to "Roblox", "minecraft" to "Minecraft", "netflix" to "Netflix", "tinder" to "Tinder",
)

private val CATEGORIES = listOf(
    "porn" to "Porn", "gambling" to "Gambling", "dating" to "Dating",
    "piracy" to "Piracy", "social-networks" to "Social networks",
)
