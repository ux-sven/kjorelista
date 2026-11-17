import SwiftUI

/// Port av kjoreliste-app/src/components/TripRow.tsx (innholdet i én rad).
struct TripRowView: View {
    @Environment(AppSettings.self) private var settings
    let trip: Trip

    var body: some View {
        let lang = settings.language
        let date = Format.shortDateWithWeekday(trip.date, lang)
        let hasTime = trip.hours > 0 || trip.minutes > 0

        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(date)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(AppColor.foreground)
                    Text(trip.route)
                        .foregroundStyle(AppColor.foreground)
                        .lineLimit(2)
                    if trip.hasPassengers || trip.otherExpenses > 0 {
                        HStack(spacing: 14) {
                            if trip.hasPassengers {
                                meta("person.2", "\(trip.passengerCount)" + (trip.passengerKilometers > 0 ? " • \(Format.number(trip.passengerKilometers, lang)) km" : ""))
                            }
                            if trip.otherExpenses > 0 {
                                meta("creditcard", "\(Format.number(trip.otherExpenses, lang, maxDecimals: 0)) kr")
                            }
                        }
                        .padding(.top, 2)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .trailing, spacing: 6) {
                    Text("\(Format.number(trip.kilometers, lang)) km")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppColor.primary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(RoundedRectangle(cornerRadius: 8).fill(AppColor.primary.opacity(0.1)))
                        .overlay(RoundedRectangle(cornerRadius: 8).strokeBorder(AppColor.primary.opacity(0.3), lineWidth: 1))
                        .fixedSize()
                    if hasTime {
                        meta("clock", Format.clock(hours: trip.hours, minutes: trip.minutes))
                    }
                }
            }

            if !trip.comment.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "text.bubble")
                        .font(.footnote)
                        .foregroundStyle(AppColor.warning)
                        .padding(.top, 2)
                    Text(trip.comment)
                        .font(.subheadline)
                        .foregroundStyle(AppColor.foreground)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(10)
                .background(RoundedRectangle(cornerRadius: 10).fill(AppColor.warning.opacity(0.15)))
                .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(AppColor.warning.opacity(0.4), lineWidth: 1))
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
    }

    private func meta(_ icon: String, _ text: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon).font(.footnote)
            Text(text).font(.subheadline)
        }
        .foregroundStyle(AppColor.mutedForeground)
    }
}
