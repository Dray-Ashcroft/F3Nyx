package com.dking.crocapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shared "drop on water" tap effect: two soft rings spread from the tap point and the
 * tapped item gives a small squash-and-settle. Used by the Files/Folder/Text switch,
 * the phone/panel switch and the Transmit/Retrieve dock.
 */
@Stable
class DropletState internal constructor(private val scope: CoroutineScope) {
    internal val wave = Animatable(1f)
    internal val squash = Animatable(1f)
    var at by mutableStateOf(Offset.Zero)
        internal set
    var target by mutableStateOf(-1)
        internal set

    /** Start a drop at [center] (px, in the drawing box's coordinates) for item [index]. */
    fun drop(center: Offset, index: Int = -1) {
        at = center
        target = index
        scope.launch { wave.snapTo(0f); wave.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) }
        scope.launch {
            squash.snapTo(0.9f)
            squash.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow))
        }
    }
}

@Composable
fun rememberDroplet(): DropletState {
    val scope = rememberCoroutineScope()
    return remember { DropletState(scope) }
}

/** Draws the rings behind the content. Clip the parent to keep them inside the track. */
fun Modifier.dropletRings(state: DropletState, color: Color, maxRadiusPx: Float): Modifier =
    drawBehind {
        val p = state.wave.value
        if (p >= 1f) return@drawBehind
        for (k in 0..1) {
            val start = k * 0.22f
            val q = ((p - start) / (1f - start)).coerceIn(0f, 1f)
            if (q <= 0f) continue
            val e = 1f - (1f - q) * (1f - q)
            drawCircle(
                color = color.copy(alpha = 0.5f * (1f - q)),
                radius = maxRadiusPx * e,
                center = state.at,
                style = Stroke(width = (2.2f * (1f - q) + 0.6f) * density)
            )
        }
        drawCircle(color.copy(alpha = 0.14f * (1f - p)), radius = maxRadiusPx * 0.5f * p, center = state.at)
    }

/** Squash-and-settle for the item that was tapped. */
fun Modifier.dropletSquash(state: DropletState, index: Int): Modifier =
    graphicsLayer {
        if (state.target == index) {
            val s = state.squash.value
            scaleX = 2f - s
            scaleY = s
        }
    }
