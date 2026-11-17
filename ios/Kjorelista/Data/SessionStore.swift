import Foundation
import Observation

/// Port av kjoreliste-app/src/lib/session.tsx: innloggingsstatus, profil og turer.
@MainActor
@Observable
final class SessionStore {
    enum AuthState: Equatable {
        case initializing
        case signedOut
        case signedIn(AppUser)
    }

    let backend: any Backend
    let isDemo: Bool

    private(set) var auth: AuthState = .initializing
    private(set) var profile: UserProfile?
    private(set) var trips: [Trip] = []
    private(set) var loadingTrips = false
    /// Sant når turene er hentet minst én gang for innlogget bruker.
    private(set) var hasLoadedTrips = false
    /// Feil ved første henting (vises i stedet for tom liste).
    var loadError: String?

    @ObservationIgnored private var authTask: Task<Void, Never>?

    init(demo: Bool = Config.isDemo) {
        isDemo = demo
        backend = demo ? DemoBackend() : SupabaseBackend()
        let events = backend.authEvents()
        authTask = Task { [weak self] in
            for await event in events {
                self?.handle(event)
            }
        }
    }

    var user: AppUser? {
        if case let .signedIn(user) = auth { return user }
        return nil
    }

    private func handle(_ event: AuthEvent) {
        switch event {
        case let .signedIn(user):
            let changed = self.user?.id != user.id
            auth = .signedIn(user)
            if changed { didSignIn(user) }
        case .signedOut:
            auth = .signedOut
            profile = nil
            trips = []
            hasLoadedTrips = false
            loadError = nil
        }
    }

    private func didSignIn(_ user: AppUser) {
        profile = nil
        trips = []
        hasLoadedTrips = false
        Task {
            if let p = try? await backend.profile(userId: user.id) { profile = p }
        }
        Task {
            do {
                try await refreshTrips()
            } catch {
                loadError = error.localizedDescription
            }
        }
    }

    private func requireUser() throws -> AppUser {
        guard let user else { throw AppError(message: "Not signed in") }
        return user
    }

    func refreshTrips() async throws {
        guard let user else { return }
        loadingTrips = true
        defer { loadingTrips = false }
        let fetched = try await backend.trips(userId: user.id)
        trips = fetched
        hasLoadedTrips = true
        loadError = nil
    }

    @discardableResult
    func addTrip(_ trip: NewTrip) async throws -> Trip {
        let created = try await backend.addTrip(trip, userId: requireUser().id)
        trips.insert(created, at: 0)
        return created
    }

    @discardableResult
    func updateTrip(_ trip: Trip) async throws -> Trip {
        let updated = try await backend.updateTrip(trip, userId: requireUser().id)
        trips = trips.map { $0.id == updated.id ? updated : $0 }
        return updated
    }

    func deleteTrip(_ id: String) async throws {
        try await backend.deleteTrip(id: id, userId: requireUser().id)
        trips.removeAll { $0.id == id }
    }

    func signIn(email: String, password: String) async throws {
        try await backend.signIn(email: email, password: password)
    }

    func signUp(email: String, password: String, firstName: String, lastName: String) async throws -> SignUpResult {
        try await backend.signUp(email: email, password: password, firstName: firstName, lastName: lastName)
    }

    func resetPassword(email: String) async throws {
        try await backend.resetPassword(email: email)
    }

    func signOut() async throws {
        try await backend.signOut()
    }

    func deleteAccount() async throws {
        try await backend.deleteAccount()
        try? await backend.signOut()
    }
}
