package no.svenway.kjorelista.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Port av SessionProvider (src/lib/session.tsx): innlogget bruker, profil og turer. */
class AppStore(val backend: Backend, private val scope: CoroutineScope) {
    val authState: StateFlow<AuthState> = backend.authState

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    private val _loadingTrips = MutableStateFlow(false)
    val loadingTrips: StateFlow<Boolean> = _loadingTrips

    /** Turer er hentet minst én gang (lasteindikator i stedet for tom-tilstand før det). */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    /** Feil ved automatisk henting som skal vises for brukeren. */
    private val _errors = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = _errors.receiveAsFlow()

    private val userId: String?
        get() = (authState.value as? AuthState.SignedIn)?.user?.id

    init {
        scope.launch {
            authState.map { (it as? AuthState.SignedIn)?.user?.id }.distinctUntilChanged().collect { id ->
                if (id == null) {
                    _profile.value = null
                    _trips.value = emptyList()
                    _loaded.value = false
                    return@collect
                }
                launch { _profile.value = runCatching { backend.profile(id) }.getOrNull() }
                try {
                    refreshTrips()
                } catch (e: BackendException) {
                    _errors.trySend(e.message ?: "")
                }
            }
        }
    }

    private fun requireUser(): String = userId ?: throw BackendException("Not signed in")

    suspend fun refreshTrips() {
        val id = userId ?: return
        _loadingTrips.value = true
        try {
            _trips.value = backend.getTrips(id)
            _loaded.value = true
        } finally {
            _loadingTrips.value = false
        }
    }

    suspend fun addTrip(trip: NewTrip): Trip {
        val created = backend.addTrip(trip, requireUser())
        _trips.update { listOf(created) + it }
        return created
    }

    suspend fun updateTrip(trip: Trip): Trip {
        val updated = backend.updateTrip(trip, requireUser())
        _trips.update { list -> list.map { if (it.id == updated.id) updated else it } }
        return updated
    }

    suspend fun deleteTrip(tripId: String) {
        backend.deleteTrip(tripId, requireUser())
        _trips.update { list -> list.filterNot { it.id == tripId } }
    }

    suspend fun signOut() = backend.signOut()

    suspend fun deleteAccount() {
        backend.deleteAccount()
        backend.signOut()
    }
}
