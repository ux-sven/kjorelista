package no.svenway.kjorelista.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import no.svenway.kjorelista.R
import no.svenway.kjorelista.data.Language

/** Aktivt språk utledes av ressurskonfigurasjonen (styrt av AppCompatDelegate). */
@Composable
@ReadOnlyComposable
fun currentLanguage(): Language = Language.fromLocale(LocalConfiguration.current.locales[0])

@Composable
fun monthNames(): Array<String> = stringArrayResource(R.array.months)
