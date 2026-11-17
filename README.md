# Kjørelista

Native iOS- og Android-apper for kjørebok-tjenesten Kjørelista – «en enklere digital kjørebok».
Appene bruker samme Supabase-backend som webløsningen, så brukerne logger inn med samme konto og ser de
samme turene på nett, iPhone og Android.

| Mappe | Innhold |
|---|---|
| [`ios/`](ios/) | iOS-app i Swift 6 og SwiftUI (iOS 18+). Åpne `ios/Kjorelista.xcodeproj` i Xcode. |
| [`android/`](android/) | Android-app i Kotlin og Jetpack Compose med Material 3 (Android 10+). Åpne `android/` i Android Studio. |
| [`supabase/functions/`](supabase/functions/) | Edge-funksjonen `delete-account` (sletting av konto i appene, påkrevd av Apple). |
| [`docs/spesifikasjon.md`](docs/spesifikasjon.md) | Felles spesifikasjon: skjermer, oppførsel, backend og demomodus. |
| [`assets/icon/`](assets/icon/) | App-ikonet og skriptet som genererer det. |

## Kom i gang

**iOS** (Xcode 26 eller nyere):

```bash
cd ios && scripts/run-demo.sh
```

Bygger, installerer og starter appen i demomodus i den bootede simulatoren. Se [`ios/README.md`](ios/README.md).

**Android** (Android Studio med SDK 37):

```bash
cd android && ./gradlew installDebug
```

Krever `android/local.properties` med `sdk.dir=...` (lages automatisk av Android Studio). Se [`android/README.md`](android/README.md).

## Demomodus

Begge appene har en demomodus med eksempeldata og uten nettverk, brukt til testing og skjermbilder.
Bruk den til all testing, aldri ekte kontoer: produksjonsdatabasen har aktive brukere.

- iOS: start med launch-argumentet `-demo`
- Android: `adb shell am start -n no.svenway.kjorelista/.MainActivity --ez demo true` (legg til `--ez demo_many true` for 30 ekstra turer)

## Backend

Supabase-prosjekt `cwuvgbmqdasqdzlxcpab`, delt med webløsningen. Tabellene `trips` og `profiles` er beskyttet
med Row Level Security; anon-nøkkelen i appene er offentlig. Edge-funksjonen legges ut med:

```bash
npx supabase functions deploy delete-account --project-ref cwuvgbmqdasqdzlxcpab --use-api
```
