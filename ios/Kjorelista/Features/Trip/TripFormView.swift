import SwiftUI

/// Port av kjoreliste-app/src/app/trip.tsx: ny, rediger eller dupliser tur.
struct TripFormView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(SessionStore.self) private var session
    @Environment(\.dismiss) private var dismiss

    let mode: TripSheet

    private struct Duration: Equatable { var h: Int; var m: Int }
    private enum Field: Hashable { case kilometers, route, passengerCount, passengerKm, amount, expenseDescription, comment }
    private enum ErrorKey: Hashable { case kilometers, route, duration, passengers }

    @State private var date: Date
    @State private var kilometers: String
    @State private var route: String
    @State private var duration: Duration?
    @State private var showDurationPicker = false
    @State private var hasPassengers: Bool
    @State private var passengerCount: String
    @State private var passengerKilometers: String
    @State private var otherExpenses: String
    @State private var expenseDescription: String
    @State private var comment: String
    @State private var errors: [ErrorKey: String] = [:]
    @State private var saving = false
    @State private var confirmDelete = false
    @State private var alert: AlertInfo?
    @FocusState private var focus: Field?

    init(mode: TripSheet) {
        self.mode = mode
        let source: Trip? = switch mode {
        case .new: nil
        case let .edit(trip), let .duplicate(trip): trip
        }
        var editingDate: Date?
        if case let .edit(trip) = mode { editingDate = Format.parseDate(trip.date) }
        _date = State(initialValue: editingDate ?? Date())
        _kilometers = State(initialValue: Format.decimalField(source?.kilometers ?? 0))
        _route = State(initialValue: source?.route ?? "")
        _duration = State(initialValue: source.map { Duration(h: $0.hours, m: $0.minutes) })
        _hasPassengers = State(initialValue: source?.hasPassengers ?? false)
        _passengerCount = State(initialValue: (source?.passengerCount ?? 0) > 0 ? String(source!.passengerCount) : "")
        _passengerKilometers = State(initialValue: Format.decimalField(source?.passengerKilometers ?? 0))
        _otherExpenses = State(initialValue: Format.decimalField(source?.otherExpenses ?? 0))
        _expenseDescription = State(initialValue: source?.expenseDescription ?? "")
        _comment = State(initialValue: source?.comment ?? "")
    }

    private var editing: Trip? {
        if case let .edit(trip) = mode { return trip }
        return nil
    }

    // MARK: - Forslag til strekning

    private struct KnownRoute { var count: Int; var latest: Trip }

    /// Tidligere strekninger, mest brukte først; siste tur per strekning brukes til utfylling.
    private var knownRoutes: [KnownRoute] {
        var byRoute: [String: KnownRoute] = [:]
        for trip in session.trips {
            let key = trip.route.trimmingCharacters(in: .whitespaces).lowercased()
            if key.isEmpty { continue }
            if var entry = byRoute[key] {
                entry.count += 1
                if trip.date > entry.latest.date { entry.latest = trip }
                byRoute[key] = entry
            } else {
                byRoute[key] = KnownRoute(count: 1, latest: trip)
            }
        }
        return byRoute.values.sorted {
            $0.count != $1.count ? $0.count > $1.count : $0.latest.date > $1.latest.date
        }
    }

    private var routeSuggestions: [Trip] {
        guard settings.routeSuggestions, focus == .route else { return [] }
        let query = route.trimmingCharacters(in: .whitespaces).lowercased()
        let known = knownRoutes
        if query.isEmpty { return known.prefix(3).map(\.latest) }
        let matches = known.filter {
            let r = $0.latest.route.lowercased()
            return r.contains(query) && r != query
        }
        // Treff i starten av strekningen først (stabil sortering)
        let starts = matches.filter { $0.latest.route.lowercased().hasPrefix(query) }
        let others = matches.filter { !$0.latest.route.lowercased().hasPrefix(query) }
        return (starts + others).prefix(4).map(\.latest)
    }

    private func pickRoute(_ trip: Trip) {
        Haptic.light.play()
        route = trip.route
        errors[.route] = nil
        // Kilometer fra forrige tur på strekningen, hvis valgt i innstillingene og feltet er tomt
        if settings.autofillKilometers, kilometers.trimmingCharacters(in: .whitespaces).isEmpty {
            kilometers = Format.decimalField(trip.kilometers)
            errors[.kilometers] = nil
        }
        focus = nil
    }

    // MARK: - Visning

    var body: some View {
        let t = settings.t
        let s = settings.s
        let lang = settings.language

        NavigationStack {
            Form {
                Section {
                    DatePicker(t.trips.date, selection: $date, displayedComponents: .date)
                        .foregroundStyle(AppColor.foreground)
                    TextField(
                        "",
                        text: $route,
                        prompt: Text("\(t.trips.route): \(s.routeHint)")
                            .foregroundStyle(errors[.route] != nil ? AppColor.destructive : AppColor.mutedForeground)
                    )
                    .textInputAutocapitalization(.words)
                    .autocorrectionDisabled()
                    .submitLabel(.next)
                    .onSubmit { focus = .kilometers }
                    .focused($focus, equals: .route)
                    .accessibilityLabel(t.trips.route)
                    .onChange(of: route) { errors[.route] = nil }

                    ForEach(routeSuggestions) { trip in
                        Button {
                            pickRoute(trip)
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: "clock.arrow.circlepath")
                                    .foregroundStyle(AppColor.mutedForeground)
                                VStack(alignment: .leading, spacing: 1) {
                                    Text(trip.route)
                                        .foregroundStyle(AppColor.foreground)
                                        .lineLimit(1)
                                    Text("\(Format.number(trip.kilometers, lang)) km · \(Format.clock(hours: trip.hours, minutes: trip.minutes))")
                                        .font(.footnote)
                                        .foregroundStyle(AppColor.mutedForeground)
                                }
                                Spacer()
                                Image(systemName: "arrow.up.left")
                                    .font(.footnote)
                                    .foregroundStyle(AppColor.mutedForeground)
                            }
                        }
                        .accessibilityLabel("\(trip.route), \(Format.number(trip.kilometers, lang)) km")
                    }
                    HStack {
                        Text(t.trips.kilometers)
                            .foregroundStyle(errors[.kilometers] != nil ? AppColor.destructive : AppColor.foreground)
                        TextField("0", text: $kilometers)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .focused($focus, equals: .kilometers)
                            .accessibilityLabel(t.trips.kilometers)
                            .onChange(of: kilometers) { errors[.kilometers] = nil }
                        Text("km").foregroundStyle(AppColor.mutedForeground)
                    }
                } header: {
                    Text(s.sectionTrip)
                } footer: {
                    if let error = errors[.route] ?? errors[.kilometers] {
                        Text(error).foregroundStyle(AppColor.destructive)
                    }
                }
                .listRowBackground(AppColor.card)

                Section {
                    Button {
                        toggleDurationPicker()
                    } label: {
                        HStack {
                            Text(s.duration)
                                .foregroundStyle(errors[.duration] != nil ? AppColor.destructive : AppColor.foreground)
                            Spacer()
                            Text(durationText ?? s.choose)
                                .foregroundStyle(showDurationPicker ? AppColor.primary : duration != nil ? AppColor.foreground : AppColor.mutedForeground)
                        }
                    }
                    .accessibilityLabel("\(s.duration): \(durationText ?? s.choose)")

                    if showDurationPicker {
                        DurationWheel(
                            hours: Binding(get: { duration?.h ?? 0 }, set: { duration = Duration(h: $0, m: duration?.m ?? 0) }),
                            minutes: Binding(get: { duration?.m ?? 0 }, set: { duration = Duration(h: duration?.h ?? 0, m: $0) }),
                            hoursLabel: s.hoursShort,
                            minutesLabel: s.minutesShort,
                            hoursA11y: t.trips.hours,
                            minutesA11y: t.trips.minutes
                        )
                    }
                } header: {
                    Text(s.sectionTime)
                } footer: {
                    if let error = errors[.duration] {
                        Text(error).foregroundStyle(AppColor.destructive)
                    } else {
                        Text(s.timeFooter)
                    }
                }
                .listRowBackground(AppColor.card)

                Section {
                    Toggle(t.trips.hadPassengers, isOn: $hasPassengers)
                        .tint(AppColor.success)
                        .accessibilityHint(t.trips.passengersDescription)
                        .onChange(of: hasPassengers) {
                            errors[.passengers] = nil
                            Haptic.medium.play()
                        }
                    if hasPassengers {
                        HStack {
                            Text(s.count)
                                .foregroundStyle(errors[.passengers] != nil ? AppColor.destructive : AppColor.foreground)
                            TextField("1", text: $passengerCount)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing)
                                .focused($focus, equals: .passengerCount)
                                .accessibilityLabel(s.count)
                                .onChange(of: passengerCount) { errors[.passengers] = nil }
                        }
                        HStack {
                            Text(s.kmWithPassengers)
                                .foregroundStyle(errors[.passengers] != nil ? AppColor.destructive : AppColor.foreground)
                            TextField("0", text: $passengerKilometers)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .focused($focus, equals: .passengerKm)
                                .accessibilityLabel(s.kmWithPassengers)
                                .onChange(of: passengerKilometers) { errors[.passengers] = nil }
                            Text("km").foregroundStyle(AppColor.mutedForeground)
                        }
                    }
                } header: {
                    Text(s.sectionPassengers)
                } footer: {
                    if let error = errors[.passengers] {
                        Text(error).foregroundStyle(AppColor.destructive)
                    }
                }
                .listRowBackground(AppColor.card)

                Section(s.sectionExpenses) {
                    HStack {
                        Text(s.amount).foregroundStyle(AppColor.foreground)
                        TextField("0", text: $otherExpenses)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .focused($focus, equals: .amount)
                            .accessibilityLabel(s.amount)
                        Text("kr").foregroundStyle(AppColor.mutedForeground)
                    }
                    TextField(t.trips.expenseDescription, text: $expenseDescription, prompt: Text(t.trips.expensePlaceholder))
                        .focused($focus, equals: .expenseDescription)
                        .accessibilityLabel(t.trips.expenseDescription)
                }
                .listRowBackground(AppColor.card)

                Section(s.sectionComment) {
                    TextField(t.trips.comment, text: $comment, prompt: Text(t.trips.commentPlaceholder), axis: .vertical)
                        .lineLimit(4...10)
                        .focused($focus, equals: .comment)
                        .accessibilityLabel(t.trips.comment)
                }
                .listRowBackground(AppColor.card)

                if editing != nil {
                    Section {
                        Button(t.trips.confirmDelete, role: .destructive) {
                            Haptic.warning.play()
                            confirmDelete = true
                        }
                        .foregroundStyle(AppColor.destructive)
                        .frame(maxWidth: .infinity)
                        .disabled(saving)
                    }
                    .listRowBackground(AppColor.card)
                }
            }
            .themedList()
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle(editing != nil ? s.editTripTitle : s.newTripTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(t.trips.cancel) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if saving {
                        ProgressView()
                    } else {
                        Button(s.save, action: save)
                            .fontWeight(.semibold)
                    }
                }
            }
            .alert(t.trips.confirmDelete, isPresented: $confirmDelete) {
                Button(t.trips.cancel, role: .cancel) {}
                Button(t.trips.delete, role: .destructive, action: remove)
            } message: {
                Text(t.trips.deleteMessage)
            }
            .alert(item: $alert) { info in
                Alert(title: Text(info.title), message: Text(info.message))
            }
            .task {
                // Strekning først: feltet får fokus når en ny tur åpnes
                if editing == nil {
                    try? await Task.sleep(for: .milliseconds(450))
                    focus = .route
                }
            }
        }
        .interactiveDismissDisabled(saving)
    }

    private var durationText: String? {
        guard let d = duration else { return nil }
        let s = settings.s
        var parts: [String] = []
        if d.h > 0 { parts.append("\(d.h) \(s.hoursShort)") }
        if d.m > 0 || d.h == 0 { parts.append("\(d.m) \(s.minutesShort)") }
        return parts.joined(separator: " ")
    }

    private func toggleDurationPicker() {
        Haptic.light.play()
        errors[.duration] = nil
        focus = nil
        if duration == nil { duration = Duration(h: 0, m: 15) }
        withAnimation { showDurationPicker.toggle() }
    }

    // MARK: - Lagring

    /// Samme regler som webskjemaet (kjoreliste-web/src/app/components/TripForm.tsx).
    private func validate() -> [ErrorKey: String] {
        let t = settings.t
        var e: [ErrorKey: String] = [:]
        if !((Format.parseDecimal(kilometers) ?? 0) > 0) { e[.kilometers] = t.trips.kilometersRequired }
        if route.trimmingCharacters(in: .whitespaces).isEmpty { e[.route] = t.trips.routeRequired }
        if duration == nil { e[.duration] = t.trips.hoursRequired }
        if hasPassengers {
            if !((Int(passengerCount.trimmingCharacters(in: .whitespaces)) ?? 0) > 0) {
                e[.passengers] = t.trips.passengerCountRequired
            } else if !((Format.parseDecimal(passengerKilometers) ?? 0) > 0) {
                e[.passengers] = t.trips.passengerKilometersRequired
            }
        }
        return e
    }

    private func save() {
        let e = validate()
        errors = e
        guard e.isEmpty, let duration else {
            Haptic.error.play()
            return
        }
        let data = NewTrip(
            date: Format.toDateString(date),
            kilometers: Format.parseDecimal(kilometers) ?? 0,
            route: route.trimmingCharacters(in: .whitespaces),
            hours: duration.h,
            minutes: duration.m,
            hasPassengers: hasPassengers,
            passengerCount: hasPassengers ? Int(passengerCount.trimmingCharacters(in: .whitespaces)) ?? 0 : 0,
            passengerKilometers: hasPassengers ? Format.parseDecimal(passengerKilometers) ?? 0 : 0,
            otherExpenses: Format.parseDecimal(otherExpenses) ?? 0,
            expenseDescription: expenseDescription.trimmingCharacters(in: .whitespaces),
            comment: comment.trimmingCharacters(in: .whitespacesAndNewlines)
        )
        saving = true
        focus = nil
        Task {
            defer { saving = false }
            do {
                if let editing {
                    try await session.updateTrip(data.withId(editing.id))
                } else {
                    try await session.addTrip(data)
                }
                Haptic.success.play()
                dismiss()
            } catch {
                Haptic.error.play()
                let t = settings.t
                alert = AlertInfo(title: editing != nil ? t.trips.tripUpdateError : t.trips.tripSaveError, message: error.localizedDescription)
            }
        }
    }

    private func remove() {
        guard let editing else { return }
        Task {
            do {
                try await session.deleteTrip(editing.id)
                Haptic.success.play()
                dismiss()
            } catch {
                Haptic.error.play()
                alert = AlertInfo(title: settings.t.trips.tripDeleteError, message: error.localizedDescription)
            }
        }
    }
}

/// Hjul for timer og minutter (24-timers), som tidsvelgeren i Expo-appen.
private struct DurationWheel: View {
    @Binding var hours: Int
    @Binding var minutes: Int
    let hoursLabel: String
    let minutesLabel: String
    let hoursA11y: String
    let minutesA11y: String

    var body: some View {
        HStack(spacing: 0) {
            Picker(hoursA11y, selection: $hours) {
                ForEach(0..<24, id: \.self) { Text("\($0) \(hoursLabel)").tag($0) }
            }
            .pickerStyle(.wheel)
            .accessibilityLabel(hoursA11y)
            Picker(minutesA11y, selection: $minutes) {
                ForEach(0..<60, id: \.self) { Text("\($0) \(minutesLabel)").tag($0) }
            }
            .pickerStyle(.wheel)
            .accessibilityLabel(minutesA11y)
        }
        .frame(height: 180)
    }
}
