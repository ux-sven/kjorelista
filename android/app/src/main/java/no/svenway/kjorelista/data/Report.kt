package no.svenway.kjorelista.data

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Port av src/lib/report.ts. Rapport-HTML er kopiert fra webløsningen slik at PDF-en ser lik ut
 * på nett og mobil. Rapporten er alltid på norsk, som i webløsningen.
 */
object Report {
    private val nb = Language.NB

    fun escapeHtml(s: String) =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun num(n: Double) = Format.number(n, nb)
    private fun nok(n: Double) = Format.currency(n, nb.locale)

    fun generateHtml(trips: List<Trip>, month: String, year: Int, profile: UserProfile?): String {
        val totalKilometers = trips.sumOf { it.kilometers }
        val totalHours = trips.sumOf { it.hours }
        val totalMinutes = trips.sumOf { it.minutes }
        val totalTimeInHours = totalHours + totalMinutes / 60.0
        val totalExpenses = trips.sumOf { it.otherExpenses }
        val totalPassengerKilometers = trips.sumOf { it.passengerKilometers }
        val tripsWithPassengers = trips.count { it.hasPassengers }

        fun formatTime(total: Double): String {
            val hours = kotlin.math.floor(total).toInt()
            val minutes = Math.round((total - hours) * 60).toInt()
            return when {
                hours == 0 -> "${minutes}m"
                minutes == 0 -> "${hours}t"
                else -> "${hours}t ${minutes}m"
            }
        }

        fun formatTimeForTable(hours: Int, minutes: Int): String {
            val total = hours * 60 + minutes
            val h = total / 60
            val m = total % 60
            return when {
                h == 0 -> "${m}m"
                m == 0 -> "${h}t"
                else -> "${h}t ${m}m"
            }
        }

        val rows = trips.joinToString("") { trip ->
            val date = Format.longDate(trip.date, nb)
            val time = if (trip.hours > 0 || trip.minutes > 0) "${trip.hours}t ${trip.minutes}m" else "-"
            val passengers = if (trip.hasPassengers) trip.passengerCount.toString() else "-"
            val passengerKm = if (trip.hasPassengers && trip.passengerKilometers > 0) num(trip.passengerKilometers) else "-"
            val expenses = if (trip.otherExpenses > 0) nok(trip.otherExpenses) else "-"
            val expenseDesc = if (trip.expenseDescription.isNotBlank()) escapeHtml(trip.expenseDescription) else "-"
            var html = """
                <tr>
                    <td class="date-cell">$date</td>
                    <td class="route-cell">${escapeHtml(trip.route)}</td>
                    <td class="number-cell">${num(trip.kilometers)}</td>
                    <td class="number-cell">$time</td>
                    <td class="number-cell">$passengers</td>
                    <td class="number-cell">$passengerKm</td>
                    <td class="number-cell">$expenses</td>
                    <td class="expense-cell">$expenseDesc</td>
                </tr>"""
            if (trip.comment.isNotBlank()) {
                html += """
                    <tr class="comment-row">
                        <td colspan="8">
                            <span class="comment-label">Kommentar:</span>
                            ${escapeHtml(trip.comment)}
                        </td>
                    </tr>"""
            }
            html
        }

        val table = if (trips.isNotEmpty()) """
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
            $rows

            <!-- Summary row -->
            <tr class="summary-row">
                <td class="summary-label">TOTALT</td>
                <td class="summary-label">${trips.size} ${if (trips.size == 1) "tur" else "turer"}</td>
                <td class="number-cell summary-label">${num(totalKilometers)} km</td>
                <td class="number-cell summary-label">${formatTimeForTable(totalHours, totalMinutes)}</td>
                <td class="number-cell summary-label">$tripsWithPassengers</td>
                <td class="number-cell summary-label">${num(totalPassengerKilometers)} km</td>
                <td class="number-cell summary-label">${if (totalExpenses > 0) nok(totalExpenses) else "-"}</td>
                <td class="summary-label">-</td>
            </tr>
        </tbody>
    </table>
    """ else """
    <div class="no-trips">
        <strong>Ingen kjøreturer registrert</strong><br>
        Det er ikke registrert noen kjøreturer for denne måneden.
    </div>
    """

        val now = LocalDateTime.now()
        val generated = "${now.format(DateTimeFormatter.ofPattern("d.M.yyyy"))} kl. ${now.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        val userInfo = profile?.let {
            """<div class="user-info">Bruker: ${escapeHtml(it.firstName)} ${escapeHtml(it.lastName)}</div>"""
        } ?: ""
        val title = escapeHtml("$month $year")

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>Kjørerapport - $title</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
            margin: 0;
            padding: 20px;
            line-height: 1.4;
            font-size: 13px;
            color: #1a1a1a;
        }
        
        .page-header {
            text-align: center;
            margin-bottom: 40px;
            border-bottom: 3px solid #2563eb;
            padding-bottom: 25px;
        }
        
        .page-header h1 {
            margin: 0 0 12px 0;
            color: #1e293b;
            font-size: 28px;
            font-weight: 600;
        }
        
        .page-header h2 {
            margin: 0 0 8px 0;
            color: #475569;
            font-size: 20px;
            font-weight: 500;
        }
        
        .page-header .user-info {
            color: #64748b;
            font-size: 14px;
            margin-top: 8px;
        }

        /* Table styles */
        .trips-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 30px;
            background: white;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        }
        
        .trips-table th {
            background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
            color: #334155;
            font-weight: 600;
            padding: 12px 8px;
            text-align: left;
            border-bottom: 2px solid #cbd5e1;
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        .trips-table td {
            padding: 10px 8px;
            border-bottom: 1px solid #f1f5f9;
            vertical-align: top;
            font-size: 12px;
        }
        
        .trips-table tbody tr {
            transition: background-color 0.2s ease;
        }
        
        .trips-table tbody tr:nth-child(even) {
            background-color: #f8fafc;
        }
        
        .trips-table tbody tr:hover {
            background-color: #f1f5f9;
        }
        
        /* Summary row styles */
        .summary-row {
            background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
            border-top: 2px solid #cbd5e1 !important;
            font-weight: 600 !important;
        }
        
        .summary-row td {
            padding: 12px 8px !important;
            border-bottom: 2px solid #cbd5e1 !important;
            font-size: 12px !important;
            color: #374151 !important;
        }
        
        .summary-label {
            font-weight: 600 !important;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        /* Specific column styles */
        .date-cell {
            font-weight: 500;
            color: #475569;
            white-space: nowrap;
            min-width: 85px;
        }
        
        .route-cell {
            max-width: 160px;
            word-wrap: break-word;
            font-weight: 500;
            color: #1e293b;
        }
        
        .number-cell {
            text-align: right;
            font-family: 'SF Mono', 'Monaco', 'Cascadia Code', monospace;
            font-weight: 500;
            color: #374151;
        }
        
        .expense-cell {
            font-size: 11px;
            max-width: 100px;
            word-wrap: break-word;
            color: #6b7280;
        }
        
        /* Comment row styles */
        .comment-row {
            background-color: #f8fafc !important;
            border-top: none !important;
        }
        
        .comment-row td {
            padding: 8px 12px 12px 12px;
            border-bottom: 2px solid #e2e8f0;
            font-style: italic;
            color: #64748b;
            font-size: 11px;
            line-height: 1.4;
        }
        
        .comment-label {
            font-weight: 600;
            color: #475569;
            margin-right: 6px;
        }

        /* Page break for summary */
        .summary-page {
            page-break-before: always;
            margin-top: 40px;
        }
        
        .summary {
            background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
            padding: 30px;
            border-radius: 12px;
            border: 2px solid #e2e8f0;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
        }
        
        .summary h3 {
            margin: 0 0 25px 0;
            color: #1e293b;
            font-size: 22px;
            font-weight: 600;
            text-align: center;
            border-bottom: 2px solid #2563eb;
            padding-bottom: 12px;
        }
        
        .summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
        }
        
        .summary-item {
            background: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            border: 1px solid #e2e8f0;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        }
        
        .summary-value {
            font-size: 24px;
            font-weight: 700;
            color: #2563eb;
            margin-bottom: 8px;
            font-family: 'SF Mono', 'Monaco', 'Cascadia Code', monospace;
        }
        
        .summary-label {
            font-size: 13px;
            color: #64748b;
            font-weight: 500;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .footer {
            margin-top: 50px;
            text-align: center;
            font-size: 11px;
            color: #94a3b8;
            border-top: 1px solid #e2e8f0;
            padding-top: 20px;
        }
        
        .no-trips {
            text-align: center;
            padding: 60px 20px;
            color: #64748b;
            font-size: 16px;
            background: #f8fafc;
            border-radius: 8px;
            border: 2px dashed #cbd5e1;
        }

        /* Print optimizations */
        @media print {
            body { 
                margin: 0; 
                padding: 15px;
                font-size: 11px;
                -webkit-print-color-adjust: exact;
                color-adjust: exact;
            }
            
            .page-header h1 { font-size: 24px; }
            .page-header h2 { font-size: 18px; }
            
            .trips-table th { 
                font-size: 9px; 
                padding: 8px 6px;
            }
            
            .trips-table td { 
                font-size: 10px; 
                padding: 6px 4px;
            }
            
            .summary-row td {
                font-size: 10px !important;
                padding: 10px 6px !important;
            }
            
            .comment-row td {
                font-size: 9px;
                padding: 6px 8px 8px 8px;
            }
            
            .summary {
                padding: 20px;
                margin-top: 30px;
            }
            
            .summary h3 { font-size: 18px; }
            .summary-value { font-size: 20px; }
            .summary-label { font-size: 11px; }
            
            .no-print { display: none; }
        }
        
        /* Mobile and iOS Safari specific optimizations */
        @media screen and (max-width: 768px) {
            body {
                font-size: 12px;
                line-height: 1.3;
            }
            
            .page-header h1 { font-size: 22px; }
            .page-header h2 { font-size: 16px; }
            
            .trips-table {
                font-size: 11px;
            }
            
            .trips-table th {
                padding: 8px 4px;
                font-size: 10px;
            }
            
            .trips-table td {
                padding: 6px 4px;
                font-size: 11px;
            }
            
            .summary {
                padding: 16px;
            }
            
            .summary-grid {
                grid-template-columns: repeat(2, 1fr);
                gap: 12px;
            }
            
            .summary-item {
                padding: 12px;
            }
            
            .summary-value {
                font-size: 18px;
            }
        }
        
        @page {
            margin: 1.5cm;
            size: A4;
        }
    </style>
</head>
<body>
    <div class="page-header">
        <h1>Kjørerapport</h1>
        <h2>$title</h2>
        $userInfo
    </div>

    $table

    <!-- Summary on separate page -->
    <div class="summary-page">
        <div class="summary">
            <h3>Sammendrag $title</h3>
            <div class="summary-grid">
                <div class="summary-item">
                    <div class="summary-value">${trips.size}</div>
                    <div class="summary-label">Antall turer</div>
                </div>
                <div class="summary-item">
                    <div class="summary-value">${num(totalKilometers)}</div>
                    <div class="summary-label">Total kilometer</div>
                </div>
                <div class="summary-item">
                    <div class="summary-value">${formatTime(totalTimeInHours)}</div>
                    <div class="summary-label">Total kjøretid</div>
                </div>
                <div class="summary-item">
                    <div class="summary-value">$tripsWithPassengers</div>
                    <div class="summary-label">Turer med passasjerer</div>
                </div>
                <div class="summary-item">
                    <div class="summary-value">${num(totalPassengerKilometers)}</div>
                    <div class="summary-label">Kilometer med passasjerer</div>
                </div>
                <div class="summary-item">
                    <div class="summary-value">${nok(totalExpenses)}</div>
                    <div class="summary-label">Andre utgifter</div>
                </div>
            </div>
        </div>
    </div>

    <div class="footer">
        <p><strong>Kjøreliste App</strong> • Generert $generated</p>
    </div>
</body>
</html>"""
    }

    /** JavaScript-lik tallformatering for CSV: 128 → «128», 42.5 → «42.5» */
    private fun jsNumber(n: Double): String = if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()

    fun generateCsv(trips: List<Trip>): String {
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val headers = listOf(
            "Dato", "Rute", "Kilometer", "Timer", "Minutter", "Har passasjerer", "Antall passasjerer",
            "Passasjer kilometer", "Andre utgifter", "Utgiftsbeskrivelse", "Kommentar",
        )
        val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val rows = trips.sortedBy { it.date }.map { t ->
            listOf(
                Format.parseDate(t.date).format(dateFormat),
                q(t.route),
                jsNumber(t.kilometers),
                t.hours.toString(),
                t.minutes.toString(),
                if (t.hasPassengers) "Ja" else "Nei",
                t.passengerCount.toString(),
                jsNumber(t.passengerKilometers),
                jsNumber(t.otherExpenses),
                q(t.expenseDescription),
                q(t.comment),
            ).joinToString(",")
        }
        return "﻿" + (listOf(headers.joinToString(",")) + rows).joinToString("\n")
    }

    fun fileBase(month: String, year: Int) =
        "kjorerapport-${month.lowercase().replace(Regex("[^a-z0-9æøå]"), "")}-$year"

    private fun exportDir(context: Context) = File(context.cacheDir, "exports").apply { mkdirs() }

    suspend fun shareCsv(context: Context, trips: List<Trip>, month: String, year: Int) {
        val file = File(exportDir(context), "${fileBase(month, year)}.csv")
        withContext(Dispatchers.IO) { file.writeText(generateCsv(trips), Charsets.UTF_8) }
        share(context, file, "text/csv", "$month $year")
    }

    suspend fun sharePdf(context: Context, trips: List<Trip>, month: String, year: Int, profile: UserProfile?) {
        val sorted = trips.sortedBy { it.date }
        val file = File(exportDir(context), "${fileBase(month, year)}.pdf")
        renderPdf(context, generateHtml(sorted, month, year, profile), file)
        share(context, file, "application/pdf", "$month $year")
    }

    /**
     * Tegner HTML-en i en WebView utenfor skjermen og skriver den til A4-sider med PdfDocument.
     * Sidene deles bare mellom tabellrader og seksjoner (posisjonene måles med JavaScript), og
     * «page-break-before» på sammendraget respekteres. Krever WebView.enableSlowWholeDocumentDraw()
     * (kalles i KjorelistaApp).
     */
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun renderPdf(context: Context, html: String, out: File) = suspendCancellableCoroutine { cont ->
        val density = context.resources.displayMetrics.density
        val widthPx = (CSS_PAGE_WIDTH * density).toInt()
        // CSS-piksler -> PDF-punkter innenfor margene
        val scale = (PAGE_WIDTH_PT - 2 * PAGE_MARGIN_PT).toFloat() / CSS_PAGE_WIDTH
        val pageContentCss = (PAGE_HEIGHT_PT - 2 * PAGE_MARGIN_PT) / scale
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.setBackgroundColor(android.graphics.Color.WHITE)
        // View.post på en WebView som ikke er festet til et vindu kjøres aldri; bruk en egen Handler
        val handler = Handler(Looper.getMainLooper())
        cont.invokeOnCancellation { handler.post { webView.destroy() } }

        fun layout(height: Int) {
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            webView.layout(0, 0, widthPx, height)
        }

        fun finish(view: WebView, measurement: String) {
            try {
                val m = parseMeasurement(measurement)
                layout(maxOf((m.height * density).toInt(), (pageContentCss * density).toInt()))
                val pdf = PdfDocument()
                // Tegner utsnittet [from, to) av dokumentet med toppen ved y = at (alt i CSS-piksler)
                fun drawSlice(canvas: android.graphics.Canvas, from: Float, to: Float, at: Float) {
                    canvas.save()
                    canvas.translate(0f, (at - from) * density)
                    canvas.clipRect(0f, from * density, widthPx.toFloat(), to * density)
                    view.draw(canvas)
                    canvas.restore()
                }
                paginate(m, pageContentCss).forEachIndexed { i, slice ->
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, i + 1).create())
                    val canvas = page.canvas
                    canvas.translate(PAGE_MARGIN_PT.toFloat(), PAGE_MARGIN_PT.toFloat())
                    canvas.scale(scale / density, scale / density)
                    var y = 0f
                    if (slice.repeatHead) {
                        drawSlice(canvas, m.headTop, m.headBottom, 0f)
                        y = m.headHeight
                    }
                    drawSlice(canvas, slice.start, slice.end, y)
                    pdf.finishPage(page)
                }
                out.outputStream().use { pdf.writeTo(it) }
                pdf.close()
                cont.resume(Unit)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            } finally {
                view.destroy()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                // Gi WebView litt tid til å legge ut innholdet før vi måler
                handler.postDelayed({
                    if (!cont.isActive) return@postDelayed
                    view.evaluateJavascript(MEASURE_JS) { result ->
                        if (cont.isActive) finish(view, result)
                    }
                }, 400)
            }
        }
        layout((pageContentCss * density).toInt())
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private class Measurement(
        val height: Float,
        val breaks: List<Float>,
        val forced: List<Float>,
        /** Tabellhodet [top, bottom) og hvor tabellkroppen slutter; gjentas øverst på nye sider inne i tabellen. */
        val headTop: Float,
        val headBottom: Float,
        val bodyBottom: Float,
    ) {
        val headHeight get() = headBottom - headTop
        fun insideTable(y: Float) = headHeight > 0 && y > headBottom + 1f && y < bodyBottom - 1f
    }

    private class PageSlice(val start: Float, val end: Float, val repeatHead: Boolean)

    private fun parseMeasurement(json: String): Measurement {
        // evaluateJavascript returnerer en JSON-streng som selv er JSON-kodet
        val inner = org.json.JSONTokener(json).nextValue() as String
        val o = org.json.JSONObject(inner)
        fun list(key: String) = o.getJSONArray(key).let { a -> List(a.length()) { a.getDouble(it).toFloat() } }
        return Measurement(
            o.getDouble("height").toFloat(), list("breaks").sorted(), list("forced").sorted(),
            o.getDouble("headTop").toFloat(), o.getDouble("headBottom").toFloat(), o.getDouble("bodyBottom").toFloat(),
        )
    }

    /** Deler innholdet i sider [start, end) i CSS-piksler, bare ved tillatte bruddpunkter. */
    private fun paginate(m: Measurement, pageHeight: Float): List<PageSlice> {
        val pages = mutableListOf<PageSlice>()
        var start = 0f
        while (start < m.height - 1f) {
            // Starter siden midt i tabellen, gjentas tabellhodet øverst og tar plass fra siden
            val repeatHead = m.insideTable(start)
            val limit = start + pageHeight - (if (repeatHead) m.headHeight else 0f)
            val forced = m.forced.firstOrNull { it > start + 1f && it <= limit }
            val end = when {
                forced != null -> forced
                limit >= m.height -> m.height
                else -> m.breaks.lastOrNull { it > start + 1f && it <= limit } ?: limit
            }
            pages += PageSlice(start, end, repeatHead)
            start = end
        }
        return pages
    }

    private fun share(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TITLE, file.name)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title))
    }

    // A4 i punkter (1/72 tomme) og CSS-piksler (96 dpi)
    private const val PAGE_WIDTH_PT = 595
    private const val PAGE_HEIGHT_PT = 842
    private const val CSS_PAGE_WIDTH = 794
    private const val PAGE_MARGIN_PT = 36

    // Tillatte sidebrudd: før hver tabellrad (ikke kommentarrader, de hører til raden over),
    // før seksjoner, og etter tabellen. Tvungne brudd: elementer med page-break-before.
    private val MEASURE_JS = """
        (function() {
          var y = function(el) { return el.getBoundingClientRect().top + window.scrollY; };
          var bottom = function(el) { return el.getBoundingClientRect().bottom + window.scrollY; };
          var breaks = [];
          document.querySelectorAll('tbody tr:not(.comment-row), .footer, .no-trips').forEach(function(el) { breaks.push(y(el)); });
          document.querySelectorAll('table').forEach(function(el) { breaks.push(bottom(el)); });
          var head = document.querySelector('table thead');
          var body = document.querySelector('table tbody');
          var forced = [];
          document.querySelectorAll('.summary-page').forEach(function(el) { forced.push(y(el)); });
          return JSON.stringify({
            height: document.documentElement.scrollHeight, breaks: breaks, forced: forced,
            headTop: head ? y(head) : 0, headBottom: head ? bottom(head) : 0, bodyBottom: body ? bottom(body) : 0
          });
        })();
    """.trimIndent()
}
