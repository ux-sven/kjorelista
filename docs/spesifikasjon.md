# Kjørelista – spesifikasjon for native iOS- og Android-apper

Felles grunnlag for de to native appene i dette repoet. Begge skal oppføre seg likt; endringer i oppførsel
gjøres i begge og beskrives her. (Appene ble opprinnelig portert fra en Expo/React Native-prototype, som
kommentarer i koden fortsatt viser til.)

| | iOS | Android |
|---|---|---|
| Mappe | `ios/` | `android/` |
| Språk/UI | Swift 6, SwiftUI | Kotlin, Jetpack Compose (Material 3) |
| Minstekrav | iOS 18.0 | Android 10 (minSdk 29), targetSdk/compileSdk 37 |
| Bundle-ID / package | `com.svenway.kjorelista` (eksisterende App Store-app, versjon 2.0) | `no.svenway.kjorelista` |
| Supabase-klient | `supabase-swift` (SPM) | `supabase-kt` (postgrest-kt, auth-kt, functions-kt) + Ktor |
| Navn på hjemskjerm | Kjørelista | Kjørelista |

Ingen tredjeparts UI-biblioteker. Bruk plattformens egne komponenter (SwiftUI `NavigationStack`, `List`/`Form`,
`DatePicker`, `.contextMenu`, `.swipeActions`, `ShareLink`/`UIActivityViewController`; Compose `Scaffold`,
`LargeTopAppBar`, `FloatingActionButton`, `DatePickerDialog`, `TimePicker`, `ModalBottomSheet`, `DropdownMenu`).

## Backend (delt med webløsningen – må ikke endres)

- URL: `https://cwuvgbmqdasqdzlxcpab.supabase.co`
- Anon-nøkkel: se `ios/Kjorelista/Core/Config.swift` / `android/.../data/SupabaseBackend.kt` (offentlig nøkkel, RLS beskytter data)
- Tabeller: `trips` (id, user_id, date, kilometers, route, hours, minutes, has_passengers, passenger_count,
  passenger_kilometers, other_expenses, expense_description, comment, created_at, updated_at) og `profiles` (`first_name`, `last_name`).
  Kolonnenavn i snake_case. `date` er `YYYY-MM-DD` og tolkes som **lokal dato**, aldri UTC.
- Auth: e-post + passord (`signInWithPassword`, `signUp` med metadata `first_name`/`last_name`, `resetPasswordForEmail` uten redirect).
  Hvis `signUp` returnerer bruker uten sesjon: vis «Sjekk e-posten din for å bekrefte kontoen…».
  Session lagres sikkert (Keychain / EncryptedSharedPreferences eller DataStore) og fornyes automatisk.
- Slett konto: `POST functions/v1/delete-account` med brukerens JWT. Kildekode i `supabase/functions/delete-account`
  (deploy: `npx supabase functions deploy delete-account --project-ref cwuvgbmqdasqdzlxcpab --use-api`). Logg ut etterpå.
- Feil fra Supabase skal **vises** for brukeren. Aldri falle stille tilbake til lokal lagring.

## Demomodus (obligatorisk – brukes til all testing og til skjermbilder)

Bruker Kari Nordmann, 5 turer relativt til dagens dato, alt i minnet (`DemoBackend` i begge apper).
- iOS: aktiveres med launch-argument `-demo` (f.eks. `xcrun simctl launch … com.svenway.kjorelista -demo`) eller env `KJORELISTA_DEMO=1`.
- Android: aktiveres med intent-extra `demo=true` (`adb shell am start -n …/.MainActivity --ez demo true`) eller en debug-BuildConfig-flagg.
- **Logg aldri inn med ekte kontoer, opprett ikke kontoer og kall aldri delete-account under testing.** Produksjonsdatabasen har aktive brukere.

## Skjermer og oppførsel

1. **Innlogging**: app-ikonet som logo, tittel «Kjørelista», tagline. Segmentkontroll Logg inn / Opprett konto,
   «Glemt passord?»-modus. Feltvalidering og feilmeldinger som i filen. Lenke til personvernerklæring.
   Autofyll av e-post/passord (textContentType / autofill hints).
2. **Hovedskjerm**:
   - Stor tittel = «Oktober 2026» (månedsnavn fra oversettelsene + år). Kollapser ved scroll (iOS large title / Compose LargeTopAppBar).
   - Header-knapper: profil/innstillinger (venstre på iOS, høyre på Android), «I dag» (vises kun når annen måned enn inneværende), eksport (del-ikon).
   - **Flytende «+»-knapp nede til høyre** på begge plattformer (iOS: rund 60pt, primærfarge, skygge; Android: Material 3 FAB).
   - **Månedsoppsummering**: flat boks med to nøkkeltall (Total kjørt, Total kjøretid, med ⌀ per tur),
     rader for «Totalt kjørt i ÅÅÅÅ», «Med passasjerer», «Utgifter» (kun når > 0), milepæl-chips med systemikoner (ingen emoji).
     Tom måned: bil-ikon + «Ingen turer registrert i <måned>» + årsrad.
     **Sveip horisontalt på boksen for å bytte måned** (venstre = neste, høyre = forrige) med glide-animasjon og haptikk.
     Under boksen: «‹ Sveip for å bytte måned ›» der pilene også kan trykkes. Tilgjengelighet: adjustable (iOS) / custom actions.
   - Vis lasteindikator (ikke tom-tilstand) mens turer hentes første gang. Dra-for-å-oppdatere.
   - **Turliste**: dato «Tir 14. okt.», strekning, km-merke, klokkeslett-format varighet «01:35»,
     passasjer- og utgiftsinfo, kommentar i gul boks. Trykk = rediger.
     iOS: kontekstmeny (Rediger / Dupliser / Slett) + sveip-for-å-slette. Android: langt trykk = meny (Rediger / Dupliser / Slett).
     Slett krever bekreftelse. Liste-bunn må ikke skjules av FAB.
3. **Ny / rediger tur**, presentert som modal/sheet (iOS) / fullskjerm-dialog (Android):
   - Avbryt (venstre) og Lagre (høyre) i toppen. Tittel «Ny kjøretur» / «Rediger tur».
   - Gruppert skjema: **Tur** (Dato, Kilometer med «km»-suffiks, Strekning), **Reisetid** (Varighet – timer:minutter-hjul / TimePicker 24t),
     **Passasjerer** (bryter, ved på: Antall, Km med passasjerer), **Utgifter** (Beløp kr, Beskrivelse), **Kommentar** (flerlinje).
     Rediger-modus: «Slett kjøretur»-knapp nederst.
   - Validering som webskjemaet: dato påkrevd, km > 0, strekning påkrevd, varighet valgt; med passasjerer: antall > 0 og passasjer-km > 0. Komma som desimaltegn godtas.
   - **Strekning**: stor forbokstav på hvert ord (`.words` / `KeyboardCapitalization.Words`), autokorrektur av.
     **Forslag** fra tidligere turer mens feltet har fokus (kun når «Forslag til strekning» er på i innstillingene):
     tomt felt → 3 mest brukte; ellers opptil 4 treff (inneholder, treff i starten først).
     Valg fyller strekning. Kilometer fra siste tur på strekningen fylles inn **kun når «Fyll inn kilometer automatisk» er på
     og feltet er tomt**. Varighet fylles ikke inn.
   - **Dupliser** åpner skjemaet forhåndsutfylt fra en tur, med dagens dato, som ny tur.
4. **Innstillinger**: gruppert liste – Konto (initialer-avatar, navn, e-post),
   Registrering av turer (bryter «Forslag til strekning», standard av; når den er på vises bryter «Fyll inn kilometer automatisk»,
   standard av; lagres lokalt på enheten), Språk (Norsk/English, hake),
   Utseende (Følg systemet / Lys / Mørk), Personvernerklæring, Logg ut (bekreft), Slett konto (destruktiv, bekreft, footer-tekst), versjon.
5. **Personvern**: tekst fra oversettelsene, kontakt-e-post `svenway+kjorelista@gmail.com` (mailto).
6. **Eksport**: valg PDF eller CSV. PDF lages fra **samme HTML-mal** i begge apper (samme som webløsningen, med HTML-escaping),
   iOS via `UIMarkupTextPrintFormatter`/`WKWebView.createPDF`, Android via WebView + `PrintManager`/`PdfDocument`.
   CSV med BOM og samme kolonner. Del via systemets delingsark. Filnavn `kjorerapport-<måned>-<år>.pdf/.csv`.

## Språk, tema, haptikk

- Tekster (nb og en): iOS: String Catalog (`ios/Kjorelista/Localization/Localizable.xcstrings`, typede oppslag genereres med `ios/scripts/generate-strings.mjs`);
  Android: `values/strings.xml` + `values-en/strings.xml` (standard nb, så `values` = norsk, `values-en` = engelsk).
  Språkvalg i appen overstyrer systemspråk (iOS: `.environment(\.locale)` + egen oppslag; Android: `AppCompatDelegate.setApplicationLocales`).
- Farger (lys/mørk): `ios/Kjorelista/Core/Theme.swift` / Android `ui/theme/Theme.kt`. Primær `#3498DB` (mørk `#60A5FA`), suksess `#27AE60`, advarsel `#D68910`, destruktiv `#C0392B`.
- Haptikk: lett ved navigering, suksess/feil ved lagring/sletting.
- Tall og datoer formateres med `nb_NO` / `en_GB` (se `Format.swift` / `Format.kt`).
- Dynamisk tekststørrelse og skjermleser skal fungere.

## Ikoner

Kildefiler i `assets/icon/` (genereres av `swift assets/icon/generate-icons.swift assets/icon`):
`icon.png` (1024, iOS AppIcon), `android-icon-foreground.png`, `android-icon-background.png`, `android-icon-monochrome.png`
(adaptive icon, 108dp-lag), `splash-icon.png`. Android trenger også `ic_launcher` i mipmap-tetthetene (skaler fra 1024).

## Ferdig når

- Prosjektet bygger fra kommandolinjen uten feil eller advarsler som betyr noe (`xcodebuild` / `./gradlew assembleDebug`).
- Appen er startet i simulator/emulator **i demomodus**, og hver skjerm og hovedflyt er sjekket med skjermbilder:
  hovedskjerm, sveip mellom måneder, ny tur med forslag, rediger, dupliser, slett, innstillinger, mørk modus, engelsk, eksport-dialog.
- `README.md` i prosjektmappen forklarer bygging, kjøring, demomodus og prosjektstruktur (på norsk).
