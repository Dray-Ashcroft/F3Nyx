package com.dking.crocapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class IconTab(val icon: ImageVector, val label: String)

private val SLOT_W = 64.dp
private val SLOT_H = 44.dp
private val PAD = 4.dp

/**
 * Icon-only switch with a sliding bubble. The bubble's leading edge springs ahead of
 * its trailing edge, so it stretches a little while it travels and settles softly.
 * Tapping drops a small ripple. Drag across the icons to scrub. The tab's name shows briefly under the icons, then fades.
 */
@Composable
fun IconSwitch(
    tabs: List<IconTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val slotPx = with(density) { SLOT_W.toPx() }
    val sel = selected.coerceIn(0, tabs.lastIndex)
    val select by rememberUpdatedState(onSelect)

    // Bubble edges in px (left and right), animated separately for the stretch.
    val left = remember { Animatable(sel * slotPx) }
    val right = remember { Animatable((sel + 1) * slotPx) }
    LaunchedEffect(sel, slotPx) {
        val toL = sel * slotPx
        val toR = (sel + 1) * slotPx
        val movingRight = toL > left.value
        val fast = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow * 1.6f)
        val slow = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow * 1.3f)
        coroutineScope {
            launch { left.animateTo(toL, if (movingRight) slow else fast) }
            launch { right.animateTo(toR, if (movingRight) fast else slow) }
        }
    }

    // Brief name hint after a change (not on first show).
    var hint by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(sel) {
        if (first) { first = false; return@LaunchedEffect }
        hint = true
        delay(1_200)
        hint = false
    }
    val hintAlpha by animateFloatAsState(if (hint) 1f else 0f, tween(220), label = "hint")

    // Droplet: a tap sends two soft rings out from the icon and the bubble gives a
    // small squash-and-settle, like a drop landing on water.
    val scope = rememberCoroutineScope()
    var dropAt by remember { mutableStateOf(Offset.Zero) }
    val wave = remember { Animatable(1f) }
    val squash = remember { Animatable(1f) }
    val slotHPx = with(density) { SLOT_H.toPx() }
    fun drop(i: Int) {
        dropAt = Offset((i + 0.5f) * slotPx, slotHPx / 2f)
        scope.launch { wave.snapTo(0f); wave.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) }
        scope.launch {
            squash.snapTo(0.9f)
            squash.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val ringColor = cs.onSurfaceVariant

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cs.surfaceContainerHighest.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .padding(PAD)
                    .pointerInput(tabs.size, enabled) {
                        if (!enabled) return@pointerInput
                        detectHorizontalDragGestures { change, _ ->
                            val i = (change.position.x / slotPx).toInt().coerceIn(0, tabs.lastIndex)
                            select(i)
                        }
                    }
            ) {
                // Bubble
                val l = with(density) { left.value.toDp() }
                val w = with(density) { (right.value - left.value).coerceAtLeast(slotPx * 0.6f).toDp() }
                Box(
                    Modifier
                        .offset(x = l)
                        .width(w)
                        .height(SLOT_H)
                        .graphicsLayer { scaleX = 2f - squash.value; scaleY = squash.value }
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (com.dking.crocapp.ui.theme.LocalMonet.current) cs.primary else cs.inverseSurface)
                )
                // Droplet rings (behind the icons, clipped to the track).
                Canvas(Modifier.matchParentSize()) {
                    val p = wave.value
                    if (p < 1f) {
                        val maxR = slotPx * 0.85f
                        for (k in 0..1) {
                            val start = k * 0.22f
                            val q = ((p - start) / (1f - start)).coerceIn(0f, 1f)
                            if (q <= 0f) continue
                            val e = 1f - (1f - q) * (1f - q)
                            drawCircle(
                                color = ringColor.copy(alpha = 0.5f * (1f - q)),
                                radius = maxR * e,
                                center = dropAt,
                                style = Stroke(width = (2.2f * (1f - q) + 0.6f) * density.density)
                            )
                        }
                        drawCircle(ringColor.copy(alpha = 0.14f * (1f - p)), radius = maxR * 0.5f * p, center = dropAt)
                    }
                }
                Row {
                    tabs.forEachIndexed { i, tab ->
                        val on = i == sel
                        val scale by animateFloatAsState(
                            if (on) 1.12f else 1f,
                            spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                            label = "iconScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(SLOT_W, SLOT_H)
                                .clip(RoundedCornerShape(22.dp))
                                .selectable(
                                    selected = on,
                                    enabled = enabled,
                                    role = Role.Tab,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { drop(i); select(i) }
                                )
                                .semantics { contentDescription = tab.label },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                tint = if (on) (if (com.dking.crocapp.ui.theme.LocalMonet.current) cs.onPrimary else cs.inverseOnSurface) else cs.onSurfaceVariant,
                                modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = tabs[sel].label,
            style = MaterialTheme.typography.labelMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = hintAlpha }
        )
    }
}
