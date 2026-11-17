import SwiftUI

/// Port av kjoreliste-app/src/app/login.tsx.
struct LoginView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(SessionStore.self) private var session

    enum Mode: Hashable { case login, register, forgot }
    enum FieldID: Hashable { case firstName, lastName, email, password, confirm }

    @State private var mode: Mode = .login
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var firstName = ""
    @State private var lastName = ""
    @State private var showPassword = false
    @State private var loading = false
    @State private var error = ""
    @State private var info = ""
    @State private var showPrivacy = false
    @FocusState private var focus: FieldID?

    var body: some View {
        let t = settings.t
        ScrollView {
            VStack(spacing: 24) {
                VStack(spacing: 6) {
                    Image("Logo")
                        .resizable()
                        .frame(width: 84, height: 84)
                        .clipShape(RoundedRectangle(cornerRadius: 19, style: .continuous))
                        .padding(.bottom, 8)
                        .accessibilityHidden(true)
                    Text(t.header.title)
                        .font(.largeTitle.bold())
                        .foregroundStyle(AppColor.foreground)
                        .accessibilityAddTraits(.isHeader)
                    Text(t.header.tagline)
                        .foregroundStyle(AppColor.mutedForeground)
                    if session.isDemo {
                        Text(settings.s.demoMode)
                            .font(.caption.weight(.semibold))
                            .padding(.horizontal, 8).padding(.vertical, 3)
                            .background(Capsule().fill(AppColor.warning.opacity(0.2)))
                            .foregroundStyle(AppColor.warning)
                    }
                }

                card
                    .padding(18)
                    .background(RoundedRectangle(cornerRadius: 14).fill(AppColor.card))
                    .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(AppColor.border, lineWidth: 0.5))

                Button(t.footer.privacyPolicy) { showPrivacy = true }
                    .font(.subheadline)
                    .foregroundStyle(AppColor.mutedForeground)
                    .accessibilityAddTraits(.isLink)
            }
            .padding(20)
            .frame(maxWidth: 520)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .defaultScrollAnchor(.center)
        .background(AppColor.background.ignoresSafeArea())
        .sheet(isPresented: $showPrivacy) {
            NavigationStack {
                PrivacyView()
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button(t.general.close) { showPrivacy = false }
                        }
                    }
            }
            .preferredColorScheme(settings.theme.colorScheme)
        }
    }

    @ViewBuilder private var card: some View {
        let t = settings.t
        VStack(alignment: .leading, spacing: 16) {
            if mode != .forgot {
                Picker("", selection: Binding(get: { mode }, set: { switchMode($0) })) {
                    Text(t.auth.signIn).tag(Mode.login)
                    Text(t.auth.signUp).tag(Mode.register)
                }
                .pickerStyle(.segmented)
                .controlSize(.large)
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    Text(t.auth.forgotPasswordTitle)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(AppColor.foreground)
                    Text(t.auth.forgotPasswordDescription)
                        .font(.subheadline)
                        .foregroundStyle(AppColor.mutedForeground)
                }
            }

            if !error.isEmpty { Banner(text: error, tint: AppColor.destructive) }
            if !info.isEmpty { Banner(text: info, tint: AppColor.success) }

            if mode == .register {
                HStack(spacing: 12) {
                    LabeledField(label: t.auth.firstName) {
                        TextField("", text: $firstName)
                            .textContentType(.givenName)
                            .focused($focus, equals: .firstName)
                            .submitLabel(.next)
                            .onSubmit { focus = .lastName }
                    }
                    LabeledField(label: t.auth.lastName) {
                        TextField("", text: $lastName)
                            .textContentType(.familyName)
                            .focused($focus, equals: .lastName)
                            .submitLabel(.next)
                            .onSubmit { focus = .email }
                    }
                }
            }

            LabeledField(label: t.auth.email, icon: "envelope") {
                TextField(settings.s.emailPlaceholder, text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(mode == .login ? .username : .emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .focused($focus, equals: .email)
                    .submitLabel(mode == .forgot ? .send : .next)
                    .onSubmit { mode == .forgot ? submit() : (focus = .password) }
            }

            if mode != .forgot {
                LabeledField(label: t.auth.password, icon: "lock") {
                    HStack {
                        Group {
                            if showPassword {
                                TextField("", text: $password)
                            } else {
                                SecureField("", text: $password)
                            }
                        }
                        .textContentType(mode == .login ? .password : .newPassword)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focus, equals: .password)
                        .submitLabel(mode == .login ? .go : .next)
                        .onSubmit { mode == .login ? submit() : (focus = .confirm) }

                        Button {
                            showPassword.toggle()
                        } label: {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundStyle(AppColor.mutedForeground)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(showPassword ? settings.s.hidePassword : settings.s.showPassword)
                    }
                }
            }

            if mode == .register {
                LabeledField(label: t.auth.confirmPassword) {
                    Group {
                        if showPassword {
                            TextField("", text: $confirmPassword)
                        } else {
                            SecureField("", text: $confirmPassword)
                        }
                    }
                    .textContentType(.newPassword)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .focused($focus, equals: .confirm)
                    .submitLabel(.go)
                    .onSubmit(submit)
                }
            }

            Button(action: submit) {
                ZStack {
                    Text(mode == .login ? t.auth.signIn : mode == .register ? t.auth.signUp : t.auth.sendResetEmail)
                        .font(.headline)
                        .opacity(loading ? 0 : 1)
                    if loading { ProgressView().tint(AppColor.primaryForeground) }
                }
                .frame(maxWidth: .infinity, minHeight: 50)
                .foregroundStyle(AppColor.primaryForeground)
                .background(RoundedRectangle(cornerRadius: 12).fill(AppColor.primary))
            }
            .buttonStyle(.plain)
            .disabled(loading)

            if mode == .login {
                ghostButton(t.auth.forgotPassword) { switchMode(.forgot) }
            } else if mode == .forgot {
                ghostButton(t.auth.backToLogin) { switchMode(.login) }
            }
        }
    }

    private func ghostButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.body.weight(.semibold))
                .frame(maxWidth: .infinity, minHeight: 44)
        }
        .buttonStyle(.plain)
        .foregroundStyle(AppColor.primary)
    }

    private func switchMode(_ next: Mode) {
        mode = next
        error = ""
        info = ""
    }

    private func fail(_ message: String) {
        Haptic.error.play()
        error = message
    }

    private func submit() {
        let t = settings.t
        let email = self.email.trimmingCharacters(in: .whitespaces)
        switch mode {
        case .login:
            if email.isEmpty { return fail(t.auth.emailRequired) }
            if password.isEmpty { return fail(t.auth.passwordRequired) }
        case .register:
            if firstName.trimmingCharacters(in: .whitespaces).isEmpty { return fail(t.auth.firstNameRequired) }
            if lastName.trimmingCharacters(in: .whitespaces).isEmpty { return fail(t.auth.lastNameRequired) }
            if email.isEmpty { return fail(t.auth.emailRequired) }
            if password.count < 6 { return fail(t.auth.passwordTooShort) }
            if password != confirmPassword { return fail(t.auth.passwordsDoNotMatch) }
        case .forgot:
            if email.isEmpty { return fail(t.auth.emailRequired) }
        }
        loading = true
        error = ""
        let currentMode = mode
        Task {
            defer { loading = false }
            do {
                switch currentMode {
                case .login:
                    try await session.signIn(email: email, password: password)
                    Haptic.success.play()
                case .register:
                    let result = try await session.signUp(
                        email: email, password: password,
                        firstName: firstName.trimmingCharacters(in: .whitespaces),
                        lastName: lastName.trimmingCharacters(in: .whitespaces)
                    )
                    Haptic.success.play()
                    if result == .confirmationRequired {
                        mode = .login
                        info = settings.s.checkEmail
                    }
                case .forgot:
                    try await session.resetPassword(email: email)
                    Haptic.success.play()
                    info = t.auth.resetEmailSent
                }
            } catch is InvalidCredentialsError {
                fail(t.auth.invalidCredentials)
            } catch {
                fail(error.localizedDescription)
            }
        }
    }
}

private struct LabeledField<Content: View>: View {
    let label: String
    var icon: String?
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(AppColor.foreground)
            HStack(spacing: 8) {
                if let icon {
                    Image(systemName: icon)
                        .foregroundStyle(AppColor.mutedForeground)
                        .accessibilityHidden(true)
                }
                content
                    .accessibilityLabel(label)
            }
            .padding(.horizontal, 12)
            .frame(minHeight: 50)
            .background(RoundedRectangle(cornerRadius: 12).fill(AppColor.inputBackground))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(AppColor.border, lineWidth: 1))
        }
    }
}

struct Banner: View {
    let text: String
    let tint: Color

    var body: some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(tint)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(RoundedRectangle(cornerRadius: 12).fill(tint.opacity(0.1)))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(tint, lineWidth: 1))
            .accessibilityAddTraits(.updatesFrequently)
    }
}
