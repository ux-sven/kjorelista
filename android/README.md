# Kjørelista for Android

Native Android-app for kjørebok-tjenesten «Kjørelista», skrevet i Kotlin med Jetpack Compose og Material 3.
Felles oppførsel for iOS og Android er beskrevet i [`../docs/spesifikasjon.md`](../docs/spesifikasjon.md).
Den bruker samme Supabase-backend som webløsningen.

## Krav og versjoner

| | |
|---|---|
| minSdk / targetSdk / compileSdk | 29 / 37 / 37 |
| Gradle (wrapper) | 9.9.1 |
| Android Gradle Plugin | 9.5.1 (innebygd Kotlin-støtte) |
| Kotlin | 2.4.21 (Compose- og serialization-plugin) |
| Compose BOM | 2026.09.00 (Material 3) |
| supabase-kt | 3.9.0 (postgrest-kt, auth-kt, functions-kt) med Ktor 3.6.1 (OkHttp) |
| Java | JDK-en som følger med Android Studio (JBR 25) |

Alle versjoner står i `gradle/libs.versions.toml`.

## Bygging

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug        # bygger app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # installerer på tilkoblet enhet/emulator
```

`local.properties` må peke på Android SDK-en (`sdk.dir=/Users/<deg>/Library/Android/sdk`). Filen er ikke sjekket inn.

## Kjøring og demomodus

Demomodus bruker eksempeldata i minnet (Kari Nordmann og fem turer relativt til dagens dato, samme som
de samme eksempeldataene som iOS-appen). Supabase-klienten opprettes aldri, og innlogging/utlogging/sletting av konto
er bare simulert. **Bruk alltid demomodus til testing og skjermbilder – produksjonsdatabasen har aktive brukere.**

```sh
# Start emulatoren
~/Library/Android/sdk/emulator/emulator -avd Pixel_9 -no-snapshot-save &
adb wait-for-device

# Start appen i demomodus (intent-extra)
adb shell am start -n no.svenway.kjorelista/.MainActivity --ez demo true

# …eller bygg med demomodus slått på permanent (BuildConfig.DEMO)
./gradlew installDebug -Pdemo=true
```

Uten `demo=true` starter appen mot ekte Supabase og viser innloggingsskjermen.

Nyttige kommandoer under testing:

```sh
adb shell cmd locale set-app-locales no.svenway.kjorelista --locales nb   # eller en
adb exec-out screencap -p > skjerm.png
```

## Funksjoner

- **Innlogging**: logg inn / opprett konto (segmentert knapp), glemt passord, feltvalidering, autofyll-hint,
  lenke til personvernerklæringen. «Sjekk e-posten din …» vises når registrering krever bekreftelse.
- **Hovedskjerm**: `LargeTopAppBar` med «November 2026», knapper for «I dag» (bare i annen måned), eksport og
  innstillinger, Material 3 FAB, månedsoppsummering med nøkkeltall, årsrad, passasjerer, utgifter og milepæler.
  Sveip på oppsummeringen bytter måned (venstre = neste) med glide-animasjon og haptikk; pilene under kan også
  trykkes, og skjermlesere får «Forrige/Neste måned» som egne handlinger. Dra-for-å-oppdatere og lasteindikator
  ved første henting.
- **Turliste**: trykk = rediger, langt trykk = meny (Rediger / Dupliser / Slett). Sletting krever bekreftelse.
- **Ny / rediger / dupliser tur**: fullskjermdialog med Lagre, gruppert skjema (Tur, Reisetid, Passasjerer,
  Utgifter, Kommentar), `DatePickerDialog`, 24-timers `TimePicker` for varighet, validering som webskjemaet,
  komma som desimaltegn, forslag til strekning fra tidligere turer (fyller km/varighet bare hvis tomme).
- **Innstillinger**: konto med initialer, språk (Norsk/English), utseende (følg systemet/lys/mørk),
  personvernerklæring, logg ut og slett konto (begge med bekreftelse), versjon.
- **Eksport**: `ModalBottomSheet` med PDF eller CSV. PDF lages fra samme HTML-mal som webløsningen
  (WebView utenfor skjermen → `PdfDocument`, A4), CSV med BOM og samme kolonner. Filene deles via systemets
  delingsark som `kjorerapport-<måned>-<år>.pdf/.csv`.

## Prosjektstruktur

```
app/src/main/
├── AndroidManifest.xml              # FileProvider, per-app-språk, INTERNET
├── java/no/svenway/kjorelista/
│   ├── KjorelistaApp.kt             # Application
│   ├── MainActivity.kt              # Eneste aktivitet (AppCompatActivity), leser demo-flagget, tema
│   ├── AppGraph.kt                  # Prosess-levetid: Preferences + AppStore (demo eller Supabase)
│   ├── data/
│   │   ├── Models.kt                # Trip, NewTrip, UserProfile, AuthState, BackendException
│   │   ├── Format.kt                # Port av format.ts (datoer, tall, varighet, månedsstatistikk)
│   │   ├── Backend.kt               # Grensesnitt for auth + turer
│   │   ├── SupabaseBackend.kt       # supabase-kt: trips, profiles, delete-account
│   │   ├── SecureSessionManager.kt  # Sesjon kryptert med AES-GCM-nøkkel i Android Keystore
│   │   ├── DemoBackend.kt           # Demodata i minnet
│   │   ├── AppStore.kt              # Port av session.tsx (profil, turer, lasting, feil)
│   │   ├── Preferences.kt           # Tema og språk (AppCompatDelegate.setApplicationLocales)
│   │   └── Report.kt                # HTML-mal, CSV, PDF-rendering og deling
│   └── ui/
│       ├── AppNavigation.kt         # Navigation Compose: innlogget / ikke innlogget
│       ├── Haptics.kt, Locals.kt
│       ├── theme/Theme.kt           # Farger fra theme.tsx (lys/mørk)
│       ├── components/              # Section, MonthSwiper, MonthlySummary, TripRow, dialoger
│       └── screens/                 # Login, Home, Trip, Settings, Privacy
└── res/
    ├── values/strings.xml           # Norsk (standard)
    ├── values-en/strings.xml        # Engelsk
    ├── mipmap-*/                    # Adaptive icon (forgrunn/bakgrunn/monokrom) + ic_launcher
    └── drawable-nodpi/app_logo.png  # Logo på innloggingsskjermen
```

## Backend

- Supabase-URL og offentlig anon-nøkkel ligger i `SupabaseBackend.kt` (`SupabaseConfig`). RLS beskytter data.
- Tabellen `trips` (snake_case-kolonner) og `profiles` (`first_name`, `last_name`) deles med webløsningen og må ikke endres.
- `date` er `YYYY-MM-DD` og tolkes alltid som lokal dato.
- Slett konto kaller Edge-funksjonen `delete-account` med brukerens JWT og logger ut etterpå.
- Feil fra Supabase vises alltid i en dialog; appen faller aldri tilbake til lokal lagring.
