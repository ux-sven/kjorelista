import SwiftUI

/// Port av kjoreliste-app/src/components/MonthlySummary.tsx:
/// to nøkkeltall øverst, detaljer som rader under, milepæler som chips.
struct MonthlySummaryView: View {
    @Environment(AppSettings.self) private var settings

    let trips: [Trip]
    let allTrips: [Trip]
    let monthName: String
    let year: Int

    private struct Row: Identifiable {
        let icon: String
        let label: String
        let value: String
        var tint: Color?
        var id: String { label }
    }

    private struct Milestone: Identifiable {
        let icon: String
        let label: String
        var id: String { label }
    }

    var body: some View {
        let t = settings.t
        let str = settings.s
        let lang = settings.language
        let s = MonthStats(trips)
        let yearlyTrips = allTrips.inYear(year)
        let yearlyKm = yearlyTrips.reduce(0) { $0 + $1.kilometers }
        let tripWord: (Int) -> String = { $0 == 1 ? t.trips.trip : t.trips.tripsPlural }

        var rows: [Row] = []
        if yearlyKm > 0 {
            rows.append(Row(
                icon: "calendar",
                label: "\(t.summary.totalDrivenInYear) \(year)",
                value: "\(Format.number(yearlyKm, lang)) km · \(yearlyTrips.count) \(tripWord(yearlyTrips.count))",
                tint: AppColor.success
            ))
        }
        if s.tripsWithPassengers > 0 {
            rows.append(Row(
                icon: "person.2",
                label: t.summary.withPassengers,
                value: "\(s.tripsWithPassengers) \(tripWord(s.tripsWithPassengers)) · \(Format.number(s.totalPassengerKilometers, lang)) km"
            ))
        }
        if s.totalExpenses > 0 {
            rows.append(Row(icon: "creditcard", label: t.summary.expenses, value: "\(Format.number(s.totalExpenses, lang, maxDecimals: 0)) kr"))
        }

        var milestones: [Milestone] = []
        if s.totalKilometers > 1000 { milestones.append(Milestone(icon: "trophy", label: str.over1000km)) }
        if s.totalTimeInHours >= 50 { milestones.append(Milestone(icon: "clock", label: "\(Int(s.totalTimeInHours.rounded()))+ \(str.hoursWord)")) }
        if s.uniqueRoutes >= 5 { milestones.append(Milestone(icon: "map", label: "\(s.uniqueRoutes) \(str.routesWord)")) }
        if s.count >= 10 { milestones.append(Milestone(icon: "chart.bar", label: "\(s.count) \(t.trips.tripsPlural)")) }
        if yearlyKm > 5000 { milestones.append(Milestone(icon: "star", label: "\(Int((yearlyKm / 1000).rounded()))k+ km \(str.thisYear)")) }

        return VStack(spacing: 0) {
            if trips.isEmpty {
                VStack(spacing: 4) {
                    Image(systemName: "car")
                        .font(.system(size: 30))
                        .foregroundStyle(AppColor.mutedForeground)
                        .padding(.bottom, 2)
                    Text("\(t.summary.noTripsThisMonth) \(monthName.lowercased())")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(AppColor.foreground)
                        .multilineTextAlignment(.center)
                    Text(t.summary.addFirstTrip)
                        .font(.subheadline)
                        .foregroundStyle(AppColor.mutedForeground)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 22)
                .padding(.horizontal, 16)
            } else {
                HStack(alignment: .top, spacing: 0) {
                    Hero(
                        label: t.summary.totalDriven,
                        value: Format.number(s.totalKilometers, lang),
                        unit: "km",
                        sub: "⌀ \(Format.number(s.avgKmPerTrip, lang)) km \(str.perTrip)"
                    )
                    Rectangle().fill(AppColor.border).frame(width: 0.5).padding(.vertical, 4)
                    Hero(
                        label: t.summary.totalDriveTime,
                        value: Format.duration(s.totalTimeInHours, lang),
                        unit: nil,
                        sub: "⌀ \(Format.duration(s.avgTimePerTrip, lang)) \(str.perTrip)"
                    )
                }
                .padding(.vertical, 16)
            }

            ForEach(rows) { row in
                Separator()
                HStack(spacing: 12) {
                    Image(systemName: row.icon)
                        .foregroundStyle(row.tint ?? AppColor.primary)
                        .frame(width: 24)
                    Text(row.label)
                        .font(.subheadline)
                        .foregroundStyle(AppColor.foreground)
                    Spacer(minLength: 8)
                    Text(row.value)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(row.tint ?? AppColor.foreground)
                        .multilineTextAlignment(.trailing)
                        .layoutPriority(1)
                }
                .padding(.horizontal, 16)
                .frame(minHeight: 48)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(row.label): \(row.value)")
            }

            if !trips.isEmpty && !milestones.isEmpty {
                Separator()
                FlowLayout(spacing: 8) {
                    ForEach(milestones) { m in
                        Label {
                            Text(m.label).font(.footnote.weight(.medium)).foregroundStyle(AppColor.foreground)
                        } icon: {
                            Image(systemName: m.icon).font(.footnote).foregroundStyle(AppColor.primary)
                        }
                        .labelStyle(ChipLabelStyle())
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(AppColor.card))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).strokeBorder(AppColor.border, lineWidth: 0.5))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct Hero: View {
    let label: String
    let value: String
    let unit: String?
    let sub: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.footnote.weight(.medium))
                .foregroundStyle(AppColor.mutedForeground)
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(value)
                    .font(.title.bold())
                    .foregroundStyle(AppColor.foreground)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                if let unit {
                    Text(unit)
                        .font(.headline)
                        .foregroundStyle(AppColor.mutedForeground)
                }
            }
            Text(sub)
                .font(.footnote)
                .foregroundStyle(AppColor.mutedForeground)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label): \(value) \(unit ?? "")")
    }
}

private struct Separator: View {
    var body: some View {
        Rectangle().fill(AppColor.border).frame(height: 0.5).padding(.leading, 16)
    }
}

private struct ChipLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 5) {
            configuration.icon
            configuration.title
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(Capsule().fill(AppColor.muted))
    }
}

/// Enkel flytende layout for chips (bryter linjen når det ikke er plass).
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0, width: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > maxWidth {
                y += rowHeight + spacing
                x = 0
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            width = max(width, x - spacing)
        }
        return CGSize(width: width, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowHeight: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > bounds.minX && x + size.width > bounds.maxX {
                y += rowHeight + spacing
                x = bounds.minX
                rowHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
