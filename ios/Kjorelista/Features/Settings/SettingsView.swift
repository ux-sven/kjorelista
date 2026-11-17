import SwiftUI

/// Port av kjoreliste-app/src/app/settings.tsx: gruppert liste i samme stil som iOS Innstillinger.
struct SettingsView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(SessionStore.self) private var session
    @Environment(\.dismiss) private var dismiss

    @State private var confirmSignOut = false
    @State private var confirmDeleteAccount = false
    @State private var deleting = false
    @State private var alert: AlertInfo?

    var body: some View {
        @Bindable var settings = settings
        let t = settings.t
        let s = settings.s
        let profile = session.profile
        let name = profile.map { "\($0.firstName) \($0.lastName)".trimmingCharacters(in: .whitespaces) } ?? ""
        let initials = profile.map { "\($0.firstName.prefix(1))\($0.lastName.prefix(1))".uppercased() } ?? ""
        let email = session.user?.email ?? ""

        NavigationStack {
            List {
                Section(s.account) {
                    HStack(spacing: 14) {
                        ZStack {
                            Circle().fill(AppColor.primary)
                            if initials.isEmpty {
                                Image(systemName: "person.fill").font(.title2)
                            } else {
                                Text(initials).font(.title3.weight(.semibold))
                            }
                        }
                        .foregroundStyle(AppColor.primaryForeground)
                        .frame(width: 52, height: 52)
                        VStack(alignment: .leading, spacing: 2) {
                            if !name.isEmpty {
                                Text(name).font(.headline).foregroundStyle(AppColor.foreground)
                            }
                            Text(email).font(.subheadline).foregroundStyle(AppColor.mutedForeground)
                        }
                    }
                    .padding(.vertical, 6)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("\(name), \(email)")
                }
                .listRowBackground(AppColor.card)

                Section {
                    Toggle(s.routeSuggestions, isOn: $settings.routeSuggestions)
                    // Automatisk km er bare relevant når forslag er slått på
                    if settings.routeSuggestions {
                        Toggle(s.autofillKilometers, isOn: $settings.autofillKilometers)
                    }
                } header: {
                    Text(s.tripEntry)
                } footer: {
                    Text(settings.routeSuggestions ? s.autofillKilometersFooter : s.routeSuggestionsFooter)
                }
                .listRowBackground(AppColor.card)
                .onChange(of: settings.routeSuggestions) { Haptic.light.play() }
                .onChange(of: settings.autofillKilometers) { Haptic.light.play() }

                Section(t.general.language) {
                    selectRow("Norsk", selected: settings.language == .nb) { settings.language = .nb }
                    selectRow("English", selected: settings.language == .en) { settings.language = .en }
                }
                .listRowBackground(AppColor.card)

                Section(s.appearance) {
                    selectRow(s.themeSystem, selected: settings.theme == .system) { settings.theme = .system }
                    selectRow(s.themeLight, selected: settings.theme == .light) { settings.theme = .light }
                    selectRow(s.themeDark, selected: settings.theme == .dark) { settings.theme = .dark }
                }
                .listRowBackground(AppColor.card)

                Section {
                    NavigationLink {
                        PrivacyView()
                    } label: {
                        Label {
                            Text(t.footer.privacyPolicy).foregroundStyle(AppColor.foreground)
                        } icon: {
                            Image(systemName: "checkmark.shield").foregroundStyle(AppColor.primary)
                        }
                    }
                }
                .listRowBackground(AppColor.card)

                Section {
                    Button(t.auth.signOut) { confirmSignOut = true }
                        .frame(maxWidth: .infinity)
                }
                .listRowBackground(AppColor.card)

                Section {
                    if deleting {
                        ProgressView().tint(AppColor.destructive).frame(maxWidth: .infinity)
                    } else {
                        Button(s.deleteAccount, role: .destructive) {
                            Haptic.warning.play()
                            confirmDeleteAccount = true
                        }
                        .foregroundStyle(AppColor.destructive)
                        .frame(maxWidth: .infinity)
                    }
                } footer: {
                    VStack(spacing: 24) {
                        Text(s.deleteAccountFooter)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text("\(t.header.title) · \(s.version) \(Config.appVersion)\(session.isDemo ? " · \(s.demoMode)" : "")")
                            .frame(maxWidth: .infinity)
                    }
                }
                .listRowBackground(AppColor.card)
            }
            .themedList()
            .navigationTitle(s.settings)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(t.general.finish) { dismiss() }
                        .fontWeight(.semibold)
                }
            }
            .alert(t.auth.signOut, isPresented: $confirmSignOut) {
                Button(t.trips.cancel, role: .cancel) {}
                Button(t.auth.signOut, role: .destructive, action: signOut)
            } message: {
                Text(t.auth.signOutConfirmation)
            }
            .alert(s.deleteAccount, isPresented: $confirmDeleteAccount) {
                Button(t.trips.cancel, role: .cancel) {}
                Button(s.deleteAccount, role: .destructive, action: deleteAccount)
            } message: {
                Text(s.deleteAccountMessage)
            }
            .alert(item: $alert) { info in
                Alert(title: Text(info.title), message: Text(info.message))
            }
        }
    }

    private func selectRow(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button {
            Haptic.light.play()
            action()
        } label: {
            HStack {
                Text(label).foregroundStyle(AppColor.foreground)
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(AppColor.primary)
                }
            }
            .contentShape(Rectangle())
        }
        .accessibilityAddTraits(selected ? [.isSelected] : [])
    }

    private func signOut() {
        Task {
            do {
                try await session.signOut()
                Haptic.success.play()
            } catch {
                Haptic.error.play()
                alert = AlertInfo(title: settings.s.errorTitle, message: error.localizedDescription)
            }
        }
    }

    /// Apple krever at kontoer som opprettes i appen også kan slettes i appen.
    /// Selve slettingen gjøres av Edge-funksjonen «delete-account».
    private func deleteAccount() {
        deleting = true
        Task {
            defer { deleting = false }
            do {
                try await session.deleteAccount()
                Haptic.success.play()
            } catch {
                Haptic.error.play()
                alert = AlertInfo(title: settings.s.deleteAccountError, message: error.localizedDescription)
            }
        }
    }
}
