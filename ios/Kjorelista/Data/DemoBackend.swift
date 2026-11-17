import Foundation

/// Demomodus med samme eksempeldata som kjoreliste-app/src/lib/demo.ts.
/// Ingen nettverkskall: innlogging, registrering og «glemt passord» er bare simulert.
actor DemoBackend: Backend {
    static let user = AppUser(id: "demo-user", email: "kari@example.com")
    static let profile = UserProfile(firstName: "Kari", lastName: "Nordmann")

    private var store: [Trip]
    private var nextId = 100
    private var continuations: [UUID: AsyncStream<AuthEvent>.Continuation] = [:]
    private var signedIn = true

    init() {
        func daysAgo(_ n: Int) -> String {
            Format.toDateString(Calendar.current.date(byAdding: .day, value: -n, to: Date()) ?? Date())
        }
        func trip(
            _ id: String, _ days: Int, _ km: Double, _ route: String, _ h: Int, _ m: Int,
            passengers: Int = 0, passengerKm: Double = 0, expenses: Double = 0, expenseDescription: String = "", comment: String = ""
        ) -> Trip {
            Trip(
                id: id, date: daysAgo(days), kilometers: km, route: route, hours: h, minutes: m,
                hasPassengers: passengers > 0, passengerCount: passengers, passengerKilometers: passengerKm,
                otherExpenses: expenses, expenseDescription: expenseDescription, comment: comment
            )
        }
        store = [
            trip("d1", 0, 42.5, "Oslo – Drammen", 0, 45),
            trip("d2", 1, 128, "Oslo – Hamar", 1, 35, passengers: 2, passengerKm: 128),
            trip("d3", 3, 18.2, "Kontoret – kundemøte Lysaker", 0, 25, expenses: 64, expenseDescription: "Bompenger"),
            trip("d4", 6, 87, "Oslo – Moss", 1, 10, comment: "Befaring hos kunde"),
            trip("d5", 35, 240, "Oslo – Lillehammer t/r", 3, 20),
        ]
    }

    nonisolated func authEvents() -> AsyncStream<AuthEvent> {
        AsyncStream { continuation in
            let id = UUID()
            Task { await self.register(id, continuation) }
            continuation.onTermination = { _ in Task { await self.unregister(id) } }
        }
    }

    private func register(_ id: UUID, _ continuation: AsyncStream<AuthEvent>.Continuation) {
        continuations[id] = continuation
        continuation.yield(signedIn ? .signedIn(Self.user) : .signedOut)
    }

    private func unregister(_ id: UUID) { continuations[id] = nil }

    private func emit(_ event: AuthEvent) {
        for c in continuations.values { c.yield(event) }
    }

    func signIn(email: String, password: String) async throws {
        signedIn = true
        emit(.signedIn(Self.user))
    }

    func signUp(email: String, password: String, firstName: String, lastName: String) async throws -> SignUpResult {
        .confirmationRequired
    }

    func resetPassword(email: String) async throws {}

    func signOut() async throws {
        signedIn = false
        emit(.signedOut)
    }

    func deleteAccount() async throws {
        throw AppError(message: "Ikke tilgjengelig i demomodus / Not available in demo mode")
    }

    func profile(userId: String) async throws -> UserProfile? { Self.profile }

    func trips(userId: String) async throws -> [Trip] {
        store.sorted { $0.date > $1.date }
    }

    func addTrip(_ trip: NewTrip, userId: String) async throws -> Trip {
        let created = trip.withId("d\(nextId)")
        nextId += 1
        store.insert(created, at: 0)
        return created
    }

    func updateTrip(_ trip: Trip, userId: String) async throws -> Trip {
        store = store.map { $0.id == trip.id ? trip : $0 }
        return trip
    }

    func deleteTrip(id: String, userId: String) async throws {
        store.removeAll { $0.id == id }
    }
}
