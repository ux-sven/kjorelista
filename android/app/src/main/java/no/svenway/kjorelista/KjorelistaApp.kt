package no.svenway.kjorelista

import android.app.Application
import android.webkit.WebView

class KjorelistaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // PDF-eksport tegner hele rapporten fra en WebView utenfor skjermen
        WebView.enableSlowWholeDocumentDraw()
        AppGraph.init(this)
    }
}
