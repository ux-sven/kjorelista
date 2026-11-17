package no.svenway.kjorelista.data

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

// Port av kjoreliste-app/src/lib/format.ts

enum class Language(val tag: String) {
    NB("nb"), EN("en");

    /** nb_NO / en_GB som i Expo-appen */
    val locale: Locale get() = if (this == NB) Locale.forLanguageTag("nb-NO") else Locale.forLanguageTag("en-GB")

    companion object {
        fun fromLocale(locale: Locale?): Language =
            when (locale?.language?.lowercase()) {
                "nb", "no", "nn" -> NB
                "en" -> EN
                null, "" -> NB
                else -> EN
            }
    }
}

object Format {
    /** «YYYY-MM-DD» tolkes som lokal dato, aldri UTC */
    fun parseDate(date: String): LocalDate = LocalDate.parse(date)

    fun toDateString(date: LocalDate): String = date.toString()

    private fun String.capitalized() = replaceFirstChar { it.titlecase(Locale.ROOT) }

    /** «Tir 14. okt.» / «Tue 14 Oct» */
    fun shortDateWithWeekday(date: String, language: Language): String {
        val d = parseDate(date)
        val weekday = d.dayOfWeek.getDisplayName(TextStyle.SHORT, language.locale).replace(".", "")
        val pattern = if (language == Language.NB) "d. MMM" else "d MMM"
        val dayMonth = d.format(DateTimeFormatter.ofPattern(pattern, language.locale))
        return "${weekday.capitalized()} $dayMonth"
    }

    /** «Tirsdag 14. oktober 2026» / «Tuesday 14 October 2026» */
    fun longDate(date: String, language: Language): String {
        val d = parseDate(date)
        val weekday = d.dayOfWeek.getDisplayName(TextStyle.FULL, language.locale)
        val pattern = if (language == Language.NB) "d. MMMM yyyy" else "d MMMM yyyy"
        return "${weekday.capitalized()} ${d.format(DateTimeFormatter.ofPattern(pattern, language.locale))}"
    }

    fun number(n: Double, language: Language, maxDecimals: Int = 1): String =
        NumberFormat.getNumberInstance(language.locale).apply {
            maximumFractionDigits = maxDecimals
            minimumFractionDigits = 0
        }.format(n)

    fun currency(n: Double, locale: Locale): String =
        NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance("NOK") }.format(n)

    /** Klokkeformat for én tur: «01:30» */
    fun clock(hours: Int, minutes: Int): String =
        if (hours == 0 && minutes == 0) "-" else "%02d:%02d".format(hours, minutes)

    /** Varighet fra desimaltimer: «3t 15m» */
    fun duration(totalHours: Double, language: Language): String {
        var h = floor(totalHours).toInt()
        var m = ((totalHours - h) * 60).roundToInt()
        if (m == 60) { h += 1; m = 0 }
        val hu = if (language == Language.NB) "t" else "h"
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "$h$hu"
            else -> "$h$hu ${m}m"
        }
    }

    fun tripsInMonth(trips: List<Trip>, year: Int, month: Int): List<Trip> =
        trips.filter { val d = parseDate(it.date); d.year == year && d.monthValue == month }
}

data class MonthStats(
    val count: Int,
    val totalKilometers: Double,
    val totalHours: Int,
    val totalMinutes: Int,
    val totalTimeInHours: Double,
    val totalExpenses: Double,
    val tripsWithPassengers: Int,
    val totalPassengerKilometers: Double,
    val uniqueRoutes: Int,
    val avgKmPerTrip: Double,
    val avgTimePerTrip: Double,
) {
    companion object {
        fun of(trips: List<Trip>): MonthStats {
            val km = trips.sumOf { it.kilometers }
            val hours = trips.sumOf { it.hours }
            val minutes = trips.sumOf { it.minutes }
            val time = hours + minutes / 60.0
            return MonthStats(
                count = trips.size,
                totalKilometers = km,
                totalHours = hours,
                totalMinutes = minutes,
                totalTimeInHours = time,
                totalExpenses = trips.sumOf { it.otherExpenses },
                tripsWithPassengers = trips.count { it.hasPassengers },
                totalPassengerKilometers = trips.sumOf { it.passengerKilometers },
                uniqueRoutes = trips.map { it.route.lowercase() }.toSet().size,
                avgKmPerTrip = if (trips.isEmpty()) 0.0 else km / trips.size,
                avgTimePerTrip = if (trips.isEmpty()) 0.0 else time / trips.size,
            )
        }
    }
}
