package no.svenway.kjorelista

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.svenway.kjorelista.data.Preferences
import no.svenway.kjorelista.data.ThemePreference
import no.svenway.kjorelista.ui.AppRoot
import no.svenway.kjorelista.ui.theme.KjorelistaTheme

/**
 * Eneste aktivitet. AppCompatActivity brukes for per-app-språk (AppCompatDelegate.setApplicationLocales).
 *
 * Demomodus: `adb shell am start -n no.svenway.kjorelista/.MainActivity --ez demo true`
 * eller bygg med `-Pdemo=true`.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Preferences.applyDefaultLanguage()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val demo = BuildConfig.DEMO || intent.getBooleanExtra(EXTRA_DEMO, false)
        AppGraph.start(this, demo, manyTrips = intent.getBooleanExtra(EXTRA_DEMO_MANY, false))

        setContent {
            val preference by AppGraph.preferences.theme.collectAsStateWithLifecycle()
            val dark = when (preference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            KjorelistaTheme(dark = dark) {
                AppRoot(store = AppGraph.store, preferences = AppGraph.preferences)
            }
        }
    }

    companion object {
        const val EXTRA_DEMO = "demo"
        /** Med demo: 30 ekstra turer i forrige måned (test av lange lister og PDF-sidedeling). */
        const val EXTRA_DEMO_MANY = "demo_many"
    }
}
