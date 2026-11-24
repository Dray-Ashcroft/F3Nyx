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
 * tap the stage to skip; never with Full effects off, animations off or battery saver on.
 */
object SuitCue {
    var request by mutableStateOf<Boolean?>(null) // true = launch (transmitted), false = land (retrieved)
        private set
    var key by mutableIntStateOf(0)
        private set
    /** The finished transfer this cue belongs to; only that transfer's card plays it. */
    var source by mutableIntStateOf(0)
        private set

    /** [source] identifies the finished transfer so re-showing the same result doesn't replay it. */
    fun play(launch: Boolean, source: Int) {
        if (source == this.source) return
        this.source = source
        request = launch
        key++
    }

    fun done() { request = null }
}

private const val SUIT_ASPECT = 360f / 537f
private const val FOOT_L = 0.42f
private const val FOOT_R = 0.63f
// Landing image (356x398 px at the same scale as the 360 px standing image), placed so the bodies line up.
private val LAND_W = 148.dp
private val LAND_H = 166.dp
private val LAND_DX = 41.dp
private val CHEST = Offset(0.57f, 0.245f)
private val HAND_L = Offset(0.10f, 0.52f)
private val HAND_R = Offset(0.88f, 0.54f)

/** True while a suit cue is queued and motion is allowed; hosts show [SuitScene] then. */
@Composable
fun suitPlaying(source: Int? = null): Boolean {
    val context = LocalContext.current
    SuitCue.request ?: return false
    // Both tabs stay built in the pager; only the card of the transfer that just
    // finished may host the scene, or two scenes race and cut each other short.
    if (source != null && source != SuitCue.source) return false
    if (!UiEffects.motionAllowed(context)) {
        LaunchedEffect(SuitCue.key) { SuitCue.done() }
        return false
    }
    return true
}

/**
 * The suit scene, drawn inside its host (the Stage card), never over the screen.
 * He stands on the host's floor line [bottomGap] above its bottom edge; lift-off leaves
 * through the top of the host, the landing comes in from it. The host should clip.
 */
@Composable
fun SuitScene(modifier: Modifier = Modifier, suitW: androidx.compose.ui.unit.Dp = 120.dp, bottomGap: androidx.compose.ui.unit.Dp = 14.dp) {
    val isLaunch = SuitCue.request ?: return
    val context = LocalContext.current
    val k = suitW / 150.dp // landing image was laid out for a 150 dp suit

    BoxWithConstraints(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { SuitCue.done() } // tap anywhere to skip
    ) {
        val density = LocalDensity.current
        val suitH = suitW / SUIT_ASPECT
        // Travel: from the floor line out past the top of the host.
        val screenH = with(density) { (maxHeight + suitH).toPx() }

        val y = remember { Animatable(0f) }      // px offset from the standing spot (negative = up)
        val alpha = remember { Animatable(0f) }
        val jet = remember { Animatable(0f) }    // 0..1.3 jet length
        val dip = remember { Animatable(1f) }    // landing / take-off squash
        val ground = remember { Animatable(0f) }
        val look = remember { Animatable(0f) }   // 0 = head level, 1 = looking up
        val hands = remember { Animatable(0f) }  // palm jets
        val pose = remember { Animatable(0f) }   // 1 = superhero-landing crouch
        val shock = remember { Animatable(0f) }  // landing shockwave 0..1
        val dust = remember { Animatable(0f) }   // touchdown dust 0..1

        LaunchedEffect(SuitCue.key) {
            val full = UiEffects.consumeSuitFullForToday(context, isLaunch)
            val ease = CubicBezierEasing(0.45f, 0f, 0.8f, 0.45f)  // slow lift-off, then away
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
                    y.animateTo(-screenH, tween(if (full) 2200 else 1200, easing = ease))
                }
            } else {
                y.snapTo(-screenH); jet.snapTo(1.3f); hands.snapTo(1f); alpha.snapTo(1f); pose.snapTo(0f)
                // Descent: thrusters ease off as he slows toward the ground.
                val d = if (full) 1400 else 1000
                coroutineScope {
                    launch { jet.animateTo(0.3f, tween(d, easing = soft)) }
                    launch { hands.animateTo(0.25f, tween(d, easing = soft)) }
                    y.animateTo(0f, tween(d, easing = soft))
                }
                // Touchdown: jets cut, superhero landing, a little dust.
                coroutineScope {
                    launch { jet.animateTo(0f, tween(220)) }
                    launch { hands.animateTo(0f, tween(200)) }
                    launch { pose.animateTo(1f, tween(140)) }
                    launch { shock.snapTo(0f); shock.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
                    launch { dust.snapTo(0f); dust.animateTo(1f, tween(1100, easing = LinearEasing)) }
                    launch { ground.snapTo(0.6f); ground.animateTo(0f, tween(500)) }
                    dip.snapTo(0.93f); dip.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
                }
                kotlinx.coroutines.delay(if (full) 1300 else 900) // hold the landing pose so it reads
                pose.animateTo(0f, tween(if (full) 420 else 320, easing = FastOutSlowInEasing)) // stands up
                kotlinx.coroutines.delay(if (full) 500 else 250)
                alpha.animateTo(0f, tween(if (full) 450 else 300))
            }
            SuitCue.done()
        }

        val flick = rememberInfiniteTransition(label = "jet").animateFloat(
            0.88f, 1.1f, infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse), label = "flick"
        ).value
        val pulse = rememberInfiniteTransition(label = "core").animateFloat(
            0.75f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
        ).value

        // Ground effects stay on the floor line.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -bottomGap)
                .size(suitW, suitH)
                .graphicsLayer { this.alpha = alpha.value }
        ) {
            // Ground glow (stays at the feet's line).
            Canvas(Modifier.fillMaxSize()) {
                // Thruster wash on the floor: grows as the jets get close.
                val prox = (1f + y.value / (size.height * 1.1f)).coerceIn(0f, 1f)
                val wash = jet.value.coerceAtMost(1.2f) * prox
                if (wash > 0.01f) {
                    val c0 = Offset(size.width / 2f, size.height)
                    val rr = size.width * (0.35f + 0.75f * prox)
                    drawOval(
                        Brush.radialGradient(
                            0f to Color(0xFFE1F8FF).copy(alpha = 0.55f * wash),
                            0.35f to Color(0xFF5EE6FF).copy(alpha = 0.35f * wash),
                            1f to Color.Transparent,
                            center = c0, radius = rr
                        ),
                        topLeft = Offset(c0.x - rr, c0.y - rr * 0.13f), size = Size(rr * 2f, rr * 0.26f)
                    )
                }
                // Dust kicked up at touchdown.
                val dq = dust.value
                if (dq > 0f && dq < 1f) {
                    val q = 1f - (1f - dq) * (1f - dq) * (1f - dq)
                    val w = size.width
                    for (i in 0 until 22) {
                        val dir = if (i % 2 == 1) 1f else -1f
                        val v = 0.35f + ((i * 37) % 17) / 17f * 0.9f
                        val up = ((i * 53) % 11) / 11f * 0.35f
                        val sz = 0.6f + ((i * 29) % 7) / 7f * 0.8f
                        val px = w / 2f + dir * w * (0.12f + v * 0.9f * q)
                        val py = size.height - w * (up * 0.35f * q + 0.02f) + w * 0.08f * q * q
                        val rad = w * 0.06f * sz * (1f + 2.4f * q)
                        drawCircle(
                            Brush.radialGradient(listOf(Color(0xFFA0968E).copy(alpha = 0.16f * (1f - dq)), Color.Transparent), center = Offset(px, py), radius = rad),
                            radius = rad, center = Offset(px, py)
                        )
                    }
                }
                val g = ground.value
                if (g > 0f) {
                    val c = Offset(size.width / 2f, size.height)
                    drawOval(
                        Brush.radialGradient(listOf(Color(0xFF5EE6FF).copy(alpha = 0.45f * g), Color.Transparent), center = c, radius = size.width * 0.9f),
                        topLeft = Offset(c.x - size.width * 0.9f, c.y - size.width * 0.14f),
                        size = Size(size.width * 1.8f, size.width * 0.28f)
                    )
                }
                val k = shock.value
                if (k > 0f && k < 1f) {
                    val c = Offset(size.width / 2f, size.height)
                    val rx = size.width * (0.3f + 1.1f * k)
                    drawOval(
                        Color(0xFFFFDCAA).copy(alpha = 0.18f * (1f - k)),
                        topLeft = Offset(c.x - rx, c.y - rx * 0.16f),
                        size = Size(rx * 2f, rx * 0.32f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * (1f - k) + 1f)
                    )
                }
            }
        }
        // Landing crouch (Retrieve only).
        if (pose.value > 0f) Image(
            painter = painterResource(R.drawable.suit_land),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = LAND_DX * k, y = -bottomGap)
                .size(LAND_W * k, LAND_H * k)
                .graphicsLayer {
                    this.alpha = alpha.value * pose.value
                    scaleY = dip.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -bottomGap)
                .size(suitW, suitH)
                .graphicsLayer {
                    translationY = y.value
                    this.alpha = alpha.value * (1f - pose.value)
                    scaleY = dip.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
        ) {
            // Jets below each boot (drawn under the suit).
            Canvas(Modifier.fillMaxSize()) {
                val j = jet.value * flick
                if (j > 0.01f) {
                    drawJet(Offset(size.width * FOOT_L, size.height * 0.985f), j, 0.65f)
                    drawJet(Offset(size.width * FOOT_R, size.height * 0.985f), j, 0.65f)
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
                    drawJet(Offset(size.width * HAND_L.x, size.height * HAND_L.y), h, 0.4f)
                    drawJet(Offset(size.width * HAND_R.x, size.height * HAND_R.y), h, 0.4f)
                }
            }
            // Chest reactor: soft glow over the suit's own reactor, brighter while the jets are lit.
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width * CHEST.x, size.height * CHEST.y)
                val u = size.width / 100f
                val k = (pulse + 0.35f * jet.value.coerceAtMost(1f)).coerceAtMost(1.25f)
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF5EE6FF).copy(alpha = 0.55f * k.coerceAtMost(1f)), Color.Transparent), center = c, radius = 11f * u * k),
                    radius = 11f * u * k, center = c
                )
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
