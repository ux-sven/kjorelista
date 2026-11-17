package no.svenway.kjorelista.data

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/** Delt Supabase-prosjekt med webløsningen. Anon-nøkkelen er offentlig; tilgang styres av RLS. */
object SupabaseConfig {
    const val URL = "https://cwuvgbmqdasqdzlxcpab.supabase.co"
    const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN3dXZnYm1xZGFzcWR6bHhjcGFiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTE0Mzc3NTgsImV4cCI6MjA2NzAxMzc1OH0.EwNlo3KSlsSTONFqk6NaBDkZ7WMqBOijZovxDd6tWGo"
    const val CONTACT_EMAIL = "svenway+kjorelista@gmail.com"
}

// Tabellen «trips» deles med webløsningen. Kolonnenavn må ikke endres.
@Serializable
private data class DbTrip(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    // numeric-kolonner kan komme som tall eller tekst
    val kilometers: JsonPrimitive,
    val route: String,
    val hours: Int,
    val minutes: Int,
    @SerialName("has_passengers") val hasPassengers: Boolean = false,
    @SerialName("passenger_count") val passengerCount: Int = 0,
    @SerialName("passenger_kilometers") val passengerKilometers: JsonPrimitive? = null,
    @SerialName("other_expenses") val otherExpenses: JsonPrimitive? = null,
    @SerialName("expense_description") val expenseDescription: String? = null,
    val comment: String? = null,
) {
    fun toTrip() = Trip(
        id = id,
        date = date,
        kilometers = kilometers.number(),
        route = route,
        hours = hours,
        minutes = minutes,
        hasPassengers = hasPassengers,
        passengerCount = passengerCount,
        passengerKilometers = passengerKilometers.number(),
        otherExpenses = otherExpenses.number(),
        expenseDescription = expenseDescription ?: "",
        comment = comment ?: "",
    )
}

private fun JsonPrimitive?.number(): Double = this?.content?.toDoubleOrNull() ?: 0.0

@Serializable
private data class DbTripWrite(
    @SerialName("user_id") val userId: String,
    val date: String,
    val kilometers: Double,
    val route: String,
    val hours: Int,
    val minutes: Int,
    @SerialName("has_passengers") val hasPassengers: Boolean,
    @SerialName("passenger_count") val passengerCount: Int,
    @SerialName("passenger_kilometers") val passengerKilometers: Double,
    @SerialName("other_expenses") val otherExpenses: Double,
    @SerialName("expense_description") val expenseDescription: String,
    val comment: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun NewTrip.toDb(userId: String, updatedAt: String? = null) = DbTripWrite(
    userId = userId, date = date, kilometers = kilometers, route = route, hours = hours, minutes = minutes,
    hasPassengers = hasPassengers, passengerCount = passengerCount, passengerKilometers = passengerKilometers,
    otherExpenses = otherExpenses, expenseDescription = expenseDescription, comment = comment,
    updatedAt = updatedAt,
)

private fun Trip.toNew() = NewTrip(
    date, kilometers, route, hours, minutes, hasPassengers, passengerCount, passengerKilometers,
    otherExpenses, expenseDescription, comment,
)

@Serializable
private data class DbProfile(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
)

/**
 * Ekte backend. I motsetning til webløsningen faller vi aldri tilbake til lokal lagring ved feil:
 * alle feil kastes som [BackendException] og vises for brukeren.
 */
class SupabaseBackend(context: Context, scope: CoroutineScope) : Backend {
    override val isDemo = false

    private val client: SupabaseClient = createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.ANON_KEY) {
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false })
        install(Auth) {
            sessionManager = SecureSessionManager(context.applicationContext)
            autoLoadFromStorage = true
            autoSaveToStorage = true
            alwaysAutoRefresh = true
        }
        install(Postgrest)
        install(Functions)
    }

    override val authState: StateFlow<AuthState> = client.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> status.session.user
                    ?.let { AuthState.SignedIn(AuthUser(it.id, it.email ?: "")) }
                    ?: AuthState.SignedOut
                is SessionStatus.NotAuthenticated -> AuthState.SignedOut
                is SessionStatus.Initializing -> AuthState.Initializing
                // Fornyelse feilet (f.eks. uten nett): behold brukeren, nytt forsøk skjer automatisk
                is SessionStatus.RefreshFailure -> client.auth.currentUserOrNull()
                    ?.let { AuthState.SignedIn(AuthUser(it.id, it.email ?: "")) }
                    ?: AuthState.SignedOut
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, AuthState.Initializing)

    private suspend fun <T> call(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthRestException) {
            val invalid = e.errorCode == AuthErrorCode.InvalidCredentials || e.error == "invalid_credentials" ||
                e.errorDescription == "Invalid login credentials"
            throw BackendException(e.errorDescription, invalidCredentials = invalid, cause = e)
        } catch (e: RestException) {
            throw BackendException(e.description ?: e.error, cause = e)
        } catch (e: HttpRequestException) {
            throw BackendException(e.message?.lineSequence()?.firstOrNull() ?: e.toString(), cause = e)
        } catch (e: Exception) {
            throw BackendException(e.message?.lineSequence()?.firstOrNull() ?: e.toString(), cause = e)
        }

    override suspend fun signIn(email: String, password: String) = call {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String, firstName: String, lastName: String): Boolean = call {
        // Databasetriggeren oppretter profilen fra disse metadataene
        val user = client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
            }
        }
        user != null && client.auth.currentSessionOrNull() == null
    }

    override suspend fun resetPassword(email: String) = call {
        // Uten redirect: lenken i e-posten åpner webløsningen (Supabase «Site URL»)
        client.auth.resetPasswordForEmail(email)
    }

    override suspend fun signOut() = call { client.auth.signOut() }

    override suspend fun deleteAccount() = call {
        // Edge-funksjonen «delete-account» sletter brukeren og alle turer. Brukerens JWT sendes automatisk.
        client.functions.invoke("delete-account")
        Unit
    }

    override suspend fun profile(userId: String): UserProfile? = call {
        client.from("profiles")
            .select(Columns.list("first_name", "last_name")) { filter { eq("id", userId) } }
            .decodeSingleOrNull<DbProfile>()
            ?.let { UserProfile(it.firstName ?: "", it.lastName ?: "") }
    }

    override suspend fun getTrips(userId: String): List<Trip> = call {
        client.from("trips")
            .select {
                filter { eq("user_id", userId) }
                order("date", Order.DESCENDING)
            }
            .decodeList<DbTrip>()
            .map { it.toTrip() }
    }

    override suspend fun addTrip(trip: NewTrip, userId: String): Trip = call {
        client.from("trips")
            .insert(trip.toDb(userId)) { select() }
            .decodeSingle<DbTrip>()
            .toTrip()
    }

    override suspend fun updateTrip(trip: Trip, userId: String): Trip = call {
        client.from("trips")
            .update(trip.toNew().toDb(userId, updatedAt = Instant.now().toString())) {
                select()
                filter {
                    eq("id", trip.id)
                    eq("user_id", userId)
                }
            }
            .decodeSingle<DbTrip>()
            .toTrip()
    }

    override suspend fun deleteTrip(tripId: String, userId: String) = call {
        client.from("trips").delete {
            filter {
                eq("id", tripId)
                eq("user_id", userId)
            }
        }
        Unit
    }
}
