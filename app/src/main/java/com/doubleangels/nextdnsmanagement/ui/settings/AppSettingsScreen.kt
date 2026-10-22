package com.doubleangels.nextdnsmanagement.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doubleangels.nextdnsmanagement.BuildConfig
import com.doubleangels.nextdnsmanagement.R
import com.doubleangels.nextdnsmanagement.biometriclock.BiometricLock
import com.doubleangels.nextdnsmanagement.data.AppPreferences
import com.doubleangels.nextdnsmanagement.data.ThemeMode
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer
import com.doubleangels.nextdnsmanagement.ui.SessionViewModel
import com.doubleangels.nextdnsmanagement.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(session: SessionViewModel, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var appLock by remember { mutableStateOf(AppPreferences.appLock) }
    var errorReporting by remember { mutableStateOf(AppPreferences.errorReporting) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val activity = context.findActivity()
    val lockAvailable = remember {
        AppPreferences.appLockAvailable && activity != null && BiometricLock(activity).canAuthenticate()
    }
    fun open(url: String) = runCatching { uriHandler.openUri(url) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(stringResource(R.string.settings_appearance)) {
                Text(stringResource(R.string.dark_mode), style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val modes = listOf(
                        ThemeMode.SYSTEM to R.string.theme_system,
                        ThemeMode.LIGHT to R.string.theme_light,
                        ThemeMode.DARK to R.string.theme_dark,
                    )
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = session.themeMode == mode,
                            onClick = { session.updateTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        ) { Text(stringResource(label)) }
                    }
                }
                SwitchRow(
                    Icons.Outlined.Palette, stringResource(R.string.settings_dynamic_color),
                    stringResource(R.string.settings_dynamic_color_help), session.dynamicColor, session::updateDynamicColor,
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.settings_security_privacy)) {
                if (lockAvailable && activity != null) {
                    SwitchRow(
                        Icons.Outlined.Key, stringResource(R.string.app_lock), stringResource(R.string.app_lock_instructions), appLock,
                    ) { enable ->
                        // Changing app lock always requires authenticating first.
                        BiometricLock(activity).showPrompt(
                            context.getString(if (enable) R.string.enable_app_lock_title else R.string.disable_app_lock_title),
                            context.getString(if (enable) R.string.enable_app_lock_subtitle else R.string.disable_app_lock_subtitle),
                            context.getString(R.string.app_lock_confirm_action),
                            object : BiometricLock.BiometricLockCallback {
                                override fun onAuthenticationSucceeded() {
                                    AppPreferences.appLock = enable
                                    AppPreferences.lastAuthenticated = System.currentTimeMillis()
                                    appLock = enable
                                }

                                override fun onAuthenticationError(errorCode: Int, error: String?) = Unit
                                override fun onAuthenticationFailed() = Unit
                            },
                        )
                    }
                }
                SwitchRow(
                    Icons.Outlined.BugReport, stringResource(R.string.settings_error_reporting),
                    stringResource(R.string.whitelist_instructions), errorReporting,
                ) { enable ->
                    AppPreferences.errorReporting = enable
                    errorReporting = enable
                    SentryInitializer.applyPreference(context, enable)
                }
                if (errorReporting) {
                    Text(
                        stringResource(R.string.whitelist_domains),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    listOf(R.string.whitelist_domain_1, R.string.whitelist_domain_2).forEach { res ->
                        val domain = stringResource(res)
                        LinkRow(Icons.Outlined.ContentCopy, domain) {
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText(domain, domain))
                            Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.settings_account)) {
                val key = AppPreferences.apiKey.orEmpty()
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Outlined.Key, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_api_key)) },
                    supportingContent = { Text("••••••••" + key.takeLast(4)) },
                )
                LinkRow(Icons.AutoMirrored.Outlined.Logout, stringResource(R.string.settings_sign_out)) { confirmSignOut = true }
            }
        }
        item {
            SectionCard(stringResource(R.string.settings_tools)) {
                LinkRow(Icons.Outlined.NetworkPing, stringResource(R.string.ping_title)) { open(context.getString(R.string.ping_url)) }
                LinkRow(Icons.Outlined.SettingsEthernet, stringResource(R.string.private_dns)) {
                    runCatching { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.settings_about)) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.version)) },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                    modifier = Modifier.clickable { open(context.getString(R.string.versions_url)) },
                )
                val links = listOf(
                    R.string.github to R.string.github_url,
                    R.string.issues to R.string.github_issues_url,
                    R.string.feedback_title to R.string.tally_feedback_url,
                    R.string.translate to R.string.tally_incorrect_translation_url,
                    R.string.donation to R.string.donation_url,
                )
                links.forEach { (label, url) ->
                    LinkRow(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(label)) { open(context.getString(url)) }
                }
                HorizontalDivider()
                listOf(
                    R.string.privacy_policy to R.string.privacy_policy_url,
                    R.string.nextdns_privacy_policy to R.string.nextdns_privacy_policy_url,
                    R.string.nextdns_user_agreement to R.string.nextdns_user_agreement_url,
                ).forEach { (label, url) ->
                    LinkRow(Icons.Outlined.Policy, stringResource(label)) { open(context.getString(url)) }
                }
                Text(
                    stringResource(R.string.disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.settings_sign_out)) },
            text = { Text(stringResource(R.string.settings_sign_out_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    session.signOut()
                }) { Text(stringResource(R.string.settings_sign_out)) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

fun Context.findActivity(): AppCompatActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is AppCompatActivity) return current
        current = current.baseContext
    }
    return null
}
