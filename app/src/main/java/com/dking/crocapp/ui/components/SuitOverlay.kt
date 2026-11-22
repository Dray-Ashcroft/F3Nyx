package com.dking.crocapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dking.crocapp.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * One-shot cue for the stealth suit after a finished transfer.
 *  - Transmitted: he stands, looks up, fires boot and palm jets and launches up out of the screen.
 *  - Retrieved:   he descends from above into the same spot, lands and fades out.
 * Rules: once per finished transfer; full version the first time each day, short after;
 * tap anywhere to skip; never with Full effects off.
 */
object SuitCue {
    var request by mutableStateOf<Boolean?>(null) // true = launch (transmitted), false = land (retrieved)
        private set
    var key by mutableIntStateOf(0)
        private set
    private var lastSource: Int = 0

    /** [source] identifies the finished transfer so re-showing the same result doesn't replay it. */
    fun play(launch: Boolean, source: Int) {
        if (source == lastSource) return
        lastSource = source
        request = launch
        key++
    }

    fun done() { request = null }
}

private const val SUIT_ASPECT = 300f / 890f
private const val FOOT_L = 0.23f
private const val FOOT_R = 0.70f
private val CHEST = Offset(0.60f, 0.21f)
private val HAND_L = Offset(0.21f, 0.555f)
private val HAND_R = Offset(0.88f, 0.535f)

@Composable
fun SuitOverlay() {
    val isLaunch = SuitCue.request ?: return
    val context = LocalContext.current
    if (!UiEffects.full) {
        LaunchedEffect(SuitCue.key) { SuitCue.done() }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { SuitCue.done() } // tap anywhere to skip
    ) {
        val density = LocalDensity.current
        val screenH = with(density) { maxHeight.toPx() }
        val suitW = 100.dp
        val suitH = suitW / SUIT_ASPECT
        val bottomGap = 132.dp // stands just above the dock

        val y = remember { Animatable(0f) }      // px offset from the standing spot (negative = up)
        val alpha = remember { Animatable(0f) }
        val jet = remember { Animatable(0f) }    // 0..1.3 jet length
        val dip = remember { Animatable(1f) }    // landing / take-off squash
        val ground = remember { Animatable(0f) }
        val look = remember { Animatable(0f) }   // 0 = head level, 1 = looking up
        val hands = remember { Animatable(0f) }  // palm jets

        LaunchedEffect(SuitCue.key) {
            val full = UiEffects.consumeSuitFullForToday(context)
            val ease = CubicBezierEasing(0.5f, 0f, 0.75f, 0.2f)   // accelerate away
            val soft = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)    // decelerate in
            if (isLaunch) {
                y.snapTo(0f); jet.snapTo(0f)
                alpha.animateTo(1f, tween(if (full) 300 else 120))
                if (full) kotlinx.coroutines.delay(350)
                look.animateTo(1f, tween(if (full) 380 else 160, easing = FastOutSlowInEasing))
                if (full) kotlinx.coroutines.delay(250)
                coroutineScope {
                    launch { jet.animateTo(1f, tween(if (full) 280 else 140)) }
                    launch { hands.animateTo(1f, tween(if (full) 280 else 140)) }
                    launch { ground.animateTo(1f, tween(200)); ground.animateTo(0f, tween(600)) }
                    dip.animateTo(0.96f, tween(140)); dip.animateTo(1f, tween(160))
                }
                coroutineScope {
                    launch { jet.animateTo(1.3f, tween(300)) }
                    y.animateTo(-screenH, tween(if (full) 950 else 600, easing = ease))
                }
            } else {
                y.snapTo(-screenH); jet.snapTo(1.3f); hands.snapTo(1f); alpha.snapTo(1f)
                coroutineScope {
                    launch { jet.animateTo(0.5f, tween(if (full) 1100 else 600)) }
                    launch { hands.animateTo(0.6f, tween(if (full) 1100 else 600)) }
                    y.animateTo(0f, tween(if (full) 1100 else 600, easing = soft))
                }
                coroutineScope {
                    launch { jet.animateTo(0f, tween(220)) }
                    launch { hands.animateTo(0f, tween(260)) }
                    launch { ground.animateTo(1f, tween(140)); ground.animateTo(0f, tween(700)) }
                    dip.animateTo(0.96f, tween(120)); dip.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }
                kotlinx.coroutines.delay(if (full) 1200 else 350)
                alpha.animateTo(0f, tween(if (full) 450 else 250))
            }
            SuitCue.done()
        }

        val flick = rememberInfiniteTransition(label = "jet").animateFloat(
            0.88f, 1.1f, infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse), label = "flick"
        ).value
        val pulse = rememberInfiniteTransition(label = "core").animateFloat(
            0.75f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
        ).value

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -bottomGap)
                .size(suitW, suitH)
                .graphicsLayer {
                    translationY = y.value
                    this.alpha = alpha.value
                    scaleY = dip.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
        ) {
            // Ground glow (stays at the feet's line).
            Canvas(Modifier.fillMaxSize()) {
                val g = ground.value
                if (g > 0f) {
                    val c = Offset(size.width / 2f, size.height)
                    drawOval(
                        Brush.radialGradient(listOf(Color(0xFF5EE6FF).copy(alpha = 0.45f * g), Color.Transparent), center = c, radius = size.width * 0.9f),
                        topLeft = Offset(c.x - size.width * 0.9f, c.y - size.width * 0.14f),
                        size = Size(size.width * 1.8f, size.width * 0.28f)
                    )
                }
            }
            // Jets below each boot (drawn under the suit).
            Canvas(Modifier.fillMaxSize()) {
                val j = jet.value * flick
                if (j > 0.01f) {
                    drawJet(Offset(size.width * FOOT_L, size.height * 0.985f), j)
                    drawJet(Offset(size.width * FOOT_R, size.height * 0.985f), j)
                }

            }
            Image(
                painter = painterResource(R.drawable.suit_stealth),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            // Head tilt: same pose with the head raised, faded in over the base (bodies match pixel for pixel).
            if (look.value > 0f) Image(
                painter = painterResource(R.drawable.suit_stealth_up),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = look.value }
            )
            // Palm jets, drawn over the body so they read in front of the legs.
            Canvas(Modifier.fillMaxSize()) {
                val h = hands.value * flick
                if (h > 0.01f) {
                    drawJet(Offset(size.width * HAND_L.x, size.height * HAND_L.y), h, 0.55f)
                    drawJet(Offset(size.width * HAND_R.x, size.height * HAND_R.y), h, 0.55f)
                }
            }
            // Chest reactor: glowing core, brighter while the jets are lit.
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width * CHEST.x, size.height * CHEST.y)
                val u = size.width / 100f
                val k = (pulse + 0.35f * jet.value.coerceAtMost(1f)).coerceAtMost(1.25f)
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF5EE6FF).copy(alpha = 0.55f * k.coerceAtMost(1f)), Color.Transparent), center = c, radius = 16f * u * k),
                    radius = 16f * u * k, center = c
                )
                drawCircle(Color(0xFFBFF4FF).copy(alpha = 0.9f), radius = 5.2f * u, center = c)
                drawCircle(Color.White, radius = 3.2f * u, center = c)
            }
        }
    }
}

/** Layered jet: soft blue plume, cyan cone, white-hot core. Points down from [top]. */
private fun DrawScope.drawJet(top: Offset, length: Float, scale: Float = 1f) {
    val u = size.width / 100f * scale
    val plumeH = 96f * u * length
    val coneH = 62f * u * length
    val coreH = 20f * u * length.coerceAtMost(1f)
    // plume
    drawOval(
        Brush.verticalGradient(
            0f to Color(0x8C78B4FF), 0.55f to Color(0x2E3C78FF), 1f to Color.Transparent,
            startY = top.y, endY = top.y + plumeH
        ),
        topLeft = Offset(top.x - 13f * u, top.y - 2f * u), size = Size(26f * u, plumeH)
    )
    // cone
    drawOval(
        Brush.verticalGradient(
            0f to Color(0xF2DCFAFF), 0.35f to Color(0xD95EE6FF), 0.75f to Color(0x402F8CFF), 1f to Color.Transparent,
            startY = top.y, endY = top.y + coneH
        ),
        topLeft = Offset(top.x - 7f * u, top.y), size = Size(14f * u, coneH)
    )
    // core
    drawOval(
        Brush.verticalGradient(0f to Color.White, 0.6f to Color(0xD9FFFFFF), 1f to Color.Transparent, startY = top.y, endY = top.y + coreH),
        topLeft = Offset(top.x - 3.5f * u, top.y), size = Size(7f * u, coreH)
    )
}
