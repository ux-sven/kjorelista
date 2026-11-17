package no.svenway.kjorelista.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Samme typer som src/lib/haptic.ts
enum class Haptic { LIGHT, MEDIUM, SUCCESS, ERROR, WARNING }

class Haptics(private val feedback: HapticFeedback) {
    operator fun invoke(type: Haptic = Haptic.LIGHT) {
        feedback.performHapticFeedback(
            when (type) {
                Haptic.LIGHT -> HapticFeedbackType.SegmentTick
                Haptic.MEDIUM -> HapticFeedbackType.LongPress
                Haptic.SUCCESS -> HapticFeedbackType.Confirm
                Haptic.ERROR -> HapticFeedbackType.Reject
                Haptic.WARNING -> HapticFeedbackType.LongPress
            },
        )
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
