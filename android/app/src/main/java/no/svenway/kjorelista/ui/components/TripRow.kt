package no.svenway.kjorelista.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import no.svenway.kjorelista.R
import no.svenway.kjorelista.data.Format
import no.svenway.kjorelista.data.Trip
import no.svenway.kjorelista.ui.Haptic
import no.svenway.kjorelista.ui.currentLanguage
import no.svenway.kjorelista.ui.rememberHaptics
import no.svenway.kjorelista.ui.theme.LocalAppColors

/** Én tur i listen. Trykk = rediger, langt trykk = meny (Rediger / Dupliser / Slett). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripRow(
    trip: Trip,
    onEdit: (Trip) -> Unit,
    onDuplicate: (Trip) -> Unit,
    onDelete: (Trip) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val language = currentLanguage()
    val haptics = rememberHaptics()
    var menuOpen by remember { mutableStateOf(false) }
    val date = Format.shortDateWithWeekday(trip.date, language)
    val hasTime = trip.hours > 0 || trip.minutes > 0
    val editLabel = stringResource(R.string.trips_edit)
    val duplicateLabel = stringResource(R.string.duplicate)
    val deleteLabel = stringResource(R.string.trips_delete)
    val kmText = "${Format.number(trip.kilometers, language)} km"

    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = editLabel,
                    onClick = { onEdit(trip) },
                    onLongClick = {
                        haptics(Haptic.MEDIUM)
                        menuOpen = true
                    },
                )
                .semantics(mergeDescendants = true) {
                    customActions = listOf(
                        CustomAccessibilityAction(duplicateLabel) { onDuplicate(trip); true },
                        CustomAccessibilityAction(deleteLabel) { onDelete(trip); true },
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(trip.route, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (trip.hasPassengers || trip.otherExpenses > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 2.dp)) {
                            if (trip.hasPassengers) {
                                val km = if (trip.passengerKilometers > 0) " • ${Format.number(trip.passengerKilometers, language)} km" else ""
                                Meta(Icons.Outlined.People, "${trip.passengerCount}$km")
                            }
                            if (trip.otherExpenses > 0) {
                                Meta(Icons.Outlined.CreditCard, "${Format.number(trip.otherExpenses, language, 0)} kr")
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        kmText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                        modifier = Modifier
                            .background(colors.primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, colors.primary.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    if (hasTime) Meta(Icons.Outlined.Schedule, Format.clock(trip.hours, trip.minutes))
                }
            }
            if (trip.comment.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.warning.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, colors.warning.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp),
                    )
                    Text(trip.comment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            Text(
                "$date – ${trip.route}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.mutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            DropdownMenuItem(
                text = { Text(editLabel) },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = { menuOpen = false; onEdit(trip) },
            )
            DropdownMenuItem(
                text = { Text(duplicateLabel) },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = { menuOpen = false; onDuplicate(trip) },
            )
            DropdownMenuItem(
                text = { Text(deleteLabel, color = colors.destructive) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.destructive) },
                onClick = { menuOpen = false; onDelete(trip) },
            )
        }
    }
}

@Composable
private fun Meta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = colors.mutedForeground, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
    }
}
