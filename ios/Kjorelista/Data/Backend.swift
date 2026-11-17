import Foundation

enum AuthEvent: Sendable {
    case signedIn(AppUser)
    case signedOut
}

enum SignUpResult: Sendable {
    /// Kontoen er opprettet og brukeren er logget inn.
    case signedIn
    /// Kontoen må bekreftes via e-post før innlogging.
    case confirmationRequired
}

/// Alt appen trenger fra serveren. `SupabaseBackend` snakker med produksjonsdatabasen,
/// `DemoBackend` holder eksempeldata i minnet.
protocol Backend: Sendable {
    /// Strøm av innloggingsstatus. Første verdi er gjeldende status (fra Keychain).
    func authEvents() -> AsyncStream<AuthEvent>

    func signIn(email: String, password: String) async throws
    func signUp(email: String, password: String, firstName: String, lastName: String) async throws -> SignUpResult
    func resetPassword(email: String) async throws
    func signOut() async throws
    func deleteAccount() async throws

    func profile(userId: String) async throws -> UserProfile?
    func trips(userId: String) async throws -> [Trip]
    func addTrip(_ trip: NewTrip, userId: String) async throws -> Trip
    func updateTrip(_ trip: Trip, userId: String) async throws -> Trip
    func deleteTrip(id: String, userId: String) async throws
}

struct AppError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

/// Feil e-post eller passord (Supabase: «Invalid login credentials»).
struct InvalidCredentialsError: Error {}
