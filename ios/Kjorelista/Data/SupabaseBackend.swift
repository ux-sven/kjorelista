import Foundation
import Supabase

/// Ekte backend: samme Supabase-prosjekt og tabeller som webløsningen.
/// Sesjonen lagres i Keychain og fornyes automatisk av supabase-swift.
/// Feil kastes videre slik at brukeren får beskjed – vi faller aldri tilbake til lokal lagring.
final class SupabaseBackend: Backend {
    private let client: SupabaseClient

    init() {
        client = SupabaseClient(
            supabaseURL: Config.supabaseURL,
            supabaseKey: Config.supabaseAnonKey,
            options: SupabaseClientOptions(
                auth: .init(
                    storage: KeychainLocalStorage(),
                    autoRefreshToken: true,
                    emitLocalSessionAsInitialSession: true
                )
            )
        )
    }

    // MARK: - Auth

    func authEvents() -> AsyncStream<AuthEvent> {
        let changes = client.auth.authStateChanges
        return AsyncStream { continuation in
            let task = Task {
                for await (event, session) in changes {
                    switch event {
                    case .initialSession, .signedIn, .tokenRefreshed, .userUpdated:
                        if let session {
                            continuation.yield(.signedIn(AppUser(id: session.user.id.uuidString.lowercased(), email: session.user.email ?? "")))
                        } else {
                            continuation.yield(.signedOut)
                        }
                    case .signedOut, .userDeleted:
                        continuation.yield(.signedOut)
                    default:
                        break
                    }
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    func signIn(email: String, password: String) async throws {
        do {
            try await client.auth.signIn(email: email, password: password)
        } catch let error as AuthError where error.errorCode == .invalidCredentials || error.message == "Invalid login credentials" {
            throw InvalidCredentialsError()
        }
    }

    func signUp(email: String, password: String, firstName: String, lastName: String) async throws -> SignUpResult {
        // Databasetriggeren oppretter profilen fra disse metadataene
        let response = try await client.auth.signUp(
            email: email,
            password: password,
            data: ["first_name": .string(firstName), "last_name": .string(lastName)]
        )
        return response.session == nil ? .confirmationRequired : .signedIn
    }

    func resetPassword(email: String) async throws {
        // Uten redirect: lenken åpner Supabase sin «Site URL» (webløsningen)
        try await client.auth.resetPasswordForEmail(email)
    }

    func signOut() async throws {
        try await client.auth.signOut()
    }

    /// Edge-funksjonen «delete-account» sletter brukeren og alle turer. Kalles med brukerens JWT.
    func deleteAccount() async throws {
        try await client.functions.invoke("delete-account", options: FunctionInvokeOptions(method: .post))
    }

    // MARK: - Data

    func profile(userId: String) async throws -> UserProfile? {
        let rows: [ProfileRow] = try await client
            .from("profiles")
            .select("first_name, last_name")
            .eq("id", value: userId)
            .limit(1)
            .execute()
            .value
        return rows.first.map { UserProfile(firstName: $0.first_name ?? "", lastName: $0.last_name ?? "") }
    }

    func trips(userId: String) async throws -> [Trip] {
        let rows: [TripRow] = try await client
            .from("trips")
            .select()
            .eq("user_id", value: userId)
            .order("date", ascending: false)
            .execute()
            .value
        return rows.map(\.trip)
    }

    func addTrip(_ trip: NewTrip, userId: String) async throws -> Trip {
        let row: TripRow = try await client
            .from("trips")
            .insert(TripWrite(trip, userId: userId), returning: .representation)
            .select()
            .single()
            .execute()
            .value
        return row.trip
    }

    func updateTrip(_ trip: Trip, userId: String) async throws -> Trip {
        var write = TripWrite(NewTrip(trip), userId: userId)
        write.updated_at = ISO8601DateFormatter().string(from: Date())
        let row: TripRow = try await client
            .from("trips")
            .update(write, returning: .representation)
            .eq("id", value: trip.id)
            .eq("user_id", value: userId)
            .select()
            .single()
            .execute()
            .value
        return row.trip
    }

    func deleteTrip(id: String, userId: String) async throws {
        try await client
            .from("trips")
            .delete()
            .eq("id", value: id)
            .eq("user_id", value: userId)
            .execute()
    }
}

// MARK: - Databaserader (tabellen «trips» deles med webløsningen – kolonnenavn må ikke endres)

private struct ProfileRow: Decodable {
    let first_name: String?
    let last_name: String?
}

private struct TripRow: Decodable {
    let id: String
    let date: String
    let kilometers: Double
    let route: String
    let hours: Int
    let minutes: Int
    let has_passengers: Bool
    let passenger_count: Int
    let passenger_kilometers: Double
    let other_expenses: Double
    let expense_description: String?
    let comment: String?

    enum CodingKeys: String, CodingKey {
        case id, date, kilometers, route, hours, minutes, has_passengers, passenger_count,
             passenger_kilometers, other_expenses, expense_description, comment
    }

    init(from decoder: any Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        // id kan være uuid (streng) eller tall
        if let s = try? c.decode(String.self, forKey: .id) { id = s } else { id = String(try c.decode(Int.self, forKey: .id)) }
        date = try c.decode(String.self, forKey: .date)
        kilometers = try c.flexibleDouble(.kilometers)
        route = try c.decodeIfPresent(String.self, forKey: .route) ?? ""
        hours = Int(try c.flexibleDouble(.hours))
        minutes = Int(try c.flexibleDouble(.minutes))
        has_passengers = try c.decodeIfPresent(Bool.self, forKey: .has_passengers) ?? false
        passenger_count = Int(try c.flexibleDouble(.passenger_count))
        passenger_kilometers = try c.flexibleDouble(.passenger_kilometers)
        other_expenses = try c.flexibleDouble(.other_expenses)
        expense_description = try c.decodeIfPresent(String.self, forKey: .expense_description)
        comment = try c.decodeIfPresent(String.self, forKey: .comment)
    }

    var trip: Trip {
        Trip(
            id: id, date: String(date.prefix(10)), kilometers: kilometers, route: route, hours: hours, minutes: minutes,
            hasPassengers: has_passengers, passengerCount: passenger_count, passengerKilometers: passenger_kilometers,
            otherExpenses: other_expenses, expenseDescription: expense_description ?? "", comment: comment ?? ""
        )
    }
}

private extension KeyedDecodingContainer where K == TripRow.CodingKeys {
    /// Postgres `numeric` kan komme som tall eller streng.
    func flexibleDouble(_ key: K) throws -> Double {
        if let d = try? decode(Double.self, forKey: key) { return d }
        if let s = try? decode(String.self, forKey: key), let d = Double(s) { return d }
        return 0
    }
}

private struct TripWrite: Encodable {
    let user_id: String
    let date: String
    let kilometers: Double
    let route: String
    let hours: Int
    let minutes: Int
    let has_passengers: Bool
    let passenger_count: Int
    let passenger_kilometers: Double
    let other_expenses: Double
    let expense_description: String
    let comment: String
    var updated_at: String?

    init(_ trip: NewTrip, userId: String) {
        user_id = userId
        date = trip.date
        kilometers = trip.kilometers
        route = trip.route
        hours = trip.hours
        minutes = trip.minutes
        has_passengers = trip.hasPassengers
        passenger_count = trip.passengerCount
        passenger_kilometers = trip.passengerKilometers
        other_expenses = trip.otherExpenses
        expense_description = trip.expenseDescription
        comment = trip.comment
    }

    func encode(to encoder: any Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(user_id, forKey: .user_id)
        try c.encode(date, forKey: .date)
        try c.encode(kilometers, forKey: .kilometers)
        try c.encode(route, forKey: .route)
        try c.encode(hours, forKey: .hours)
        try c.encode(minutes, forKey: .minutes)
        try c.encode(has_passengers, forKey: .has_passengers)
        try c.encode(passenger_count, forKey: .passenger_count)
        try c.encode(passenger_kilometers, forKey: .passenger_kilometers)
        try c.encode(other_expenses, forKey: .other_expenses)
        try c.encode(expense_description, forKey: .expense_description)
        try c.encode(comment, forKey: .comment)
        try c.encodeIfPresent(updated_at, forKey: .updated_at)
    }

    enum CodingKeys: String, CodingKey {
        case user_id, date, kilometers, route, hours, minutes, has_passengers, passenger_count,
             passenger_kilometers, other_expenses, expense_description, comment, updated_at
    }
}

extension NewTrip {
    init(_ trip: Trip) {
        self.init(
            date: trip.date, kilometers: trip.kilometers, route: trip.route, hours: trip.hours, minutes: trip.minutes,
            hasPassengers: trip.hasPassengers, passengerCount: trip.passengerCount, passengerKilometers: trip.passengerKilometers,
            otherExpenses: trip.otherExpenses, expenseDescription: trip.expenseDescription, comment: trip.comment
        )
    }
}
