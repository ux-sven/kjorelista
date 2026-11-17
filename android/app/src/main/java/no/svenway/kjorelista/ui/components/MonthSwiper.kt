package no.svenway.kjorelista.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.svenway.kjorelista.R
import no.svenway.kjorelista.ui.Haptic
import no.svenway.kjorelista.ui.rememberHaptics
import no.svenway.kjorelista.ui.theme.LocalAppColors
import kotlin.math.abs

private val SWIPE_DISTANCE = 70.dp
private const val SWIPE_VELOCITY_DP = 600f

/**
 * Sveip til venstre = neste måned, til høyre = forrige måned (port av MonthSwiper.tsx).
 * Skjermlesere får «Forrige/Neste måned» som egendefinerte handlinger.
 */
@Composable
fun MonthSwiper(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentOnNext by rememberUpdatedState(onNext)
    val previousLabel = stringResource(R.string.navigation_previous_month)
    val nextLabel = stringResource(R.string.navigation_next_month)
    val hint = stringResource(R.string.swipe_month_hint)

    var width by remember { mutableFloatStateOf(1f) }
    val swipeDistance = with(density) { SWIPE_DISTANCE.toPx() }
    val swipeVelocity = with(density) { SWIPE_VELOCITY_DP.dp.toPx() }

    fun change(direction: Int) {
        haptics(Haptic.LIGHT)
        if (direction == 1) currentOnNext() else currentOnPrevious()
    }

    // Skyv ut, bytt måned, og la den nye måneden gli inn fra motsatt side
    fun slide(direction: Int) {
        scope.launch {
            offset.animateTo(-direction * width, tween(160))
            change(direction)
            offset.snapTo(direction * width * 0.6f)
            offset.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
                .graphicsLayer {
                    translationX = offset.value
                    alpha = 1f - 0.8f * (abs(offset.value) / width).coerceIn(0f, 1f)
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> scope.launch { offset.snapTo(offset.value + delta) } },
                    onDragStopped = { velocity ->
                        val x = offset.value
                        when {
                            x < -swipeDistance || velocity < -swipeVelocity -> slide(1)
                            x > swipeDistance || velocity > swipeVelocity -> slide(-1)
                            else -> offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
                        }
                    },
                )
                .semantics {
                    stateDescription = label
                    customActions = listOf(
                        CustomAccessibilityAction(previousLabel) { change(-1); true },
                        CustomAccessibilityAction(nextLabel) { change(1); true },
                    )
                },
        ) { content() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { slide(-1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = previousLabel, tint = colors.mutedForeground)
            }
            Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            IconButton(onClick = { slide(1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = nextLabel, tint = colors.mutedForeground)
            }
        }
    }
}
