import SwiftUI
import UIKit

/// Farger fra kjoreliste-app/src/lib/theme.tsx (som igjen er hentet fra webløsningen).
/// Hver farge har en lys og en mørk variant og følger `preferredColorScheme`.
enum AppColor {
    static let background = dynamic(0xF6F7FA, 0x0F172A)
    static let foreground = dynamic(0x2C3E50, 0xF8FAFC)
    static let card = dynamic(0xFFFFFF, 0x1E293B)
    static let primary = dynamic(0x3498DB, 0x60A5FA)
    static let primaryForeground = dynamic(0xFFFFFF, 0x0F172A)
    static let muted = dynamic(0xF1F2F6, 0x334155)
    static let mutedForeground = dynamic(0x5A6C7D, 0xCBD5E1)
    static let destructive = dynamic(0xC0392B, 0xEF4444)
    static let destructiveForeground = dynamic(0xFFFFFF, 0xF8FAFC)
    static let success = dynamic(0x27AE60, 0x10B981)
    static let warning = dynamic(0xD68910, 0xF59E0B)
    static let border = dynamic(0xE5E7EB, 0x475569)
    static let inputBackground = dynamic(0xF8F9FB, 0x0F172A)

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
