package no.svenway.kjorelista.data

import kotlinx.coroutines.flow.StateFlow

/** Felles grensesnitt for Supabase (ekte data) og demomodus (alt i minnet). */
interface Backend {
    val isDemo: Boolean
    val authState: StateFlow<AuthState>

    suspend fun signIn(email: String, password: String)

    /** Returnerer true hvis brukeren må bekrefte e-posten før innlogging (bruker uten sesjon). */
    suspend fun signUp(email: String, password: String, firstName: String, lastName: String): Boolean
    suspend fun resetPassword(email: String)
    suspend fun signOut()
    suspend fun deleteAccount()

    suspend fun profile(userId: String): UserProfile?
    suspend fun getTrips(userId: String): List<Trip>
    suspend fun addTrip(trip: NewTrip, userId: String): Trip
    suspend fun updateTrip(trip: Trip, userId: String): Trip
    suspend fun deleteTrip(tripId: String, userId: String)
}
