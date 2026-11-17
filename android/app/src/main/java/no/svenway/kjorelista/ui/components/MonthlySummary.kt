package no.svenway.kjorelista.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.svenway.kjorelista.R
import no.svenway.kjorelista.data.Format
import no.svenway.kjorelista.data.MonthStats
import no.svenway.kjorelista.data.Trip
import no.svenway.kjorelista.ui.currentLanguage
import no.svenway.kjorelista.ui.theme.LocalAppColors
import kotlin.math.roundToInt

private data class SummaryRow(val icon: ImageVector, val label: String, val value: String, val tint: Color? = null)
private data class Milestone(val icon: ImageVector, val label: String)

/** Flat oppsummering: to nøkkeltall øverst, detaljer som rader under (port av MonthlySummary.tsx). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlySummary(trips: List<Trip>, allTrips: List<Trip>, month: String, year: Int, loading: Boolean) {
    val colors = LocalAppColors.current
    val language = currentLanguage()
    val s = MonthStats.of(trips)
    val yearlyTrips = allTrips.filter { Format.parseDate(it.date).year == year }
    val yearlyKm = yearlyTrips.sumOf { it.kilometers }

    val rows = buildList {
        if (yearlyKm > 0) add(
            SummaryRow(
                Icons.Outlined.CalendarMonth,
                stringResource(R.string.summary_total_driven_in_year, year),
                "${Format.number(yearlyKm, language)} km · ${pluralStringResource(R.plurals.trip_count, yearlyTrips.size, yearlyTrips.size)}",
                colors.success,
            ),
        )
        if (s.tripsWithPassengers > 0) add(
            SummaryRow(
                Icons.Outlined.People,
                stringResource(R.string.summary_with_passengers),
                "${pluralStringResource(R.plurals.trip_count, s.tripsWithPassengers, s.tripsWithPassengers)} · ${Format.number(s.totalPassengerKilometers, language)} km",
            ),
        )
        if (s.totalExpenses > 0) add(
            SummaryRow(Icons.Outlined.CreditCard, stringResource(R.string.summary_expenses), "${Format.number(s.totalExpenses, language, 0)} kr"),
        )
    }

    val milestones = buildList {
        if (s.totalKilometers > 1000) add(Milestone(Icons.Outlined.EmojiEvents, "Over 1000 km"))
        if (s.totalTimeInHours >= 50) add(Milestone(Icons.Outlined.Schedule, "${s.totalTimeInHours.roundToInt()}+ ${stringResource(R.string.hours_word)}"))
        if (s.uniqueRoutes >= 5) add(Milestone(Icons.Outlined.Map, "${s.uniqueRoutes} ${stringResource(R.string.routes_word)}"))
        if (s.count >= 10) add(Milestone(Icons.Outlined.BarChart, pluralStringResource(R.plurals.trip_count, s.count, s.count)))
        if (yearlyKm > 5000) add(Milestone(Icons.Outlined.StarOutline, "${(yearlyKm / 1000).roundToInt()}k+ km ${stringResource(R.string.this_year)}"))
    }

    Surface(shape = RoundedCornerShape(20.dp), color = colors.card, modifier = Modifier.fillMaxWidth()) {
        Column {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
                trips.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = colors.mutedForeground, modifier = Modifier.size(32.dp))
                    Text(
                        stringResource(R.string.summary_no_trips_this_month, month.lowercase()),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.summary_add_first_trip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedForeground,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> Row(modifier = Modifier.height(IntrinsicSize.Min).padding(vertical = 16.dp)) {
                    Hero(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.summary_total_driven),
                        value = Format.number(s.totalKilometers, language),
                        unit = "km",
                        sub = "⌀ ${Format.number(s.avgKmPerTrip, language)} km ${stringResource(R.string.per_trip)}",
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp), color = colors.border)
                    Hero(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.summary_total_drive_time),
                        value = Format.duration(s.totalTimeInHours, language),
                        unit = null,
                        sub = "⌀ ${Format.duration(s.avgTimePerTrip, language)} ${stringResource(R.string.per_trip)}",
                    )
                }
            }

            if (!loading) rows.forEach { row ->
                RowDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clearAndSetSemantics { contentDescription = "${row.label}: ${row.value}" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(row.icon, contentDescription = null, tint = row.tint ?: colors.primary, modifier = Modifier.size(20.dp))
                    Text(row.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        row.value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = row.tint ?: MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    )
                }
            }

            if (!loading && trips.isNotEmpty() && milestones.isNotEmpty()) {
                RowDivider()
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    milestones.forEach { m ->
                        Row(
                            modifier = Modifier
                                .background(colors.muted, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(m.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                            Text(m.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(modifier: Modifier, label: String, value: String, unit: String?, sub: String) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clearAndSetSemantics { contentDescription = "$label: $value ${unit ?: ""}".trim() },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            if (unit != null) {
                Text(
                    unit,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.mutedForeground,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
        }
        Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
    }
}
