package no.svenway.kjorelista.ui.screens

import no.svenway.kjorelista.ui.theme.Brand
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import no.svenway.kjorelista.R
import no.svenway.kjorelista.data.AppStore
import no.svenway.kjorelista.data.Format
import no.svenway.kjorelista.data.Report
import no.svenway.kjorelista.data.Trip
import no.svenway.kjorelista.ui.Haptic
import no.svenway.kjorelista.ui.components.ConfirmDialog
import no.svenway.kjorelista.ui.components.DialogMessage
import no.svenway.kjorelista.ui.components.MessageDialog
import no.svenway.kjorelista.ui.components.MonthSwiper
import no.svenway.kjorelista.ui.components.MonthlySummary
import no.svenway.kjorelista.ui.components.TripRow
import no.svenway.kjorelista.ui.components.clickableRow
import no.svenway.kjorelista.ui.monthNames
import no.svenway.kjorelista.ui.rememberHaptics
import no.svenway.kjorelista.ui.theme.LocalAppColors
import java.time.YearMonth

private enum class ExportFormat { PDF, CSV }

/** Hovedskjerm (port av src/app/index.tsx). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    store: AppStore,
    onAddTrip: () -> Unit,
    onEditTrip: (Trip) -> Unit,
    onDuplicateTrip: (Trip) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val trips by store.trips.collectAsStateWithLifecycle()
    val profile by store.profile.collectAsStateWithLifecycle()
    val loaded by store.loaded.collectAsStateWithLifecycle()

    var current by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var refreshing by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Trip?>(null) }
    var message by remember { mutableStateOf<DialogMessage?>(null) }

    val monthName = monthNames()[current.monthValue - 1]
    val title = "$monthName ${current.year}"
    val isCurrentMonth = current == YearMonth.now()
    val monthTrips = remember(trips, current) {
        Format.tripsInMonth(trips, current.year, current.monthValue).sortedByDescending { it.date }
    }

    val errorTitle = stringResource(R.string.error_title)
    val deleteErrorTitle = stringResource(R.string.trips_delete_error)
    val exportTitle = stringResource(R.string.export_title)
    val noTripsToExport = stringResource(R.string.export_no_trips)

    // Feil ved automatisk henting av turer vises i en dialog (aldri stille)
    LaunchedEffect(store) {
        store.errors.collect {
            haptics(Haptic.ERROR)
            message = DialogMessage(errorTitle, it)
        }
    }

    fun runExport(format: ExportFormat) {
        exporting = true
        scope.launch {
            try {
                if (format == ExportFormat.PDF) Report.sharePdf(context, monthTrips, monthName, current.year, profile)
                else Report.shareCsv(context, monthTrips, monthName, current.year)
                haptics(Haptic.SUCCESS)
            } catch (e: Exception) {
                haptics(Haptic.ERROR)
                message = DialogMessage(errorTitle, e.message)
            } finally {
                exporting = false
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    // Ny måned: fold ut den store tittelen og gå til toppen. Ellers blir tittelen stående
    // sammenfoldet på en tom måned, der listen ikke kan scrolles for å folde den ut igjen.
    LaunchedEffect(current) {
        listState.scrollToItem(0)
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                actions = {
                    if (!isCurrentMonth) {
                        TextButton(onClick = {
                            haptics(Haptic.LIGHT)
                            current = YearMonth.now()
                        }) { Text(stringResource(R.string.today).uppercase(), fontWeight = FontWeight.SemiBold) }
                    }
                    IconButton(
                        enabled = !exporting,
                        onClick = {
                            if (monthTrips.isEmpty()) {
                                message = DialogMessage(exportTitle, noTripsToExport)
                            } else {
                                haptics(Haptic.LIGHT)
                                showExport = true
                            }
                        },
                    ) {
                        if (exporting) CircularProgressIndicator(modifier = Modifier.padding(12.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.summary_export))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptics(Haptic.LIGHT)
                    onAddTrip()
                },
                containerColor = Brand.gold,
                contentColor = Brand.onGold,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.trips_add_new))
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    try {
                        store.refreshTrips()
                    } catch (e: Exception) {
                        haptics(Haptic.ERROR)
                        message = DialogMessage(errorTitle, e.message)
                    } finally {
                        refreshing = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Ekstra luft nederst slik at FAB-en ikke skjuler siste tur
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = padding.calculateBottomPadding() + 104.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "summary") {
                    Column(
                        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        MonthSwiper(
                            label = title,
                            onPrevious = { current = current.minusMonths(1) },
                            onNext = { current = current.plusMonths(1) },
                        ) {
                            MonthlySummary(
                                trips = monthTrips,
                                allTrips = trips,
                                month = monthName,
                                year = current.year,
                                loading = !loaded,
                            )
                        }
                        if (monthTrips.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(stringResource(R.string.trips_this_month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    pluralStringResource(R.plurals.trip_count, monthTrips.size, monthTrips.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.mutedForeground,
                                )
                            }
                        }
                    }
                }

                itemsIndexed(monthTrips, key = { _, trip -> trip.id }) { index, trip ->
                    val first = index == 0
                    val last = index == monthTrips.lastIndex
                    val shape = RoundedCornerShape(
                        topStart = if (first) 16.dp else 0.dp,
                        topEnd = if (first) 16.dp else 0.dp,
                        bottomStart = if (last) 16.dp else 0.dp,
                        bottomEnd = if (last) 16.dp else 0.dp,
                    )
                    Surface(
                        shape = shape,
                        color = colors.card,
                        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().animateItem(),
                    ) {
                        Column {
                            if (!first) no.svenway.kjorelista.ui.components.RowDivider()
                            TripRow(
                                trip = trip,
                                onEdit = onEditTrip,
                                onDuplicate = onDuplicateTrip,
                                onDelete = {
                                    haptics(Haptic.WARNING)
                                    pendingDelete = it
                                },
                            )
                        }
                    }
                }

                if (monthTrips.isNotEmpty()) {
                    item(key = "hint") {
                        Text(
                            stringResource(R.string.trips_tap_to_edit),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }

    if (showExport) {
        ModalBottomSheet(onDismissRequest = { showExport = false }) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    "$exportTitle – $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                val itemColors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.export_pdf)) },
                    leadingContent = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = colors.primary) },
                    colors = itemColors,
                    modifier = Modifier.clickableRow {
                        showExport = false
                        runExport(ExportFormat.PDF)
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.export_csv)) },
                    supportingContent = { Text(stringResource(R.string.export_csv_description)) },
                    leadingContent = { Icon(Icons.Outlined.Description, contentDescription = null, tint = colors.primary) },
                    colors = itemColors,
                    modifier = Modifier.clickableRow {
                        showExport = false
                        runExport(ExportFormat.CSV)
                    },
                )
            }
        }
    }

    pendingDelete?.let { trip ->
        ConfirmDialog(
            title = stringResource(R.string.trips_confirm_delete),
            message = stringResource(R.string.trips_delete_message),
            confirmLabel = stringResource(R.string.trips_delete),
            destructive = true,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                scope.launch {
                    try {
                        store.deleteTrip(trip.id)
                        haptics(Haptic.SUCCESS)
                    } catch (e: Exception) {
                        haptics(Haptic.ERROR)
                        message = DialogMessage(deleteErrorTitle, e.message)
                    }
                }
            },
        )
    }

    message?.let { MessageDialog(it.title, it.message) { message = null } }
}
