package no.svenway.kjorelista.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemePreference(val key: String) { SYSTEM("system"), LIGHT("light"), DARK("dark") }

/** Tema og registreringsvalg lagres lokalt. Språk lagres av AppCompatDelegate (per-app-språk). */
class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        ThemePreference.entries.firstOrNull { it.key == prefs.getString("theme", null) } ?: ThemePreference.SYSTEM,
    )
    val theme: StateFlow<ThemePreference> = _theme

    fun setTheme(value: ThemePreference) {
        _theme.value = value
        prefs.edit { putString("theme", value.key) }
    }

    // Forslag til strekning fra tidligere turer. Av som standard.
    private val _routeSuggestions = MutableStateFlow(prefs.getBoolean("route_suggestions", false))
    val routeSuggestions: StateFlow<Boolean> = _routeSuggestions

    fun setRouteSuggestions(value: Boolean) {
        _routeSuggestions.value = value
        prefs.edit { putBoolean("route_suggestions", value) }
    }

    // Fyll inn kilometer fra forrige tur når et forslag velges. Av som standard, og har bare
    // virkning når forslag er slått på.
    private val _autofillKilometers = MutableStateFlow(prefs.getBoolean("autofill_kilometers", false))
    val autofillKilometers: StateFlow<Boolean> = _autofillKilometers

    fun setAutofillKilometers(value: Boolean) {
        _autofillKilometers.value = value
        prefs.edit { putBoolean("autofill_kilometers", value) }
    }

    companion object {
        /** Språkvalget i appen overstyrer systemspråket. */
        fun setLanguage(language: Language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }

        /**
         * Uten eget valg: norsk system (nb/no/nn) gir norsk, alt annet engelsk – som i Expo-appen.
         * Standardressursene er norske, så andre språk enn engelsk må settes eksplisitt.
         */
        fun applyDefaultLanguage() {
            if (!AppCompatDelegate.getApplicationLocales().isEmpty) return
            val system = LocaleListCompat.getAdjustedDefault()[0]
            if (Language.fromLocale(system) == Language.EN && system?.language != "en") setLanguage(Language.EN)
        }
    }
}
