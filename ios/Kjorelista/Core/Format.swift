import Foundation

/// Port av kjoreliste-app/src/lib/format.ts.
enum Format {
    static func locale(_ language: Language) -> Locale {
        Locale(identifier: language == .nb ? "nb_NO" : "en_GB")
    }

    private static var calendar: Calendar { Calendar.current }

    /// «YYYY-MM-DD» tolkes som lokal dato, ikke UTC.
    static func parseDate(_ date: String) -> Date {
        let parts = date.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return Date() }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2])) ?? Date()
    }

    static func toDateString(_ date: Date) -> String {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 1, c.day ?? 1)
    }

    static func capitalize(_ s: String) -> String {
        s.prefix(1).uppercased() + s.dropFirst()
    }

    private static func formatted(_ date: Date, template: String, locale: Locale) -> String {
        let f = DateFormatter()
        f.locale = locale
        f.setLocalizedDateFormatFromTemplate(template)
        return f.string(from: date)
    }

    /// «Tir 14. okt.»
    static func shortDateWithWeekday(_ date: String, _ language: Language) -> String {
        let d = parseDate(date)
        let loc = locale(language)
        let weekday = formatted(d, template: "EEE", locale: loc).replacingOccurrences(of: ".", with: "")
        let dayMonth = formatted(d, template: "dMMM", locale: loc)
        return "\(capitalize(weekday)) \(dayMonth)"
    }

    /// «Tirsdag 14. oktober 2026»
    static func longDate(_ date: String, _ language: Language) -> String {
        let d = parseDate(date)
        let loc = locale(language)
        let weekday = formatted(d, template: "EEEE", locale: loc)
        let rest = formatted(d, template: "dMMMMy", locale: loc)
        return "\(capitalize(weekday)) \(rest)"
    }

    static func number(_ n: Double, _ language: Language, maxDecimals: Int = 1) -> String {
        number(n, locale: locale(language), maxDecimals: maxDecimals)
    }

    static func number(_ n: Double, locale: Locale, maxDecimals: Int = 1) -> String {
        let f = NumberFormatter()
        f.locale = locale
        f.numberStyle = .decimal
        f.maximumFractionDigits = maxDecimals
        f.minimumFractionDigits = 0
        return f.string(from: NSNumber(value: n)) ?? "\(n)"
    }

    static func currencyNOK(_ n: Double, locale: Locale) -> String {
        let f = NumberFormatter()
        f.locale = locale
        f.numberStyle = .currency
        f.currencyCode = "NOK"
        return f.string(from: NSNumber(value: n)) ?? "\(n)"
    }

    /// Klokkeformat for én tur: «01:30»
    static func clock(hours: Int, minutes: Int) -> String {
        if hours == 0 && minutes == 0 { return "-" }
        return String(format: "%02d:%02d", hours, minutes)
    }

    /// Varighet fra desimaltimer: «3t 15m»
    static func duration(_ totalHours: Double, _ language: Language) -> String {
        var h = Int(totalHours.rounded(.down))
        var m = Int(((totalHours - Double(h)) * 60).rounded())
        if m == 60 { h += 1; m = 0 }
        let hu = language == .nb ? "t" : "h"
        if h == 0 { return "\(m)m" }
        if m == 0 { return "\(h)\(hu)" }
        return "\(h)\(hu) \(m)m"
    }

    /// Tall slik JavaScript skriver dem (`String(42.5)` → «42.5», `String(128)` → «128»).
    static func jsNumber(_ n: Double) -> String {
        if n == n.rounded(), abs(n) < 1e15 { return String(Int(n)) }
        return String(n)
    }

    /// Norsk tastatur gir komma som desimaltegn.
    static func parseDecimal(_ s: String) -> Double? {
        let cleaned = s.trimmingCharacters(in: .whitespaces).replacingOccurrences(of: ",", with: ".")
        return Double(cleaned)
    }

    static func decimalField(_ n: Double) -> String {
        n == 0 ? "" : jsNumber(n).replacingOccurrences(of: ".", with: ",")
    }
}

struct MonthStats {
    let count: Int
    let totalKilometers: Double
    let totalHours: Int
    let totalMinutes: Int
    let totalTimeInHours: Double
    let totalExpenses: Double
    let tripsWithPassengers: Int
    let totalPassengerKilometers: Double
    let uniqueRoutes: Int
    let avgKmPerTrip: Double
    let avgTimePerTrip: Double

    init(_ trips: [Trip]) {
        count = trips.count
        totalKilometers = trips.reduce(0) { $0 + $1.kilometers }
        totalHours = trips.reduce(0) { $0 + $1.hours }
        totalMinutes = trips.reduce(0) { $0 + $1.minutes }
        totalTimeInHours = Double(totalHours) + Double(totalMinutes) / 60
        totalExpenses = trips.reduce(0) { $0 + $1.otherExpenses }
        tripsWithPassengers = trips.filter(\.hasPassengers).count
        totalPassengerKilometers = trips.reduce(0) { $0 + $1.passengerKilometers }
        uniqueRoutes = Set(trips.map { $0.route.lowercased() }).count
        avgKmPerTrip = trips.isEmpty ? 0 : totalKilometers / Double(trips.count)
        avgTimePerTrip = trips.isEmpty ? 0 : totalTimeInHours / Double(trips.count)
    }
}

extension Array where Element == Trip {
    /// Turer i gitt måned (month 0–11, som i JavaScript), nyeste først.
    func inMonth(year: Int, month: Int) -> [Trip] {
        let prefix = String(format: "%04d-%02d-", year, month + 1)
        return filter { $0.date.hasPrefix(prefix) }.sorted { $0.date > $1.date }
    }

    func inYear(_ year: Int) -> [Trip] {
        let prefix = String(format: "%04d-", year)
        return filter { $0.date.hasPrefix(prefix) }
    }
}
