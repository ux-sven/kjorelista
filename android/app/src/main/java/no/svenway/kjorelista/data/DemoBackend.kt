package no.svenway.kjorelista.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Demomodus med eksempeldata, uten Supabase og uten nettverk (port av src/lib/demo.ts).
 * Innlogging, registrering og sletting av konto er bare simulert.
 */
class DemoBackend(manyTrips: Boolean = false) : Backend {
    override val isDemo = true

    private val demoUser = AuthUser(id = "demo-user", email = "kari@example.com")
    private val state = MutableStateFlow<AuthState>(AuthState.SignedIn(demoUser))
    override val authState: StateFlow<AuthState> = state

    private fun daysAgo(n: Long) = LocalDate.now().minusDays(n).toString()

    private var store: List<Trip> = listOf(
        Trip(id = "d1", date = daysAgo(0), kilometers = 42.5, route = "Oslo – Drammen", hours = 0, minutes = 45),
        Trip(
            id = "d2", date = daysAgo(1), kilometers = 128.0, route = "Oslo – Hamar", hours = 1, minutes = 35,
            hasPassengers = true, passengerCount = 2, passengerKilometers = 128.0,
        ),
        Trip(
            id = "d3", date = daysAgo(3), kilometers = 18.2, route = "Kontoret – kundemøte Lysaker", hours = 0, minutes = 25,
            otherExpenses = 64.0, expenseDescription = "Bompenger",
        ),
        Trip(id = "d4", date = daysAgo(6), kilometers = 87.0, route = "Oslo – Moss", hours = 1, minutes = 10, comment = "Befaring hos kunde"),
        Trip(id = "d5", date = daysAgo(35), kilometers = 240.0, route = "Oslo – Lillehammer t/r", hours = 3, minutes = 20),
    )
    private var nextId = 100

    init {
        if (manyTrips) {
            val first = LocalDate.now().minusMonths(1).withDayOfMonth(1)
            val routes = listOf("Oslo – Drammen", "Oslo – Hamar", "Kontoret – kundemøte Lysaker", "Oslo – Moss", "Oslo – Fredrikstad")
            store = store + List(30) { i ->
                Trip(
                    id = "m$i",
                    date = first.plusDays((i % 28).toLong()).toString(),
                    kilometers = 20.0 + i * 3.5,
                    route = routes[i % routes.size],
                    hours = i % 3,
                    minutes = (i * 7) % 60,
                    comment = if (i % 4 == 0) "Befaring og oppfølging hos kunde nr. ${i + 1}" else "",
                )
            }
        }
    }

    override suspend fun signIn(email: String, password: String) {
        delay(300)
        state.value = AuthState.SignedIn(demoUser)
    }

    override suspend fun signUp(email: String, password: String, firstName: String, lastName: String): Boolean {
        delay(300)
        state.value = AuthState.SignedIn(demoUser)
        return false
    }

    override suspend fun resetPassword(email: String) = delay(300)

    override suspend fun signOut() {
        state.value = AuthState.SignedOut
    }

    override suspend fun deleteAccount() = delay(300)

    override suspend fun profile(userId: String) = UserProfile("Kari", "Nordmann")

    override suspend fun getTrips(userId: String): List<Trip> {
        delay(250)
        return store.sortedByDescending { it.date }
    }

    override suspend fun addTrip(trip: NewTrip, userId: String): Trip {
        val created = trip.withId("d${nextId++}")
        store = listOf(created) + store
        return created
    }

    override suspend fun updateTrip(trip: Trip, userId: String): Trip {
        store = store.map { if (it.id == trip.id) trip else it }
        return trip
    }

    override suspend fun deleteTrip(tripId: String, userId: String) {
        store = store.filterNot { it.id == tripId }
    }
}
