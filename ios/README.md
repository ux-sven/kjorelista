# Kjørelista – iOS

Native iOS-app (Swift 6 + SwiftUI, iOS 18.0+) for kjørebok-tjenesten Kjørelista. Felles oppførsel for iOS og
Android er beskrevet i [`../docs/spesifikasjon.md`](../docs/spesifikasjon.md). Appen bruker samme Supabase-backend
som webløsningen (tabellene `trips` og `profiles`, Edge-funksjonen `delete-account` i `../supabase/functions`).

- Bundle-ID: `no.svenway.kjorelista`, navn på hjemskjerm: «Kjørelista»
- Eneste avhengighet: [supabase-swift](https://github.com/supabase/supabase-swift) 2.x (Swift Package Manager)
- Ingen tredjeparts UI-biblioteker

## Bygge

Krever Xcode 16 eller nyere (prosjektet bruker filsystem-synkroniserte grupper, så nye `.swift`-filer
i `Kjorelista/` blir automatisk med i bygget – ingen manuell redigering av prosjektfilen).

```sh
# Simulator
xcodebuild -scheme Kjorelista -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build

# Release for enhet (uten signering, for å sjekke at alt kompilerer)
xcodebuild -scheme Kjorelista -configuration Release -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build
```

Eller åpne `Kjorelista.xcodeproj` i Xcode. Swift-pakkene hentes automatisk første gang.

**App Store:** sett `DEVELOPMENT_TEAM` (Signing & Capabilities i Xcode), øk `CURRENT_PROJECT_VERSION`/
`MARKETING_VERSION`, og velg *Product → Archive*. App-ikonet er et enkelt 1024×1024-bilde uten alfakanal.

## Kjøre i demomodus

Demomodus bruker faste eksempeldata (Kari Nordmann, 5 turer relativt til
dagens dato). Alt ligger i minnet, og **appen gjør ingen nettverkskall** – Supabase-klienten opprettes ikke
engang. Innlogging, registrering og «glemt passord» er simulert, og «Slett konto» gir bare en feilmelding.

Aktiveres med launch-argumentet `-demo` eller miljøvariabelen `KJORELISTA_DEMO=1`:

```sh
scripts/run-demo.sh                     # bygger, installerer og starter i første bootede simulator
scripts/run-demo.sh <simulator-UDID>    # eller i en bestemt simulator

# manuelt
xcrun simctl install booted build/Build/Products/Debug-iphonesimulator/Kjorelista.app
xcrun simctl launch booted no.svenway.kjorelista -demo
```

I Xcode: skjemaet har argumentet `-demo` forhåndsutfylt under *Edit Scheme → Run → Arguments* (skru det på).

> Bruk alltid demomodus til testing. Produksjonsdatabasen har aktive brukere – ikke logg inn med ekte
> kontoer, ikke opprett testkontoer og ikke kall slett konto.

## Prosjektstruktur

```
Kjorelista.xcodeproj/          Prosjekt + delt skjema «Kjorelista»
Kjorelista/
  App/KjorelistaApp.swift      @main, miljø (språk, tema, locale), RootView (innlogget / ikke innlogget)
  Core/
    Config.swift               Supabase-URL og anon-nøkkel, kontakt-e-post, demo-flagg, versjon
    Models.swift               Trip, NewTrip, UserProfile, AppUser
    Format.swift               Port av format.ts (datoer som lokal dato, tall, varighet) + MonthStats
    Theme.swift                Farger fra theme.tsx (lys/mørk), ThemePreference
    Haptics.swift              Port av haptic.ts
  Data/
    Backend.swift              Protokoll for auth + turer
    SupabaseBackend.swift      supabase-swift: auth (Keychain, auto-refresh), trips, profiles, delete-account
    DemoBackend.swift          Demodata i minnet
    SessionStore.swift         Port av session.tsx (@Observable)
  Localization/
    Localizable.xcstrings      String Catalog (nb + en) – GENERERT
    L10n.generated.swift       Typede oppslag t.trips.addNew / s.settings – GENERERT
    Localization.swift         Språkvalg som overstyrer systemspråket, AppSettings (språk + tema)
  Features/
    Login/LoginView.swift      login.tsx
    Home/HomeView.swift        index.tsx: stor tittel, verktøylinje, FAB, liste, eksport, slett
    Home/MonthSwiper.swift     Sveip mellom måneder (UIPanGestureRecognizer som bare tar vannrette sveip)
    Home/MonthlySummaryView.swift  MonthlySummary.tsx
    Home/TripRowView.swift     TripRow.tsx
    Trip/TripFormView.swift    trip.tsx: ny / rediger / dupliser, forslag til strekning, validering
    Settings/SettingsView.swift, PrivacyView.swift
    Export/Report.swift        report.ts: HTML-mal → PDF (WKWebView + UIPrintPageRenderer, A4), CSV med BOM
    Export/ReportStyle.swift   CSS kopiert uendret fra report.ts
  Assets.xcassets              AppIcon (1024, single-size), Logo (samme bilde), AccentColor
scripts/
  generate-strings.mjs         Genererer String Catalog + Swift-oppslag fra translations.ts/appStrings.ts
  run-demo.sh                  Bygg + installer + start i demomodus
```

### Tekster og språk

Tekstene ligger i `Kjorelista/Localization/Localizable.xcstrings` (norsk og engelsk), som er fasit på iOS.
Legg til eller endre tekster der, og generer deretter de typede oppslagene (`t.trips.addNew`, `s.settings`):

```sh
node scripts/generate-strings.mjs   # skriver Kjorelista/Localization/L10n.generated.swift
```

Tilsvarende tekster for Android ligger i `../android/app/src/main/res/values*/strings.xml` og må holdes i takt.

Språkvalget i Innstillinger overstyrer systemspråket: oppslag gjøres i `nb.lproj`/`en.lproj` direkte, og
`\.locale` settes til `nb_NO`/`en_GB` slik at datovelgere og tallformat følger valget. Standard er norsk hvis
enheten er satt til nb/no/nn, ellers engelsk. Språk og tema lagres i UserDefaults med samme nøkler som
Expo-appen (`drive-log-language`, `driving-log-theme`).

Eksportert rapport (PDF/CSV) er alltid på norsk, som i webløsningen.
