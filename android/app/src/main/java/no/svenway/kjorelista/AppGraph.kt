package no.svenway.kjorelista

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import no.svenway.kjorelista.data.AppStore
import no.svenway.kjorelista.data.DemoBackend
import no.svenway.kjorelista.data.Preferences
import no.svenway.kjorelista.data.SupabaseBackend

/**
 * Enkel avhengighetsgraf som lever like lenge som prosessen, slik at data overlever
 * at aktiviteten gjenskapes (f.eks. ved bytte av språk).
 */
object AppGraph {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var preferences: Preferences
        private set

    private var _store: AppStore? = null
    val store: AppStore get() = checkNotNull(_store) { "AppGraph.start() er ikke kalt" }

    fun init(context: Context) {
        if (!::preferences.isInitialized) preferences = Preferences(context.applicationContext)
    }

    /** Demomodus slås på med intent-extra «demo=true» eller BuildConfig.DEMO. Supabase-klienten lages da aldri. */
    fun start(context: Context, demo: Boolean, manyTrips: Boolean = false) {
        init(context)
        val current = _store
        if (current != null && (!demo || current.backend.isDemo)) return
        val backend = if (demo) DemoBackend(manyTrips) else SupabaseBackend(context.applicationContext, scope)
        _store = AppStore(backend, scope)
    }
}
