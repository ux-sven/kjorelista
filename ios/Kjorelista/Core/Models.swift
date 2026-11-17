import Foundation

/// Samme form som Trip i webløsningen og Expo-appen (src/lib/types.ts).
struct Trip: Identifiable, Hashable, Sendable {
    var id: String
    /// `YYYY-MM-DD`, tolkes alltid som lokal dato.
    var date: String
    var kilometers: Double
    var route: String
    var hours: Int
    var minutes: Int
    var hasPassengers: Bool
    var passengerCount: Int
    var passengerKilometers: Double
    var otherExpenses: Double
    var expenseDescription: String
    var comment: String
}

/// En tur som ikke er lagret ennå (uten id).
struct NewTrip: Hashable, Sendable {
    var date: String
    var kilometers: Double
    var route: String
    var hours: Int
    var minutes: Int
    var hasPassengers: Bool
    var passengerCount: Int
    var passengerKilometers: Double
    var otherExpenses: Double
    var expenseDescription: String
    var comment: String

    func withId(_ id: String) -> Trip {
        Trip(
            id: id, date: date, kilometers: kilometers, route: route, hours: hours, minutes: minutes,
            hasPassengers: hasPassengers, passengerCount: passengerCount, passengerKilometers: passengerKilometers,
            otherExpenses: otherExpenses, expenseDescription: expenseDescription, comment: comment
        )
    }
}

struct UserProfile: Hashable, Sendable {
    var firstName: String
    var lastName: String
}

struct AppUser: Hashable, Sendable {
    var id: String
    var email: String
}
