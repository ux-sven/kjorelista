package no.svenway.kjorelista.data

// Samme form som Trip i webløsningen og Expo-appen (src/lib/types.ts)
data class Trip(
    val id: String,
    val date: String, // YYYY-MM-DD, tolkes som lokal dato
    val kilometers: Double,
    val route: String,
    val hours: Int,
    val minutes: Int,
    val hasPassengers: Boolean = false,
    val passengerCount: Int = 0,
    val passengerKilometers: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val expenseDescription: String = "",
    val comment: String = "",
)

data class NewTrip(
    val date: String,
    val kilometers: Double,
    val route: String,
    val hours: Int,
    val minutes: Int,
    val hasPassengers: Boolean,
    val passengerCount: Int,
    val passengerKilometers: Double,
    val otherExpenses: Double,
    val expenseDescription: String,
    val comment: String,
) {
    fun withId(id: String) = Trip(
        id = id, date = date, kilometers = kilometers, route = route, hours = hours, minutes = minutes,
        hasPassengers = hasPassengers, passengerCount = passengerCount, passengerKilometers = passengerKilometers,
        otherExpenses = otherExpenses, expenseDescription = expenseDescription, comment = comment,
    )
}

data class UserProfile(val firstName: String, val lastName: String)

data class AuthUser(val id: String, val email: String)

sealed interface AuthState {
    data object Initializing : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

/** Feil som vises for brukeren. [invalidCredentials] gir den oversatte «Ugyldig e-post eller passord». */
class BackendException(message: String, val invalidCredentials: Boolean = false, cause: Throwable? = null) :
    Exception(message, cause)
