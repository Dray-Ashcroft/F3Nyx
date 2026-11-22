package com.dking.crocapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dking.crocapp.R
import com.dking.crocapp.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Jarvis (phone: Send + Receive): cyan HUD */
val MekoGreen = Color(0xFF38BDF8)
private val MekoDeep = Color(0xFF0369A1)

/** Friday (panel: Receive only, waits with a QR): amber HUD */
val RimasViolet = Color(0xFFF59E0B)
private val RimasDeep = Color(0xFFB45309)

/**
 * The small mode dot shown beside each main screen's title.
 *
 * Hold it (~1 s, the system long-press) to switch Meko ⇄ Rimas; a short
 * callout says who was summoned. A single tap does nothing on purpose, so a
 * student brushing the smart panel can't switch it by accident.
 *
 * It reads and writes the saved mode directly; the app reacts to the change
 * (Rimas hides the bottom bar and goes to the QR screen).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModeDot(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val repo = remember { UserPreferencesRepository(context) }
    val panelModeFlow = remember { repo.preferencesFlow.map { it.panelMode } }
    val panelMode by panelModeFlow.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val isRimas = panelMode ?: return // not loaded yet: draw nothing
    val color = if (isRimas) RimasViolet else MekoGreen
    val name = stringResource(if (isRimas) R.string.mode_rimas else R.string.mode_meko)
    val a11y = stringResource(R.string.mode_dot_a11y, name)

    // Remember the last summoned mode so the label keeps its colour while fading out.
    var lastRimas by remember { mutableStateOf(false) }
    ModeCallout.summoned?.let { lastRimas = it }
    val labelColor = if (lastRimas) RimasViolet else MekoGreen

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // Mode light: cyan ring = Jarvis (phone), amber ring = Friday (panel). Hold to switch.
        Box(
            modifier = Modifier
                .size(40.dp) // touch target around the small light
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* intentionally nothing */ },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val next = !isRimas
                        scope.launch { repo.updatePanelMode(next) }
                        ModeCallout.show(next)
                    }
                )
                .semantics { contentDescription = a11y },
            contentAlignment = Alignment.Center
        ) {
            ModeLight(color = color)
        }

        // Quiet inline callout: slides out beside the dot, then fades away.
        AnimatedVisibility(
            visible = false, // name now shown by the full-screen switch overlay
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            Text(
                text = stringResource(
                    if (lastRimas) R.string.mode_summoned_rimas else R.string.mode_summoned_meko
                ),
                color = labelColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * App-wide "… is summoned" callout. Lives outside the screens because
 * switching mode changes the screen (and the dot that was pressed).
 */
object ModeCallout {
    /** true = Rimas summoned, false = Meko summoned, null = hidden */
    var summoned by mutableStateOf<Boolean?>(null)
        private set
    var key by mutableIntStateOf(0)
        private set

    fun show(rimas: Boolean) {
        summoned = rimas
        key++
    }

    fun hide() {
        summoned = null
    }
}

/**
 * Runs the callout timer. The label itself is drawn by [ModeDot] (beside the
 * title), so the new screen's dot shows it even after the mode switch
 * changed screens. Place once in the app.
 */
@Composable
fun ModeCalloutHost() {
    val key = ModeCallout.key
    // Hiding is driven by ModeSwitchOverlay (1 s flash or 4.5 s first-time intro).
    LaunchedEffect(key) { }
}

/**
 * Mode mark: a small HUD ring with two rotating arc segments and a core.
 * Animation runs in the draw phase only (no recomposition).
 *  - Jarvis (phone): arcs turn slowly all the time.
 *  - Friday (panel): still, to spare weak panel GPUs; spins up only when summoned.
 */
@Composable
fun HudMark(rimas: Boolean, modifier: Modifier = Modifier) {
    val spin = remember { Animatable(0f) }
    val glow = remember { Animatable(0.4f) }

    LaunchedEffect(rimas, ModeCallout.key) {
        launch {
            glow.animateTo(1f, tween(250))
            glow.animateTo(0.4f, tween(900))
        }
        // Summon: one fast turn.
        spin.animateTo(spin.value + 360f, tween(900, easing = FastOutSlowInEasing))
        spin.snapTo(spin.value % 360f)
        if (!rimas && UiEffects.full) {
            while (true) {
                spin.animateTo(spin.value + 360f, tween(6_000, easing = LinearEasing))
                spin.snapTo(spin.value % 360f)
            }
        }
    }

    val light = if (rimas) RimasViolet else MekoGreen
    val deep = if (rimas) RimasDeep else MekoDeep

    Canvas(modifier = modifier) {
        drawHud(light, deep, spin.value, glow.value)
    }
}

private fun DrawScope.drawHud(light: Color, deep: Color, angle: Float, glowAlpha: Float) {
    val r = size.minDimension / 2f
    val c = center
    // Outer thin ring
    drawCircle(light.copy(alpha = 0.35f + 0.4f * glowAlpha), radius = r * 0.94f, style = Stroke(width = r * 0.08f))
    // Two rotating arc segments
    rotate(angle, pivot = c) {
        val inset = r * 0.26f
        val arcSize = androidx.compose.ui.geometry.Size(2f * (r - inset), 2f * (r - inset))
        val topLeft = Offset(c.x - (r - inset), c.y - (r - inset))
        listOf(20f, 200f).forEach { start ->
            drawArc(
                color = light,
                startAngle = start,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = r * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
    // Core
    drawCircle(
        Brush.radialGradient(listOf(Color.White, light, deep), center = c, radius = r * 0.36f),
        radius = r * 0.30f,
        center = c
    )
}

/**
 * FeNyx mark: two arrows (red, gold) chasing each other around a small core.
 * Each arrow is an arc with a rounded tail and a filled head aligned to the arc's
 * tangent, so both ends are clean at any size or rotation.
 */
@Composable
fun LogoMark(
    modifier: Modifier = Modifier.size(28.dp),
    core: Color = Color(0xFF5EE6FF),
    rotation: Float = 0f,
    coreAlpha: Float = 1f
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 32f
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = 10.5f * u
        val stroke = 2.6f * u
        val headLen = 4.4f * u
        val headHalf = 3.0f * u

        fun arrow(color: Color, startDeg: Float, sweepDeg: Float) {
            // Arc body: round tail, ends where the head's base begins.
            drawArc(
                color = color, startAngle = startDeg, sweepAngle = sweepDeg, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            // Head: base on the circle at the arc's end, tip along the clockwise tangent.
            val a = Math.toRadians((startDeg + sweepDeg).toDouble())
            val cos = kotlin.math.cos(a).toFloat()
            val sin = kotlin.math.sin(a).toFloat()
            val end = Offset(c.x + r * cos, c.y + r * sin)
            val tangent = Offset(-sin, cos)      // clockwise direction on screen
            val normal = Offset(cos, sin)        // pointing outward
            val tip = end + tangent * headLen
            val outer = end + normal * headHalf
            val inner = end - normal * headHalf
            val head = Path().apply {
                moveTo(outer.x, outer.y); lineTo(tip.x, tip.y); lineTo(inner.x, inner.y); close()
            }
            drawPath(head, color)
            drawPath(head, color, style = Stroke(width = 0.8f * u, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }

        rotate(rotation, pivot = c) {
            arrow(Color(0xFFC42B22), startDeg = 196f, sweepDeg = 122f)  // red: left, over the top
            arrow(Color(0xFFD4A017), startDeg = 16f, sweepDeg = 122f)   // gold: right, under the bottom
        }
        // Thin gold bracket ring around the core (matches the app icon).
        val br = 5.2f * u
        for (k in 0 until 4) {
            drawArc(
                Color(0xFFD4A017).copy(alpha = 0.9f), startAngle = k * 90f + 57f, sweepAngle = 66f, useCenter = false,
                topLeft = Offset(c.x - br, c.y - br), size = androidx.compose.ui.geometry.Size(br * 2, br * 2),
                style = Stroke(width = 0.9f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        // Core stays upright and blinks on its own.
        drawCircle(core.copy(alpha = 0.30f * coreAlpha), radius = 4.0f * u, center = c)
        drawCircle(Color.White.copy(alpha = coreAlpha), radius = 1.6f * u, center = c)
        drawCircle(core.copy(alpha = 0.85f * coreAlpha), radius = 2.6f * u, center = c, style = Stroke(width = 1.1f * u))
    }
}

/** Header mark: slow turning arrows, faint blinking core in the mode colour. Still when Full effects is off. */
@Composable
fun ModeLogo(core: Color, modifier: Modifier = Modifier) {
    if (!UiEffects.full) {
        LogoMark(modifier = modifier, core = core)
        return
    }
    val t = rememberInfiniteTransition(label = "logo")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (TransferActivity.active) 4_000 else 12_000, easing = LinearEasing)), label = "rot")
    val blink by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(1_400), RepeatMode.Reverse), label = "blink")
    LogoMark(modifier = modifier, core = core, rotation = rot, coreAlpha = blink)
}

/** Small glowing ring in the mode colour; breathes softly when Full effects is on. */
@Composable
fun ModeLight(color: Color, modifier: Modifier = Modifier) {
    val glow = if (UiEffects.full) {
        rememberInfiniteTransition(label = "light").animateFloat(
            0.55f, 1f, infiniteRepeatable(tween(1_600), RepeatMode.Reverse), label = "glow"
        ).value
    } else 0.8f
    Canvas(modifier = modifier.size(26.dp)) {
        val r = 6.dp.toPx()
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.45f * glow), Color.Transparent), center = center, radius = r * 2.1f),
            radius = r * 2.1f
        )
        drawCircle(color, radius = r, style = Stroke(width = 2.dp.toPx()))
    }
}
