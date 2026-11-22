package com.dking.crocapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dking.crocapp.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private val ArcCyan = Color(0xFF7FD8FF)
private const val USER_NAME = "Draven"

/** Morning before 12:00, afternoon until 17:00, evening after. */
fun timeGreeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/** Place once, above the whole app: boot screen + Jarvis/Friday switch overlay. */
@Composable
fun StarkOverlays() {
    // No boot screen in the minimal design: the app opens straight to work.
    SuitOverlay()
    ModeSwitchOverlay()
}

/**
 * Minimal Jarvis / Friday switch: a dimmed screen with two HUD rings turning
 * opposite ways, tinted cyan (Jarvis) or amber (Friday), and the name, on black. ~4.5 s; tap to skip.
 */
@Composable
fun ModeSwitchOverlay() {
    val summoned = ModeCallout.summoned
    var lastFriday by remember { mutableStateOf(false) }
    if (summoned != null) lastFriday = summoned
    val color = if (lastFriday) RimasViolet else MekoGreen
    val name = if (lastFriday) "FRIDAY" else "JARVIS"
    val role = if (lastFriday) "PANEL · ONLINE" else "PHONE · ONLINE"

    val context = LocalContext.current
    val turn = remember { Animatable(0f) }   // ring rotation, 0..1 over the whole screen
    val draw = remember { Animatable(0f) }   // middle ring drawing itself
    val text = remember { Animatable(0f) }
    var long by remember { mutableStateOf(false) }
    LaunchedEffect(ModeCallout.key) {
        if (ModeCallout.key == 0) return@LaunchedEffect
        // ~2 s the first time each mode appears; ~0.8 s after that.
        long = UiEffects.consumeFirstIntro(context, ModeCallout.summoned == true)
        val duration = if (long) 2_000 else 800
        turn.snapTo(0f); draw.snapTo(0f); text.snapTo(0f)
        launch { draw.animateTo(1f, tween((duration * 0.55f).toInt(), easing = FastOutSlowInEasing)) }
        launch {
            delay((duration * 0.3f).toLong())
            text.animateTo(1f, tween((duration * 0.25f).toInt(), easing = FastOutSlowInEasing))
        }
        turn.animateTo(1f, tween(duration, easing = LinearEasing))
        ModeCallout.hide()
    }

    AnimatedVisibility(visible = summoned != null, enter = fadeIn(tween(140)), exit = fadeOut(tween(260))) {
        val pulse by rememberInfiniteTransition(label = "core").animateFloat(
            initialValue = 0.7f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { ModeCallout.hide() }, // tap to skip
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                    // Outer tick ring: faint, slow, clockwise
                    Image(
                        painter = painterResource(R.drawable.stark_hud_outer),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            rotationZ = turn.value * (if (long) 50f else 20f)
                            val sc = 0.82f + 0.18f * draw.value
                            scaleX = sc; scaleY = sc
                            alpha = draw.value
                        }
                    )
                    // Middle ring draws itself once
                    Canvas(modifier = Modifier.size(220.dp)) {
                        drawArc(
                            color = color.copy(alpha = 0.8f),
                            startAngle = -90f, sweepAngle = 360f * draw.value, useCenter = false,
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    // Inner dial: bright, faster, counter-clockwise
                    Image(
                        painter = painterResource(R.drawable.stark_hud_inner),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color),
                        modifier = Modifier.size(170.dp).graphicsLayer {
                            rotationZ = -turn.value * (if (long) 80f else 32f)
                            val sc = 0.82f + 0.18f * draw.value
                            scaleX = sc; scaleY = sc
                            alpha = draw.value
                        }
                    )
                    // Core
                    Canvas(modifier = Modifier.size(56.dp)) {
                        drawCircle(
                            Brush.radialGradient(listOf(color.copy(alpha = 0.6f * pulse), Color.Transparent)),
                        )
                        drawCircle(Color.White.copy(alpha = pulse), radius = 9.dp.toPx())
                    }
                }
                Spacer(Modifier.height(40.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        alpha = text.value
                        translationY = (1f - text.value) * 8.dp.toPx()
                    }
                ) {
                    Text(name, color = Color(0xFFF2F2F3), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 6.sp)
                    Text(role, color = color, fontSize = 13.sp, fontFamily = com.dking.crocapp.ui.theme.MonoFamily, letterSpacing = 3.sp)
                }
            }
            Text(
                "Tap to skip",
                color = Color(0xFF8A8A92), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
            )
        }
    }
}
