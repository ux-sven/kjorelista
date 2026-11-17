import SwiftUI

@main
struct KjorelistaApp: App {
    @State private var settings = AppSettings()
    @State private var session = SessionStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(settings)
                .environment(session)
                .environment(\.locale, settings.locale)
                .preferredColorScheme(settings.theme.colorScheme)
                .tint(AppColor.primary)
        }
    }
}

struct RootView: View {
    @Environment(SessionStore.self) private var session

    var body: some View {
        Group {
            switch session.auth {
            case .initializing:
                AppColor.background.ignoresSafeArea()
            case .signedOut:
                LoginView()
                    .transition(.opacity)
            case .signedIn:
                HomeView()
                    .transition(.opacity)
            }
        }
        .animation(.default, value: session.auth)
    }
}
