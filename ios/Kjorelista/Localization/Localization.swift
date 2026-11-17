import Foundation
import Observation
import SwiftUI

enum Language: String, CaseIterable, Sendable {
    case nb, en

    /// Norsk hvis enheten er satt til norsk (nb/no/nn), ellers engelsk – som i Expo-appen.
    static var device: Language {
        let code = Locale.preferredLanguages.first.map { Locale(identifier: $0).language.languageCode?.identifier ?? "" } ?? ""
        return ["nb", "no", "nn"].contains(code.lowercased()) ? .nb : .en
    }
}

/// Slår opp tekster i String Catalog-en (Localizable.xcstrings) for valgt språk,
/// uavhengig av systemspråket.
struct Lookup {
    let bundle: Bundle

    init(language: Language) {
        bundle = Bundle.main.path(forResource: language.rawValue, ofType: "lproj").flatMap(Bundle.init(path:)) ?? .main
    }

    func callAsFunction(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "Localizable")
    }
}

/// Språk- og temavalg. Lagres i UserDefaults med samme nøkler som Expo-appen brukte i AsyncStorage.
@MainActor
@Observable
final class AppSettings {
    private static let languageKey = "drive-log-language"
    private static let themeKey = "driving-log-theme"
    private static let routeSuggestionsKey = "route-suggestions"
    private static let autofillKilometersKey = "autofill-kilometers"

    var language: Language {
        didSet {
            UserDefaults.standard.set(language.rawValue, forKey: Self.languageKey)
            lookup = Lookup(language: language)
        }
    }

    var theme: ThemePreference {
        didSet { UserDefaults.standard.set(theme.rawValue, forKey: Self.themeKey) }
    }

    /// Forslag til strekning fra tidligere turer. Av som standard.
    var routeSuggestions: Bool {
        didSet { UserDefaults.standard.set(routeSuggestions, forKey: Self.routeSuggestionsKey) }
    }

    /// Fyll inn kilometer fra forrige tur når et forslag velges. Av som standard;
    /// har bare virkning når forslag er slått på.
    var autofillKilometers: Bool {
        didSet { UserDefaults.standard.set(autofillKilometers, forKey: Self.autofillKilometersKey) }
    }

    private(set) var lookup: Lookup

    init() {
        let defaults = UserDefaults.standard
        let language = defaults.string(forKey: Self.languageKey).flatMap(Language.init(rawValue:)) ?? .device
        self.language = language
        self.theme = defaults.string(forKey: Self.themeKey).flatMap(ThemePreference.init(rawValue:)) ?? .system
        self.routeSuggestions = defaults.bool(forKey: Self.routeSuggestionsKey)
        self.autofillKilometers = defaults.bool(forKey: Self.autofillKilometersKey)
        self.lookup = Lookup(language: language)
    }

    /// Tekster delt med webløsningen: `t.trips.addNew`
    var t: Translations { Translations(lookup: lookup) }
    /// Tekster som bare finnes i appen: `s.settings`
    var s: AppStrings { AppStrings(lookup: lookup) }

    var locale: Locale { Format.locale(language) }
}
