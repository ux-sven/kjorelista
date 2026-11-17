import SwiftUI

/// Hva trip-skjemaet skal vise.
enum TripSheet: Identifiable {
    case new
    case edit(Trip)
    case duplicate(Trip)

    var id: String {
        switch self {
        case .new: "new"
        case let .edit(trip): "edit-\(trip.id)"
        case let .duplicate(trip): "copy-\(trip.id)"
        }
    }
}

struct AlertInfo: Identifiable {
    let id = UUID()
    let title: String
    let message: String
}

/// Port av kjoreliste-app/src/app/index.tsx.
struct HomeView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(SessionStore.self) private var session

    @State private var current = Calendar.current.date(from: Calendar.current.dateComponents([.year, .month], from: Date())) ?? Date()
    @State private var tripSheet: TripSheet?
    @State private var showSettings = false
    @State private var pendingDelete: Trip?
    @State private var alert: AlertInfo?
    @State private var showExportDialog = false
    @State private var exporting = false
    @State private var shareURL: ShareItem?

    private var year: Int { Calendar.current.component(.year, from: current) }
    /// 0–11, som i JavaScript
    private var month: Int { Calendar.current.component(.month, from: current) - 1 }
    private var monthName: String { settings.t.months[month] }
    /// «Oktober 2026» (årstallet uten tusenskille)
    private var monthTitle: String { "\(monthName) \(String(year))" }
    private var isCurrentMonth: Bool {
        let now = Calendar.current.dateComponents([.year, .month], from: Date())
        return now.year == year && now.month == month + 1
    }
    private var monthTrips: [Trip] { session.trips.inMonth(year: year, month: month) }

    var body: some View {
        let t = settings.t
        let s = settings.s
        let trips = monthTrips

        NavigationStack {
            List {
                Section {
                    MonthSwiper(label: monthTitle, onChange: navigateMonth) {
                        if !session.hasLoadedTrips && session.loadingTrips {
                            ProgressView()
                                .controlSize(.large)
                                .frame(maxWidth: .infinity, minHeight: 140)
                                .background(RoundedRectangle(cornerRadius: 16).fill(AppColor.card))
                        } else if let error = session.loadError, !session.hasLoadedTrips {
                            Banner(text: error, tint: AppColor.destructive)
                        } else {
                            MonthlySummaryView(trips: trips, allTrips: session.trips, monthName: monthName, year: year)
                        }
                    }
                }
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 0, trailing: 16))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

                if !trips.isEmpty {
                    Section {
                        ForEach(trips) { trip in
                            row(trip)
                        }
                    } header: {
                        HStack(alignment: .firstTextBaseline) {
                            Text(t.trips.thisMonth)
                                .font(.body.weight(.semibold))
                                .foregroundStyle(AppColor.foreground)
                            Spacer()
                            Text("\(trips.count) \(trips.count == 1 ? t.trips.trip : t.trips.tripsPlural)")
                                .font(.subheadline)
                                .foregroundStyle(AppColor.mutedForeground)
                        }
                        .textCase(nil)
                        .padding(.horizontal, -4)
                    } footer: {
                        Text(t.trips.swipeInstructions)
                            .font(.footnote)
                            .foregroundStyle(AppColor.mutedForeground)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 4)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .listSectionSpacing(16)
            .themedList()
            .contentMargins(.bottom, 96, for: .scrollContent)
            .refreshable { await refresh() }
            .navigationTitle(monthTitle)
            .toolbar { toolbar }
            .overlay(alignment: .bottomTrailing) { fab }
        }
        .sheet(item: $tripSheet) { sheet in
            TripFormView(mode: sheet)
                .preferredColorScheme(settings.theme.colorScheme)
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
                .preferredColorScheme(settings.theme.colorScheme)
        }
        .sheet(item: $shareURL) { item in
            ActivityView(items: [item.url])
                .presentationDetents([.medium, .large])
                .ignoresSafeArea()
        }
        .confirmationDialog(
            Text(verbatim: "\(t.export.title) – \(monthTitle)"),
            isPresented: $showExportDialog,
            titleVisibility: .visible
        ) {
            Button(s.pdf) { runExport(.pdf) }
            Button(t.export.csv) { runExport(.csv) }
            Button(t.trips.cancel, role: .cancel) {}
        }
        .alert(
            t.trips.confirmDelete,
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
            presenting: pendingDelete
        ) { trip in
            Button(t.trips.cancel, role: .cancel) {}
            Button(t.trips.delete, role: .destructive) { delete(trip) }
        } message: { _ in
            Text(t.trips.deleteMessage)
        }
        .alert(item: $alert) { info in
            Alert(title: Text(info.title), message: Text(info.message))
        }
    }

    // MARK: - Rader

    private func row(_ trip: Trip) -> some View {
        let t = settings.t
        let s = settings.s
        return Button {
            Haptic.light.play()
            tripSheet = .edit(trip)
        } label: {
            TripRowView(trip: trip)
        }
        .buttonStyle(.plain)
        .listRowBackground(AppColor.card)
        .listRowSeparatorTint(AppColor.border)
        .contextMenu {
            Button(t.trips.edit, systemImage: "pencil") { tripSheet = .edit(trip) }
            Button(s.duplicate, systemImage: "plus.square.on.square") { tripSheet = .duplicate(trip) }
            Button(t.trips.delete, systemImage: "trash", role: .destructive) { confirmDelete(trip) }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button {
                confirmDelete(trip)
            } label: {
                Label(t.trips.delete, systemImage: "trash")
            }
            .tint(AppColor.destructive)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(Format.shortDateWithWeekday(trip.date, settings.language)), \(trip.route), \(Format.number(trip.kilometers, settings.language)) km")
        .accessibilityAddTraits(.isButton)
        .accessibilityAction(named: s.duplicate) { tripSheet = .duplicate(trip) }
        .accessibilityAction(named: t.trips.delete) { confirmDelete(trip) }
    }

    // MARK: - Verktøylinje og knapp

    @ToolbarContentBuilder private var toolbar: some ToolbarContent {
        let t = settings.t
        let s = settings.s
        ToolbarItem(placement: .topBarLeading) {
            Button {
                Haptic.light.play()
                showSettings = true
            } label: {
                Image(systemName: "person.crop.circle")
            }
            .accessibilityLabel(s.settings)
        }
        if !isCurrentMonth {
            ToolbarItem(placement: .topBarTrailing) {
                Button(s.today) { goToToday() }
            }
        }
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                openExport()
            } label: {
                if exporting { ProgressView() } else { Image(systemName: "square.and.arrow.up") }
            }
            .disabled(exporting)
            .accessibilityLabel(t.summary.export)
        }
    }

    private var fab: some View {
        Button {
            Haptic.light.play()
            tripSheet = .new
        } label: {
            Image(systemName: "plus")
                .font(.system(size: 26, weight: .semibold))
                .foregroundStyle(AppColor.onGold)
                .frame(width: 60, height: 60)
                .background(Circle().fill(AppColor.gold))
                .shadow(color: .black.opacity(0.25), radius: 6, y: 3)
        }
        .buttonStyle(FabButtonStyle())
        .padding(.trailing, 16)
        .padding(.bottom, 8)
        .accessibilityLabel(settings.t.trips.addNew)
    }

    // MARK: - Handlinger

    private func navigateMonth(_ delta: Int) {
        current = Calendar.current.date(byAdding: .month, value: delta, to: current) ?? current
    }

    private func goToToday() {
        Haptic.light.play()
        withAnimation {
            current = Calendar.current.date(from: Calendar.current.dateComponents([.year, .month], from: Date())) ?? Date()
        }
    }

    private func refresh() async {
        do {
            try await session.refreshTrips()
        } catch {
            Haptic.error.play()
            alert = AlertInfo(title: settings.s.errorTitle, message: error.localizedDescription)
        }
    }

    private func confirmDelete(_ trip: Trip) {
        Haptic.warning.play()
        pendingDelete = trip
    }

    private func delete(_ trip: Trip) {
        Task {
            do {
                try await session.deleteTrip(trip.id)
                Haptic.success.play()
            } catch {
                Haptic.error.play()
                alert = AlertInfo(title: settings.t.trips.tripDeleteError, message: error.localizedDescription)
            }
        }
    }

    private func openExport() {
        if monthTrips.isEmpty {
            alert = AlertInfo(title: settings.t.export.title, message: settings.t.export.noTripsToExport)
            return
        }
        Haptic.light.play()
        showExportDialog = true
    }

    private func runExport(_ format: ReportFormat) {
        exporting = true
        let trips = monthTrips
        let name = monthName
        let year = year
        let profile = session.profile
        Task {
            defer { exporting = false }
            do {
                let url = switch format {
                case .pdf: try await Report.pdf(trips: trips, month: name, year: year, profile: profile)
                case .csv: try Report.csv(trips: trips, month: name, year: year)
                }
                Haptic.success.play()
                shareURL = ShareItem(url: url)
            } catch {
                Haptic.error.play()
                alert = AlertInfo(title: settings.s.errorTitle, message: error.localizedDescription)
            }
        }
    }
}

private struct FabButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.94 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
