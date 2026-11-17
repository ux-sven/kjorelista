package no.svenway.kjorelista.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.svenway.kjorelista.R
import no.svenway.kjorelista.ui.theme.LocalAppColors

/** Gruppert seksjon i Material-stil: tittel i primærfarge, avrundet flate, valgfri bunntekst/feil. */
@Composable
fun Section(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
            )
        }
        Surface(shape = RoundedCornerShape(16.dp), color = colors.card, modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
        when {
            error != null -> Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = colors.destructive,
                modifier = Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
            footer != null -> Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = LocalAppColors.current.border)
}

/** Knapperad midtstilt i en seksjon (f.eks. «Logg ut», «Slett kjøretur»). */
@Composable
fun ButtonRow(title: String, onClick: () -> Unit, destructive: Boolean = false, enabled: Boolean = true) {
    val colors = LocalAppColors.current
    Text(
        text = title,
        color = if (destructive) colors.destructive else colors.primary,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(16.dp),
    )
}

/** Enkel feil-/infodialog (tilsvarer Alert.alert med én knapp). */
@Composable
fun MessageDialog(title: String, message: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = message?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.general_ok)) } },
    )
}

/** Bekreftelsesdialog med Avbryt og en (ev. destruktiv) handling. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(confirmLabel, color = if (destructive) colors.destructive else colors.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.trips_cancel)) } },
    )
}

data class DialogMessage(val title: String, val message: String?)

/** Klikkbar rad med knapperolle (brukes på ListItem). */
fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(role = Role.Button, onClick = onClick))
