import SwiftUI
import UIKit

/// Fargeprofil fra Kjørelista-logoen: lilla gradient (#63517B → #7E6F91) og gull (#DBC388).
/// Lilla er hovedfarge i lys modus, gull i mørk modus. Hver farge følger `preferredColorScheme`.
enum AppColor {
    static let background = dynamic(0xF5F3F8, 0x1C1726)
    static let foreground = dynamic(0x2E2540, 0xF5F2F9)
    static let card = dynamic(0xFFFFFF, 0x2A2337)
    static let primary = dynamic(0x5A4873, 0xDBC388)
    static let primaryForeground = dynamic(0xFFFFFF, 0x2E2540)
    static let muted = dynamic(0xEEEAF3, 0x3A3148)
    static let mutedForeground = dynamic(0x6B6280, 0xC3BBD0)
    static let destructive = dynamic(0xC0392B, 0xEF4444)
    static let destructiveForeground = dynamic(0xFFFFFF, 0xF8FAFC)
    static let success = dynamic(0x27AE60, 0x10B981)
    static let warning = dynamic(0xD68910, 0xF59E0B)
    static let border = dynamic(0xE3DEEA, 0x4A4058)
    static let inputBackground = dynamic(0xF8F6FA, 0x1C1726)

    /// Merkevarefarger (like i lys og mørk modus)
    static let gold = Color(uiColor: UIColor(hex: 0xDBC388))
    static let onGold = Color(uiColor: UIColor(hex: 0x3D3152))
    static let brandPurpleLight = Color(uiColor: UIColor(hex: 0x7E6F91))
    static let brandPurple = Color(uiColor: UIColor(hex: 0x63517B))
    static let brandPurpleDark = Color(uiColor: UIColor(hex: 0x4E3F66))

    /// Lilla gradient som i logoen
    static let brandGradient = LinearGradient(
        colors: [brandPurpleLight, brandPurpleDark],
        startPoint: .topTrailing,
        endPoint: .bottomLeading
    )

    private static func dynamic(_ light: UInt32, _ dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}

enum ThemePreference: String, CaseIterable, Sendable {
    case system, light, dark

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

extension View {
    /// Gruppert liste med appens bakgrunns- og kortfarger.
    func themedList() -> some View {
        scrollContentBackground(.hidden)
            .background(AppColor.background)
    }
}
