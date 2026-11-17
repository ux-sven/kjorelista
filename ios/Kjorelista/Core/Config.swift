import Foundation

/// Samme Supabase-prosjekt som webløsningen og Expo-appen (kjoreliste-app/src/lib/config.ts).
/// Anon-nøkkelen er offentlig; tilgang styres av Row Level Security i databasen.
enum Config {
    static let supabaseURL = URL(string: "https://cwuvgbmqdasqdzlxcpab.supabase.co")!
    static let supabaseAnonKey =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN3dXZnYm1xZGFzcWR6bHhjcGFiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTE0Mzc3NTgsImV4cCI6MjA2NzAxMzc1OH0.EwNlo3KSlsSTONFqk6NaBDkZ7WMqBOijZovxDd6tWGo"

    static let contactEmail = "svenway+kjorelista@gmail.com"

    /// Demomodus: eksempeldata i minnet, ingen nettverkskall mot Supabase.
    /// Aktiveres med launch-argumentet `-demo` eller miljøvariabelen `KJORELISTA_DEMO=1`.
    static let isDemo: Bool = {
        let info = ProcessInfo.processInfo
        return info.arguments.contains("-demo") || info.environment["KJORELISTA_DEMO"] == "1"
    }()

    static var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
    }
}
