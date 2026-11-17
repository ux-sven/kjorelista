package no.svenway.kjorelista.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import no.svenway.kjorelista.R
import no.svenway.kjorelista.data.AppStore
import no.svenway.kjorelista.data.Format
import no.svenway.kjorelista.data.NewTrip
import no.svenway.kjorelista.data.Preferences
import no.svenway.kjorelista.data.Trip
import no.svenway.kjorelista.ui.Haptic
import no.svenway.kjorelista.ui.components.ButtonRow
import no.svenway.kjorelista.ui.components.ConfirmDialog
import no.svenway.kjorelista.ui.components.DialogMessage
import no.svenway.kjorelista.ui.components.MessageDialog
import no.svenway.kjorelista.ui.components.RowDivider
import no.svenway.kjorelista.ui.components.Section
import no.svenway.kjorelista.ui.currentLanguage
import no.svenway.kjorelista.ui.rememberHaptics
import no.svenway.kjorelista.ui.theme.LocalAppColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Norsk tastatur gir komma som desimaltegn
private fun toNumber(v: String): Double = v.trim().replace(',', '.').toDoubleOrNull() ?: Double.NaN

private fun fromNumber(n: Double?): String = when {
    n == null || n == 0.0 -> ""
    n % 1.0 == 0.0 -> n.toLong().toString()
    else -> n.toString().replace('.', ',')
}

private fun fromInt(n: Int?): String = if (n == null || n == 0) "" else n.toString()

private enum class Field { KILOMETERS, ROUTE, DURATION, PASSENGERS }

/** Tidligere strekninger, mest brukte først; siste tur per strekning brukes til utfylling. */
internal fun knownRoutes(trips: List<Trip>): List<Trip> {
    data class Entry(var count: Int, var latest: Trip)
    val byRoute = LinkedHashMap<String, Entry>()
    for (trip in trips) {
        val key = trip.route.trim().lowercase()
        if (key.isEmpty()) continue
        val entry = byRoute[key]
        if (entry == null) byRoute[key] = Entry(1, trip)
        else {
            entry.count++
            if (trip.date > entry.latest.date) entry.latest = trip
        }
    }
    return byRoute.values
        .sortedWith(compareByDescending<Entry> { it.count }.thenByDescending { it.latest.date })
        .map { it.latest }
}

/** Tomt felt → 3 mest brukte; ellers opptil 4 treff (inneholder), treff i starten først. */
internal fun routeSuggestions(known: List<Trip>, input: String): List<Trip> {
    val query = input.trim().lowercase()
    if (query.isEmpty()) return known.take(3)
    return known
        .filter { val r = it.route.lowercase(); r.contains(query) && r != query }
        .sortedBy { if (it.route.lowercase().startsWith(query)) 0 else 1 }
        .take(4)
}

/**
 * Ny / rediger / dupliser tur (port av src/app/trip.tsx), som fullskjermdialog.
 * [editId] = rediger, [copyId] = ny tur forhåndsutfylt fra en eksisterende (med dagens dato).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(store: AppStore, preferences: Preferences, editId: String?, copyId: String?, onClose: () -> Unit) {
    val colors = LocalAppColors.current
    val language = currentLanguage()
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val trips by store.trips.collectAsStateWithLifecycle()
    val suggestionsEnabled by preferences.routeSuggestions.collectAsStateWithLifecycle()
    val autofillKilometers by preferences.autofillKilometers.collectAsStateWithLifecycle()

    val isEdit = editId != null
    val editing = remember(trips, editId) { trips.firstOrNull { it.id == editId } }
    // Startverdier leses én gang
    val source = remember { editing ?: trips.firstOrNull { it.id == copyId } }

    var date by rememberSaveable { mutableStateOf(editing?.date ?: LocalDate.now().toString()) }
    var kilometers by rememberSaveable { mutableStateOf(fromNumber(source?.kilometers)) }
    var route by rememberSaveable { mutableStateOf(source?.route ?: "") }
    // -1 = ikke valgt
    var durationH by rememberSaveable { mutableStateOf(source?.hours ?: -1) }
    var durationM by rememberSaveable { mutableStateOf(source?.minutes ?: -1) }
    var hasPassengers by rememberSaveable { mutableStateOf(source?.hasPassengers ?: false) }
    var passengerCount by rememberSaveable { mutableStateOf(fromInt(source?.passengerCount)) }
    var passengerKilometers by rememberSaveable { mutableStateOf(fromNumber(source?.passengerKilometers)) }
    var otherExpenses by rememberSaveable { mutableStateOf(fromNumber(source?.otherExpenses)) }
    var expenseDescription by rememberSaveable { mutableStateOf(source?.expenseDescription ?: "") }
    var comment by rememberSaveable { mutableStateOf(source?.comment ?: "") }

    var errors by remember { mutableStateOf<Map<Field, String>>(emptyMap()) }
    var saving by remember { mutableStateOf(false) }
    var routeFocused by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<DialogMessage?>(null) }

    val hasDuration = durationH >= 0 && durationM >= 0
    val kmFocus = remember { FocusRequester() }
    val routeFocus = remember { FocusRequester() }

    fun clearError(field: Field) {
        if (field in errors) errors = errors - field
    }

    val known = remember(trips) { knownRoutes(trips) }
    val suggestions = if (suggestionsEnabled && routeFocused) routeSuggestions(known, route) else emptyList()

    fun pickRoute(trip: Trip) {
        haptics(Haptic.LIGHT)
        route = trip.route
        clearError(Field.ROUTE)
        // Kilometer fra forrige tur på strekningen, hvis valgt i innstillingene og feltet er tomt
        if (autofillKilometers && kilometers.isBlank()) {
            kilometers = fromNumber(trip.kilometers)
            clearError(Field.KILOMETERS)
        }
        focusManager.clearFocus()
    }

    val hoursShort = stringResource(R.string.hours_short)
    val minutesShort = stringResource(R.string.minutes_short)
    val durationText = if (hasDuration) {
        listOfNotNull(
            if (durationH > 0) "$durationH $hoursShort" else null,
            if (durationM > 0 || durationH == 0) "$durationM $minutesShort" else null,
        ).joinToString(" ")
    } else null

    val kmRequired = stringResource(R.string.trips_kilometers_required)
    val routeRequired = stringResource(R.string.trips_route_required)
    val hoursRequired = stringResource(R.string.trips_hours_required)
    val passengerCountRequired = stringResource(R.string.trips_passenger_count_required)
    val passengerKmRequired = stringResource(R.string.trips_passenger_kilometers_required)
    val saveErrorTitle = stringResource(if (isEdit) R.string.trips_update_error else R.string.trips_save_error)
    val deleteErrorTitle = stringResource(R.string.trips_delete_error)

    // Samme regler som webskjemaet (kjoreliste-web TripForm.tsx)
    fun validate(): Map<Field, String> = buildMap {
        if (!(toNumber(kilometers) > 0)) put(Field.KILOMETERS, kmRequired)
        if (route.isBlank()) put(Field.ROUTE, routeRequired)
        if (!hasDuration) put(Field.DURATION, hoursRequired)
        if (hasPassengers) {
            if (!((passengerCount.trim().toIntOrNull() ?: 0) > 0)) put(Field.PASSENGERS, passengerCountRequired)
            else if (!(toNumber(passengerKilometers) > 0)) put(Field.PASSENGERS, passengerKmRequired)
        }
    }

    fun save() {
        val e = validate()
        errors = e
        if (e.isNotEmpty()) {
            haptics(Haptic.ERROR)
            return
        }
        val expenses = toNumber(otherExpenses).takeUnless { it.isNaN() } ?: 0.0
        val data = NewTrip(
            date = date,
            kilometers = toNumber(kilometers),
            route = route.trim(),
            hours = durationH,
            minutes = durationM,
            hasPassengers = hasPassengers,
            passengerCount = if (hasPassengers) passengerCount.trim().toInt() else 0,
            passengerKilometers = if (hasPassengers) toNumber(passengerKilometers) else 0.0,
            otherExpenses = expenses,
            expenseDescription = expenseDescription.trim(),
            comment = comment.trim(),
        )
        saving = true
        scope.launch {
            try {
                if (editId != null) store.updateTrip(data.withId(editId)) else store.addTrip(data)
                haptics(Haptic.SUCCESS)
                onClose()
            } catch (ex: Exception) {
                haptics(Haptic.ERROR)
                message = DialogMessage(saveErrorTitle, ex.message)
            } finally {
                saving = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isEdit) runCatching { routeFocus.requestFocus() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEdit) R.string.edit_trip_title else R.string.new_trip_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.trips_cancel))
                    }
                },
                actions = {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = ::save, modifier = Modifier.padding(end = 4.dp)) {
                            Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Tur
                Section(title = stringResource(R.string.section_trip), error = errors[Field.ROUTE] ?: errors[Field.KILOMETERS]) {
                    ValueRow(
                        label = stringResource(R.string.trips_date),
                        value = Format.longDate(date, language),
                        onClick = {
                            haptics(Haptic.LIGHT)
                            showDatePicker = true
                        },
                    )
                    RowDivider()
                    // Strekning først, med forslag rett under, deretter kilometer
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                        OutlinedTextField(
                            value = route,
                            onValueChange = { route = it; clearError(Field.ROUTE) },
                            label = { Text(stringResource(R.string.trips_route)) },
                            placeholder = { Text(stringResource(R.string.route_hint)) },
                            isError = Field.ROUTE in errors,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(onNext = { kmFocus.requestFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(routeFocus)
                                .onFocusChanged { routeFocused = it.isFocused },
                        )
                    }
                    suggestions.forEach { trip ->
                        SuggestionRow(
                            title = trip.route,
                            subtitle = "${Format.number(trip.kilometers, language)} km · ${Format.clock(trip.hours, trip.minutes)}",
                            onClick = { pickRoute(trip) },
                        )
                    }
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = kilometers,
                            onValueChange = { kilometers = it; clearError(Field.KILOMETERS) },
                            label = { Text(stringResource(R.string.trips_kilometers)) },
                            placeholder = { Text("0") },
                            suffix = { Text("km") },
                            isError = Field.KILOMETERS in errors,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(kmFocus),
                        )
                    }
                }

                // Reisetid
                Section(
                    title = stringResource(R.string.section_time),
                    error = errors[Field.DURATION],
                    footer = stringResource(R.string.time_footer),
                ) {
                    ValueRow(
                        label = stringResource(R.string.duration),
                        value = durationText,
                        placeholder = stringResource(R.string.choose),
                        invalid = Field.DURATION in errors,
                        onClick = {
                            haptics(Haptic.LIGHT)
                            clearError(Field.DURATION)
                            showDurationPicker = true
                        },
                    )
                }

                // Passasjerer
                Section(title = stringResource(R.string.section_passengers), error = errors[Field.PASSENGERS]) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.trips_had_passengers)) },
                        supportingContent = { Text(stringResource(R.string.trips_passengers_description)) },
                        trailingContent = { Switch(checked = hasPassengers, onCheckedChange = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(role = Role.Switch) {
                            hasPassengers = !hasPassengers
                            clearError(Field.PASSENGERS)
                            haptics(Haptic.MEDIUM)
                        },
                    )
                    if (hasPassengers) {
                        RowDivider()
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = passengerCount,
                                onValueChange = { passengerCount = it; clearError(Field.PASSENGERS) },
                                label = { Text(stringResource(R.string.count)) },
                                placeholder = { Text("1") },
                                isError = Field.PASSENGERS in errors,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = passengerKilometers,
                                onValueChange = { passengerKilometers = it; clearError(Field.PASSENGERS) },
                                label = { Text(stringResource(R.string.km_with_passengers)) },
                                placeholder = { Text("0") },
                                suffix = { Text("km") },
                                isError = Field.PASSENGERS in errors,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // Utgifter
                Section(title = stringResource(R.string.section_expenses)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = otherExpenses,
                            onValueChange = { otherExpenses = it },
                            label = { Text(stringResource(R.string.amount)) },
                            placeholder = { Text("0") },
                            suffix = { Text("kr") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = expenseDescription,
                            onValueChange = { expenseDescription = it },
                            label = { Text(stringResource(R.string.trips_expense_description)) },
                            placeholder = { Text(stringResource(R.string.trips_expense_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Kommentar
                Section(title = stringResource(R.string.section_comment)) {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(R.string.trips_comment)) },
                        placeholder = { Text(stringResource(R.string.trips_comment_placeholder)) },
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }

                if (isEdit) {
                    Section {
                        ButtonRow(
                            title = stringResource(R.string.trips_confirm_delete),
                            destructive = true,
                            enabled = !saving,
                            onClick = {
                                haptics(Haptic.WARNING)
                                confirmDelete = true
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker bruker UTC-millisekunder; konverter til lokal dato uten tidssone-forskyvning
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    showDatePicker = false
                }) { Text(stringResource(R.string.general_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.trips_cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }

    if (showDurationPicker) {
        val state = rememberTimePickerState(
            initialHour = if (hasDuration) durationH else 0,
            initialMinute = if (hasDuration) durationM else 15,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDurationPicker = false },
            title = { Text(stringResource(R.string.duration)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    durationH = state.hour
                    durationM = state.minute
                    showDurationPicker = false
                }) { Text(stringResource(R.string.general_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDurationPicker = false }) { Text(stringResource(R.string.trips_cancel)) } },
        )
    }

    if (confirmDelete && editId != null) {
        ConfirmDialog(
            title = stringResource(R.string.trips_confirm_delete),
            message = stringResource(R.string.trips_delete_message),
            confirmLabel = stringResource(R.string.trips_delete),
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                scope.launch {
                    try {
                        store.deleteTrip(editId)
                        haptics(Haptic.SUCCESS)
                        onClose()
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

/** Rad som viser en verdi og åpner en velger. */
@Composable
private fun ValueRow(label: String, value: String?, onClick: () -> Unit, placeholder: String? = null, invalid: Boolean = false) {
    val colors = LocalAppColors.current
    ListItem(
        headlineContent = { Text(label, color = if (invalid) colors.destructive else MaterialTheme.colorScheme.onSurface) },
        trailingContent = {
            Text(
                value ?: placeholder ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = if (value != null) MaterialTheme.colorScheme.onSurface else colors.primary,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick),
    )
}

/** Forslag under strekningsfeltet (tidligere strekninger). */
@Composable
private fun SuggestionRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(Icons.Outlined.History, contentDescription = null, tint = colors.mutedForeground) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.CallMade, contentDescription = null, tint = colors.mutedForeground, modifier = Modifier.size(18.dp)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick),
    )
}
