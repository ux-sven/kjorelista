package no.svenway.kjorelista.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import no.svenway.kjorelista.R
import no.svenway.kjorelista.data.SupabaseConfig
import no.svenway.kjorelista.ui.theme.LocalAppColors

/** Personvernerklæring (port av src/app/privacy.tsx). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    val sections = listOf(
        R.string.privacy_what_we_collect to R.string.privacy_what_we_collect_content,
        R.string.privacy_how_we_use to R.string.privacy_how_we_use_content,
        R.string.privacy_storage to R.string.privacy_storage_content,
        R.string.privacy_sharing to R.string.privacy_sharing_content,
        R.string.privacy_user_rights to R.string.privacy_user_rights_content,
        R.string.privacy_security to R.string.privacy_security_content,
        R.string.privacy_cookies to R.string.privacy_cookies_content,
        R.string.privacy_changes to R.string.privacy_changes_content,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.privacy_last_updated), style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                Text(stringResource(R.string.privacy_introduction), style = MaterialTheme.typography.bodyMedium)

                PrivacySection(stringResource(R.string.privacy_data_controller), divider = false) {
                    Text("svenway", style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
                    MailLink()
                }
                sections.forEach { (title, content) ->
                    PrivacySection(stringResource(title)) {
                        Text(stringResource(content), style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground, lineHeight = 22.sp)
                    }
                }
                PrivacySection(stringResource(R.string.privacy_contact)) {
                    Text(stringResource(R.string.privacy_contact_content), style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
                    MailLink()
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, divider: Boolean = true, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (divider) HorizontalDivider(color = LocalAppColors.current.border, modifier = Modifier.padding(bottom = 8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun MailLink() {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .clickable(role = Role.Button) {
                try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:${SupabaseConfig.CONTACT_EMAIL}".toUri()))
                } catch (_: ActivityNotFoundException) {
                }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.Email, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        Text(SupabaseConfig.CONTACT_EMAIL, style = MaterialTheme.typography.bodyMedium, color = colors.primary)
    }
}
