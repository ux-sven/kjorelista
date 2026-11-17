import SwiftUI
import UIKit
import WebKit

enum ReportFormat { case pdf, csv }

/// Port av kjoreliste-app/src/lib/report.ts: samme HTML-mal (PDF) og samme CSV-kolonner.
/// Rapporten er alltid på norsk, som i webløsningen.
enum Report {
    private static let nb = Locale(identifier: "nb_NO")

    static func escapeHTML(_ s: String) -> String {
        s.replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "\"", with: "&quot;")
    }

    static func fileBase(month: String, year: Int) -> String {
        let allowed = Set("abcdefghijklmnopqrstuvwxyz0123456789æøå")
        return "kjorerapport-\(String(month.lowercased().filter { allowed.contains($0) }))-\(year)"
    }

    private static func num(_ n: Double) -> String { Format.number(n, locale: nb, maxDecimals: 1) }
    private static func nok(_ n: Double) -> String { Format.currencyNOK(n, locale: nb) }

    // MARK: - HTML

    static func html(trips: [Trip], month: String, year: Int, profile: UserProfile?, now: Date = Date()) -> String {
        let totalKilometers = trips.reduce(0) { $0 + $1.kilometers }
        let totalHours = trips.reduce(0) { $0 + $1.hours }
        let totalMinutes = trips.reduce(0) { $0 + $1.minutes }
        let totalTimeInHours = Double(totalHours) + Double(totalMinutes) / 60
        let totalExpenses = trips.reduce(0) { $0 + $1.otherExpenses }
        let totalPassengerKilometers = trips.reduce(0) { $0 + $1.passengerKilometers }
        let tripsWithPassengers = trips.filter(\.hasPassengers).count

        func formatTime(_ total: Double) -> String {
            var hours = Int(total.rounded(.down))
            var minutes = Int(((total - Double(hours)) * 60).rounded())
            if minutes == 60 { hours += 1; minutes = 0 }
            if hours == 0 { return "\(minutes)m" }
            if minutes == 0 { return "\(hours)t" }
            return "\(hours)t \(minutes)m"
        }

        func formatTimeForTable(_ hours: Int, _ minutes: Int) -> String {
            let total = hours * 60 + minutes
            let h = total / 60, m = total % 60
            if h == 0 { return "\(m)m" }
            if m == 0 { return "\(h)t" }
            return "\(h)t \(m)m"
        }

        let userInfo = profile.map {
            "<div class=\"user-info\">Bruker: \(escapeHTML($0.firstName)) \(escapeHTML($0.lastName))</div>"
        } ?? ""

        var body = ""
        if trips.isEmpty {
            body = """
                <div class="no-trips">
                    <strong>Ingen kjøreturer registrert</strong><br>
                    Det er ikke registrert noen kjøreturer for denne måneden.
                </div>
                """
        } else {
            var rows = ""
            for trip in trips {
                let date = Format.longDate(trip.date, .nb)
                let time = trip.hours > 0 || trip.minutes > 0 ? "\(trip.hours)t \(trip.minutes)m" : "-"
                let passengers = trip.hasPassengers ? String(trip.passengerCount) : "-"
                let passengerKm = trip.hasPassengers && trip.passengerKilometers > 0 ? num(trip.passengerKilometers) : "-"
                let expenses = trip.otherExpenses > 0 ? nok(trip.otherExpenses) : "-"
                let expenseDesc = trip.expenseDescription.trimmingCharacters(in: .whitespaces).isEmpty ? "-" : escapeHTML(trip.expenseDescription)
                rows += """

                    <tr>
                        <td class="date-cell">\(date)</td>
                        <td class="route-cell">\(escapeHTML(trip.route))</td>
                        <td class="number-cell">\(num(trip.kilometers))</td>
                        <td class="number-cell">\(time)</td>
                        <td class="number-cell">\(passengers)</td>
                        <td class="number-cell">\(passengerKm)</td>
                        <td class="number-cell">\(expenses)</td>
                        <td class="expense-cell">\(expenseDesc)</td>
                    </tr>
                    """
                if !trip.comment.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    rows += """

                        <tr class="comment-row">
                            <td colspan="8">
                                <span class="comment-label">Kommentar:</span>
                                \(escapeHTML(trip.comment))
                            </td>
                        </tr>
                        """
                }
            }
            body = """
                <table class="trips-table">
                    <thead>
                        <tr>
                            <th>Dato</th>
                            <th>Rute</th>
                            <th>Km</th>
                            <th>Tid</th>
                            <th>Pass.</th>
                            <th>Pass. km</th>
                            <th>Utgifter</th>
                            <th>Utgiftsbeskr.</th>
                        </tr>
                    </thead>
                    <tbody>
                        \(rows)

                        <!-- Summary row -->
                        <tr class="summary-row">
                            <td class="summary-label">TOTALT</td>
                            <td class="summary-label">\(trips.count) \(trips.count == 1 ? "tur" : "turer")</td>
                            <td class="number-cell summary-label">\(num(totalKilometers)) km</td>
                            <td class="number-cell summary-label">\(formatTimeForTable(totalHours, totalMinutes))</td>
                            <td class="number-cell summary-label">\(tripsWithPassengers)</td>
                            <td class="number-cell summary-label">\(num(totalPassengerKilometers)) km</td>
                            <td class="number-cell summary-label">\(totalExpenses > 0 ? nok(totalExpenses) : "-")</td>
                            <td class="summary-label">-</td>
                        </tr>
                    </tbody>
                </table>
                """
        }

        let dateFormatter = DateFormatter()
        dateFormatter.locale = nb
        dateFormatter.dateFormat = "d.M.yyyy"
        let timeFormatter = DateFormatter()
        timeFormatter.locale = nb
        timeFormatter.dateFormat = "HH:mm"

        let safeMonth = escapeHTML(month)
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <title>Kjørerapport - \(safeMonth) \(year)</title>
                <style>
            \(ReportStyle.css)
                </style>
            </head>
            <body>
                <div class="page-header">
                    <h1>Kjørerapport</h1>
                    <h2>\(safeMonth) \(year)</h2>
                    \(userInfo)
                </div>

                \(body)

                <!-- Summary on separate page -->
                <div class="summary-page">
                    <div class="summary">
                        <h3>Sammendrag \(safeMonth) \(year)</h3>
                        <div class="summary-grid">
                            <div class="summary-item">
                                <div class="summary-value">\(trips.count)</div>
                                <div class="summary-label">Antall turer</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">\(num(totalKilometers))</div>
                                <div class="summary-label">Total kilometer</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">\(formatTime(totalTimeInHours))</div>
                                <div class="summary-label">Total kjøretid</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">\(tripsWithPassengers)</div>
                                <div class="summary-label">Turer med passasjerer</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">\(num(totalPassengerKilometers))</div>
                                <div class="summary-label">Kilometer med passasjerer</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">\(nok(totalExpenses))</div>
                                <div class="summary-label">Andre utgifter</div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="footer">
                    <p><strong>Kjøreliste App</strong> • Generert \(dateFormatter.string(from: now)) kl. \(timeFormatter.string(from: now))</p>
                </div>
            </body>
            </html>
            """
    }

    // MARK: - PDF

    /// Lager PDF (A4) fra HTML-malen med iOS sin utskriftsmotor og returnerer filen.
    @MainActor
    static func pdf(trips: [Trip], month: String, year: Int, profile: UserProfile?) async throws -> URL {
        let sorted = trips.sorted { $0.date < $1.date }
        let html = html(trips: sorted, month: month, year: year, profile: profile)

        let data = try await HTMLPDFRenderer.render(html: html, title: "Kjørerapport - \(month) \(year)")

        let url = try outputURL("\(fileBase(month: month, year: year)).pdf")
        try data.write(to: url, options: .atomic)
        return url
    }

    // MARK: - CSV

    /// CSV med BOM (for Excel) og samme kolonner som webløsningen.
    static func csv(trips: [Trip], month: String, year: Int) throws -> URL {
        let url = try outputURL("\(fileBase(month: month, year: year)).csv")
        try csvString(trips: trips).write(to: url, atomically: true, encoding: .utf8)
        return url
    }

    static func csvString(trips: [Trip]) -> String {
        func q(_ s: String) -> String { "\"\(s.replacingOccurrences(of: "\"", with: "\"\""))\"" }
        let headers = [
            "Dato", "Rute", "Kilometer", "Timer", "Minutter", "Har passasjerer", "Antall passasjerer",
            "Passasjer kilometer", "Andre utgifter", "Utgiftsbeskrivelse", "Kommentar",
        ]
        let dateFormatter = DateFormatter()
        dateFormatter.locale = nb
        dateFormatter.dateFormat = "dd.MM.yyyy"
        let rows = trips.sorted { $0.date < $1.date }.map { t in
            [
                dateFormatter.string(from: Format.parseDate(t.date)),
                q(t.route),
                Format.jsNumber(t.kilometers),
                String(t.hours),
                String(t.minutes),
                t.hasPassengers ? "Ja" : "Nei",
                String(t.passengerCount),
                Format.jsNumber(t.passengerKilometers),
                Format.jsNumber(t.otherExpenses),
                q(t.expenseDescription),
                q(t.comment),
            ].joined(separator: ",")
        }
        return "\u{FEFF}" + ([headers.joined(separator: ",")] + rows).joined(separator: "\n")
    }

    private static func outputURL(_ name: String) throws -> URL {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent("export", isDirectory: true)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let url = dir.appendingPathComponent(name)
        try? FileManager.default.removeItem(at: url)
        return url
    }
}

// MARK: - HTML → PDF

/// Rendrer HTML i en usynlig WKWebView og skriver den ut til A4-sider med UIPrintPageRenderer
/// (samme fremgangsmåte som expo-print bruker), slik at CSS-sideskift i malen respekteres.
@MainActor
private final class HTMLPDFRenderer: NSObject, WKNavigationDelegate {
    private var continuation: CheckedContinuation<Void, any Error>?
    private let webView: WKWebView

    private override init() {
        webView = WKWebView(frame: CGRect(x: 0, y: 0, width: 595, height: 842))
        super.init()
        webView.navigationDelegate = self
    }

    static func render(html: String, title: String) async throws -> Data {
        let renderer = HTMLPDFRenderer()
        try await withCheckedThrowingContinuation { (c: CheckedContinuation<Void, any Error>) in
            renderer.continuation = c
            renderer.webView.loadHTMLString(html, baseURL: nil)
        }
        return renderer.makePDF(title: title)
    }

    private func makePDF(title: String) -> Data {
        let printRenderer = UIPrintPageRenderer()
        printRenderer.addPrintFormatter(webView.viewPrintFormatter(), startingAtPageAt: 0)

        // A4 med 1,5 cm marg (som @page i malen)
        let paper = CGRect(x: 0, y: 0, width: 595.2, height: 841.8)
        let margin: CGFloat = 42.5
        printRenderer.setValue(paper, forKey: "paperRect")
        printRenderer.setValue(paper.insetBy(dx: margin, dy: margin), forKey: "printableRect")

        let data = NSMutableData()
        UIGraphicsBeginPDFContextToData(data, paper, [kCGPDFContextTitle as String: title])
        printRenderer.prepare(forDrawingPages: NSRange(location: 0, length: printRenderer.numberOfPages))
        let bounds = UIGraphicsGetPDFContextBounds()
        for page in 0..<printRenderer.numberOfPages {
            UIGraphicsBeginPDFPage()
            printRenderer.drawPage(at: page, in: bounds)
        }
        UIGraphicsEndPDFContext()
        return data as Data
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        continuation?.resume()
        continuation = nil
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: any Error) {
        continuation?.resume(throwing: error)
        continuation = nil
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: any Error) {
        continuation?.resume(throwing: error)
        continuation = nil
    }
}

// MARK: - Delingsark

struct ShareItem: Identifiable {
    let url: URL
    var id: URL { url }
}

struct ActivityView: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
