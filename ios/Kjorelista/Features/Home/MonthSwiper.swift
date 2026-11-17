import SwiftUI
import UIKit

/// Port av kjoreliste-app/src/components/MonthSwiper.tsx.
/// Sveip til venstre = neste måned, til høyre = forrige måned. Den gamle måneden glir ut,
/// den nye glir inn fra motsatt side. Under boksen: «‹ Sveip for å bytte måned ›».
struct MonthSwiper<Content: View>: View {
    @Environment(AppSettings.self) private var settings

    let label: String
    let onChange: (_ direction: Int) -> Void
    @ViewBuilder let content: Content

    @State private var offset: CGFloat = 0
    @State private var width: CGFloat = 390

    private let swipeDistance: CGFloat = 70
    private let swipeVelocity: CGFloat = 600

    var body: some View {
        VStack(spacing: 6) {
            content
                .offset(x: offset)
                .opacity(1 - 0.8 * min(abs(offset) / max(width, 1), 1))
                .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { width = $0 }
                .gesture(HorizontalPan(onChanged: { offset = $0 }, onEnded: ended))
                .accessibilityElement(children: .combine)
                .accessibilityLabel(label)
                .accessibilityHint(settings.s.swipeMonthHint)
                .accessibilityAdjustableAction { direction in
                    switch direction {
                    case .increment: change(1)
                    case .decrement: change(-1)
                    @unknown default: break
                    }
                }

            HStack(spacing: 10) {
                Button { slide(-1) } label: {
                    Image(systemName: "chevron.left").font(.footnote.weight(.semibold)).padding(4)
                }
                .accessibilityLabel(settings.t.navigation.previousMonth)
                Text(settings.s.swipeMonthHint)
                    .font(.footnote)
                Button { slide(1) } label: {
                    Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).padding(4)
                }
                .accessibilityLabel(settings.t.navigation.nextMonth)
            }
            .buttonStyle(.plain)
            .foregroundStyle(AppColor.mutedForeground)
            .frame(maxWidth: .infinity)
            .accessibilityHidden(true)
        }
    }

    private func change(_ direction: Int) {
        Haptic.light.play()
        onChange(direction)
    }

    private func ended(translation: CGFloat, velocity: CGFloat) {
        if translation < -swipeDistance || velocity < -swipeVelocity {
            slide(1)
        } else if translation > swipeDistance || velocity > swipeVelocity {
            slide(-1)
        } else {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) { offset = 0 }
        }
    }

    /// Skyv ut, bytt måned, og la den nye måneden gli inn fra motsatt side.
    private func slide(_ direction: Int) {
        let d = CGFloat(direction)
        withAnimation(.easeIn(duration: 0.16)) {
            offset = -d * width
        } completion: {
            change(direction)
            var t = Transaction()
            t.disablesAnimations = true
            withTransaction(t) { offset = d * width * 0.6 }
            withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) { offset = 0 }
        }
    }
}

/// Vannrett panorering som bare starter når bevegelsen er mer vannrett enn loddrett,
/// slik at loddrett scrolling i listen fungerer som normalt.
private struct HorizontalPan: UIGestureRecognizerRepresentable {
    let onChanged: (CGFloat) -> Void
    let onEnded: (_ translation: CGFloat, _ velocity: CGFloat) -> Void

    func makeCoordinator(converter: CoordinateSpaceConverter) -> Coordinator { Coordinator() }

    func makeUIGestureRecognizer(context: Context) -> UIPanGestureRecognizer {
        let pan = UIPanGestureRecognizer()
        pan.delegate = context.coordinator
        return pan
    }

    func handleUIGestureRecognizerAction(_ recognizer: UIPanGestureRecognizer, context: Context) {
        let view = recognizer.view
        let translation = recognizer.translation(in: view).x
        switch recognizer.state {
        case .changed:
            onChanged(translation)
        case .ended:
            onEnded(translation, recognizer.velocity(in: view).x)
        case .cancelled, .failed:
            onEnded(0, 0)
        default:
            break
        }
    }

    @MainActor final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            guard let pan = gestureRecognizer as? UIPanGestureRecognizer else { return true }
            let v = pan.velocity(in: pan.view)
            return abs(v.x) > abs(v.y) * 1.2
        }

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer
        ) -> Bool {
            false
        }
    }
}
