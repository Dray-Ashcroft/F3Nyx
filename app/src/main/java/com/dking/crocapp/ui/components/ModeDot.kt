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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
 * App top bar without words: logo on the left, the phone | panel switch centred on the
 * screen (not in the leftover space), History and Settings on the right. One centre line,
 * 56 dp tall, same edge margin as the cards below.
 */
@Composable
fun AppTopBar(
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    margin: androidx.compose.ui.unit.Dp = 16.dp
) {
    val context = LocalContext.current.applicationContext
    val repo = remember { UserPreferencesRepository(context) }
    val panelModeFlow = remember { repo.preferencesFlow.map { it.panelMode } }
    val panelMode by panelModeFlow.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = margin)
    ) {
        LogoMark(modifier = Modifier.align(Alignment.CenterStart).size(30.dp))
        panelMode?.let { isPanel ->
            LayoutSwitch(
                panel = isPanel,
                onSelect = { toPanel ->
                    if (toPanel != isPanel) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch { repo.updatePanelMode(toPanel) }
                        ModeCallout.show(toPanel)
                    }
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Row(modifier = Modifier.align(Alignment.CenterEnd).offset(x = 10.dp)) {
            androidx.compose.material3.IconButton(onClick = onHistory) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Outlined.History,
                    contentDescription = stringResource(R.string.nav_history)
                )
            }
            androidx.compose.material3.IconButton(onClick = onSettings) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.nav_settings)
                )
            }
        }
    }
}

/** Two round segments: phone | panel. The selected one is filled. */
@Composable
fun LayoutSwitch(panel: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val drop = rememberDroplet()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Show the new side at once; the layout change follows after the ripple has started,
    // because switching layout swaps the screen (and this switch with it).
    var shown by androidx.compose.runtime.remember(panel) { androidx.compose.runtime.mutableStateOf(panel) }
    fun tap(toPanel: Boolean) {
        if (toPanel == shown) return
        val i = if (toPanel) 1 else 0
        with(density) {
            drop.drop(Offset((3 + i * 46 + 22).dp.toPx(), (3 + 18).dp.toPx()), i)
        }
        shown = toPanel
        scope.launch {
            kotlinx.coroutines.delay(260)
            onSelect(toPanel)
        }
    }
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = cs.surfaceContainerHighest.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .dropletRings(drop, cs.onSurfaceVariant, with(density) { 40.dp.toPx() })
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SwitchSegment(
                icon = androidx.compose.material.icons.Icons.Rounded.Smartphone,
                label = stringResource(R.string.layout_phone),
                selected = !shown,
                modifier = Modifier.dropletSquash(drop, 0),
                onClick = { tap(false) }
            )
            SwitchSegment(
                icon = androidx.compose.material.icons.Icons.Rounded.Tv,
                label = stringResource(R.string.layout_panel),
                selected = shown,
                modifier = Modifier.dropletSquash(drop, 1),
                onClick = { tap(true) }
            )
        }
    }
}

@Composable
private fun SwitchSegment(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val monet = com.dking.crocapp.ui.theme.LocalMonet.current
    val bg by androidx.compose.animation.animateColorAsState(
        if (!selected) Color.Transparent else if (monet) cs.primary else cs.inverseSurface,
        androidx.compose.animation.core.tween(220), label = "segBg"
    )
    val fg by androidx.compose.animation.animateColorAsState(
        if (!selected) cs.onSurfaceVariant else if (monet) cs.onPrimary else cs.inverseOnSurface,
        androidx.compose.animation.core.tween(220), label = "segFg"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (selected) 1.12f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
        label = "segIcon"
    )
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.size(width = 44.dp, height = 36.dp).semantics {
            contentDescription = label
            this.selected = selected
        },
        shape = RoundedCornerShape(18.dp),
        color = bg,
        contentColor = fg
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(
                icon, contentDescription = null,
                modifier = Modifier.size(18.dp).graphicsLayer { scaleX = scale; scaleY = scale }
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
 * F3Nyx mark: two arrows (red, gold) chasing each other around a small core.
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
    // Light mode: the original mark sits on a small dark badge so it reads on white.
    val light = androidx.compose.material3.MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val red = Color(0xFFC42B22)
    val gold = Color(0xFFD4A017)
    val coreC = core
    val centre = Color.White
    Canvas(modifier = modifier) {
        val u = size.minDimension / 32f
        if (light) drawCircle(Color(0xFF1B1B1F), radius = 16f * u)
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
            arrow(red, startDeg = 196f, sweepDeg = 122f)  // red: left, over the top
            arrow(gold, startDeg = 16f, sweepDeg = 122f)   // gold: right, under the bottom
        }
        // Thin gold bracket ring around the core (matches the app icon).
        val br = 5.2f * u
        for (k in 0 until 4) {
            drawArc(
                gold.copy(alpha = 0.9f), startAngle = k * 90f + 57f, sweepAngle = 66f, useCenter = false,
                topLeft = Offset(c.x - br, c.y - br), size = androidx.compose.ui.geometry.Size(br * 2, br * 2),
                style = Stroke(width = 0.9f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        // Core stays upright and blinks on its own.
        drawCircle(coreC.copy(alpha = 0.30f * coreAlpha), radius = 4.0f * u, center = c)
        drawCircle(centre.copy(alpha = coreAlpha), radius = 1.6f * u, center = c)
        drawCircle(coreC.copy(alpha = 0.85f * coreAlpha), radius = 2.6f * u, center = c, style = Stroke(width = 1.1f * u))
    }
}



/** Small glowing ring in the mode colour; breathes softly when Full effects is on. */
@Composable
fun ModeLight(color: Color, modifier: Modifier = Modifier) {
    val glow = if (UiEffects.full) {
        rememberInfiniteTransition(label = "light").animateFloat(
            0.55f, 1f, infiniteRepeatable(tween(1_600), RepeatMode.Reverse), label = "glow"
        ).value
    } else 0.8f
    // Light mode: deeper shade and a slightly thicker ring so it reads on white.
    val light = androidx.compose.material3.MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val ring = when {
        !light -> color
        color == MekoGreen -> Color(0xFF0284C7)     // Jarvis: deep sky blue
        color == RimasViolet -> Color(0xFFD97706)   // Friday: deep amber
        else -> androidx.compose.ui.graphics.lerp(color, Color.Black, 0.25f)
    }
    Canvas(modifier = modifier.size(26.dp)) {
        val r = 6.dp.toPx()
        drawCircle(
            Brush.radialGradient(listOf(ring.copy(alpha = (if (light) 0.30f else 0.45f) * glow), Color.Transparent), center = center, radius = r * 2.1f),
            radius = r * 2.1f
        )
        drawCircle(ring, radius = r, style = Stroke(width = (if (light) 2.6f else 2f).dp.toPx()))
    }
}
