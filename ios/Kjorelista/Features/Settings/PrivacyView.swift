import SwiftUI

/// Port av kjoreliste-app/src/app/privacy.tsx (samme innhold som PrivacyPolicy i webløsningen).
struct PrivacyView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.openURL) private var openURL

    var body: some View {
        let p = settings.t.privacy
        let sections: [(String, String)] = [
            (p.dataWhatWeCollect, p.dataWhatWeCollectContent),
            (p.dataHowWeUse, p.dataHowWeUseContent),
            (p.dataStorage, p.dataStorageContent),
            (p.dataSharing, p.dataSharingContent),
            (p.userRights, p.userRightsContent),
            (p.dataSecurity, p.dataSecurityContent),
            (p.cookies, p.cookiesContent),
            (p.changes, p.changesContent),
        ]

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(p.lastUpdated)
                    .font(.footnote)
                    .foregroundStyle(AppColor.mutedForeground)
                Text(p.introduction)
                    .font(.subheadline)
                    .foregroundStyle(AppColor.foreground)

                VStack(alignment: .leading, spacing: 8) {
                    heading(p.dataController)
                    Text("svenway").font(.subheadline).foregroundStyle(AppColor.mutedForeground)
                    mail
                }
                .padding(.top, 16)

                ForEach(sections, id: \.0) { title, content in
                    VStack(alignment: .leading, spacing: 8) {
                        Divider().overlay(AppColor.border).padding(.bottom, 8)
                        heading(title)
                        Text(content)
                            .font(.subheadline)
                            .foregroundStyle(AppColor.mutedForeground)
                            .lineSpacing(4)
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Divider().overlay(AppColor.border).padding(.bottom, 8)
                    heading(p.contact)
                    Text(p.contactContent).font(.subheadline).foregroundStyle(AppColor.mutedForeground)
                    mail
                }
            }
            .padding(20)
            .padding(.bottom, 28)
            .frame(maxWidth: 720, alignment: .leading)
            .frame(maxWidth: .infinity)
        }
        .background(AppColor.background.ignoresSafeArea())
        .navigationTitle(p.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    private func heading(_ text: String) -> some View {
        Text(text)
            .font(.body.weight(.semibold))
            .foregroundStyle(AppColor.foreground)
            .accessibilityAddTraits(.isHeader)
    }

    private var mail: some View {
        Button {
            if let url = URL(string: "mailto:\(Config.contactEmail)") { openURL(url) }
        } label: {
            Label(Config.contactEmail, systemImage: "envelope")
                .font(.subheadline)
                .foregroundStyle(AppColor.primary)
        }
        .buttonStyle(.plain)
        .padding(.vertical, 4)
        .accessibilityAddTraits(.isLink)
    }
}
