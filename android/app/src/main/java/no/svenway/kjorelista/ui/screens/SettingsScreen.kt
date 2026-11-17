package no.svenway.kjorelista.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import no.svenway.kjorelista.BuildConfig
import no.svenway.kjorelista.R
import no.svenway.kjorelista.data.AppStore
import no.svenway.kjorelista.data.AuthState
import no.svenway.kjorelista.data.Language
import no.svenway.kjorelista.data.Preferences
import no.svenway.kjorelista.data.ThemePreference
import no.svenway.kjorelista.ui.Haptic
import no.svenway.kjorelista.ui.components.ButtonRow
import no.svenway.kjorelista.ui.components.ConfirmDialog
import no.svenway.kjorelista.ui.components.DialogMessage
import no.svenway.kjorelista.ui.components.MessageDialog
import no.svenway.kjorelista.ui.components.RowDivider
import no.svenway.kjorelista.ui.components.Section
import no.svenway.kjorelista.ui.components.clickableRow
import no.svenway.kjorelista.ui.currentLanguage
import no.svenway.kjorelista.ui.rememberHaptics
import no.svenway.kjorelista.ui.theme.LocalAppColors

/** Innstillinger som gruppert liste (port av src/app/settings.tsx). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(store: AppStore, preferences: Preferences, onBack: () -> Unit, onOpenPrivacy: () -> Unit) {
    val colors = LocalAppColors.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val auth by store.authState.collectAsStateWithLifecycle()
    val profile by store.profile.collectAsStateWithLifecycle()
    val theme by preferences.theme.collectAsStateWithLifecycle()
    val routeSuggestions by preferences.routeSuggestions.collectAsStateWithLifecycle()
    val autofillKilometers by preferences.autofillKilometers.collectAsStateWithLifecycle()
    val language = currentLanguage()

    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<DialogMessage?>(null) }

    val email = (auth as? AuthState.SignedIn)?.user?.email ?: ""
    val name = profile?.let { "${it.firstName} ${it.lastName}".trim() } ?: ""
    val initials = profile?.let { "${it.firstName.take(1)}${it.lastName.take(1)}".uppercase() } ?: ""
    val deleteErrorTitle = stringResource(R.string.delete_account_error)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.general_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Section(title = stringResource(R.string.account)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .semantics(mergeDescendants = true) { contentDescription = "$name, $email" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).background(colors.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (initials.isNotEmpty()) {
                                Text(initials, color = colors.primaryForeground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = colors.primaryForeground)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            if (name.isNotEmpty()) Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(email, style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
                            if (store.backend.isDemo) {
                                Text(stringResource(R.string.demo_mode), style = MaterialTheme.typography.labelSmall, color = colors.warning)
                            }
                        }
                    }
                }

                Section(
                    title = stringResource(R.string.settings_trip_entry),
                    footer = stringResource(
                        if (routeSuggestions) R.string.settings_autofill_km_footer else R.string.settings_route_suggestions_footer,
                    ),
                ) {
                    SwitchRow(stringResource(R.string.settings_route_suggestions), checked = routeSuggestions) {
                        haptics(Haptic.LIGHT)
                        preferences.setRouteSuggestions(it)
                    }
                    // Automatisk km er bare relevant når forslag er slått på
                    if (routeSuggestions) {
                        RowDivider()
                        SwitchRow(stringResource(R.string.settings_autofill_km), checked = autofillKilometers) {
                            haptics(Haptic.LIGHT)
                            preferences.setAutofillKilometers(it)
                        }
                    }
                }

                Section(title = stringResource(R.string.general_language)) {
                    SelectRow("Norsk", selected = language == Language.NB) {
                        haptics(Haptic.LIGHT)
                        Preferences.setLanguage(Language.NB)
                    }
                    RowDivider()
                    SelectRow("English", selected = language == Language.EN) {
                        haptics(Haptic.LIGHT)
                        Preferences.setLanguage(Language.EN)
                    }
                }

                Section(title = stringResource(R.string.appearance)) {
                    listOf(
                        ThemePreference.SYSTEM to R.string.theme_system,
                        ThemePreference.LIGHT to R.string.theme_light,
                        ThemePreference.DARK to R.string.theme_dark,
                    ).forEachIndexed { i, (pref, label) ->
                        if (i > 0) RowDivider()
                        SelectRow(stringResource(label), selected = theme == pref) {
                            haptics(Haptic.LIGHT)
                            preferences.setTheme(pref)
                        }
                    }
                }

                Section {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.privacy_title)) },
                        leadingContent = { Icon(Icons.Outlined.Shield, contentDescription = null, tint = colors.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickableRow(onOpenPrivacy),
                    )
                }

                Section {
                    ButtonRow(stringResource(R.string.auth_sign_out), onClick = { confirmSignOut = true })
                }

                Section(footer = stringResource(R.string.delete_account_footer)) {
                    if (deleting) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.destructive, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        ButtonRow(stringResource(R.string.delete_account), destructive = true, onClick = { confirmDeleteAccount = true })
                    }
                }

                Text(
                    "${stringResource(R.string.header_title)} · ${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = stringResource(R.string.auth_sign_out),
            message = stringResource(R.string.auth_sign_out_confirmation),
            confirmLabel = stringResource(R.string.auth_sign_out),
            destructive = true,
            onDismiss = { confirmSignOut = false },
            onConfirm = {
                haptics(Haptic.SUCCESS)
                scope.launch { runCatching { store.signOut() } }
            },
        )
    }

    // Kontoen slettes av Edge-funksjonen «delete-account». I demomodus kalles den aldri.
    if (confirmDeleteAccount) {
        ConfirmDialog(
            title = stringResource(R.string.delete_account),
            message = stringResource(R.string.delete_account_message),
            confirmLabel = stringResource(R.string.delete_account),
            destructive = true,
            onDismiss = { confirmDeleteAccount = false },
            onConfirm = {
                deleting = true
                scope.launch {
                    try {
                        store.deleteAccount()
                        haptics(Haptic.SUCCESS)
                    } catch (e: Exception) {
                        haptics(Haptic.ERROR)
                        message = DialogMessage(deleteErrorTitle, e.message)
                    } finally {
                        deleting = false
                    }
                }
            },
        )
    }

    message?.let { MessageDialog(it.title, it.message) { message = null } }
}

/** Valg med radioknapp. */
@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@Composable
private fun SelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
