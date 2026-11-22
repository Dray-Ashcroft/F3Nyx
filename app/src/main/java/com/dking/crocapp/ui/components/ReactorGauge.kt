package com.dking.crocapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dking.crocapp.ui.theme.MonoFamily

private val GaugeRed = Color(0xFFE5484D)
private val GaugeAmber = Color(0xFFF5A524)
private val GaugeGreen = Color(0xFF30C26B)
private val CoilOff = Color(0xFF18181C)
private val CoilOffStroke = Color(0xFF2A2A30)
private val Track = Color(0xFF24242A)

/** Red → amber → green as the transfer fills. */
fun gaugeColor(progress: Float): Color {
    val p = progress.coerceIn(0f, 1f)
    return if (p <= 0.5f) lerp(GaugeRed, GaugeAmber, p / 0.5f) else lerp(GaugeAmber, GaugeGreen, (p - 0.5f) / 0.5f)
}

/**
 * Reactor-style progress: 10 coil blocks (10% each) around an inner ring, with a
 * soft core glow. Flat shapes only; colour shifts red → amber → green.
 *
 * @param progress 0..1, or null while linking (an arc sweeps around instead).
 * @param complete draws a tick in the centre instead of the percentage.
 */
@Composable
fun ReactorGauge(
    progress: Float?,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    complete: Boolean = false
) {
    val linking = progress == null
    val p = (progress ?: 0f).coerceIn(0f, 1f)
    val color = if (complete) GaugeGreen else gaugeColor(p)

    // Only animate while linking (indeterminate). Static otherwise.
    val sweep = if (linking && UiEffects.full) {
        rememberInfiniteTransition(label = "link").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
            label = "sweep"
        ).value
    } else 0f

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.minDimension
            val c = Offset(w / 2f, w / 2f)
            val u = w / 260f // design units

            // Faint outer hairline
            drawCircle(Color(0xFF202026), radius = 126f * u, center = c, style = Stroke(1f * u))

            // 10 coils
            val lit = p * 10f
            val coilSize = Size(24f * u, 30f * u)
            for (i in 0 until 10) {
                val on = complete || i + 1 <= lit
                val part = !on && i < lit
                rotate(i * 36f, pivot = c) {
                    val tl = Offset(c.x - coilSize.width / 2f, 10f * u)
                    drawRoundRect(
                        color = when {
                            on -> color.copy(alpha = 0.85f)
                            part -> color.copy(alpha = 0.35f)
                            else -> CoilOff
                        },
                        topLeft = tl, size = coilSize, cornerRadius = CornerRadius(5f * u)
                    )
                    drawRoundRect(
                        color = if (on || part) color.copy(alpha = 0.9f) else CoilOffStroke,
                        topLeft = tl, size = coilSize, cornerRadius = CornerRadius(5f * u),
                        style = Stroke(1f * u)
                    )
                }
            }

            // Inner progress ring
            val r = 82f * u
            val ringTl = Offset(c.x - r, c.y - r)
            val ringSize = Size(r * 2, r * 2)
            drawCircle(Track, radius = r, center = c, style = Stroke(4f * u))
            if (linking) {
                drawArc(color, startAngle = sweep - 90f, sweepAngle = 70f, useCenter = false,
                    topLeft = ringTl, size = ringSize, style = Stroke(4f * u, cap = StrokeCap.Round))
            } else {
                drawArc(color, startAngle = -90f, sweepAngle = 360f * (if (complete) 1f else p), useCenter = false,
                    topLeft = ringTl, size = ringSize, style = Stroke(4f * u, cap = StrokeCap.Round))
            }
            drawCircle(Color(0xFF1E1E23), radius = 68f * u, center = c, style = Stroke(1f * u))
            drawCircle(color.copy(alpha = 0.08f), radius = 62f * u, center = c)

            if (complete) {
                val tick = Path().apply {
                    moveTo(c.x - 22f * u, c.y + 1f * u)
                    lineTo(c.x - 6f * u, c.y + 17f * u)
                    lineTo(c.x + 24f * u, c.y - 15f * u)
                }
                drawPath(tick, GaugeGreen, style = Stroke(6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        if (!complete) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (linking) "--" else (p * 100).toInt().toString(),
                        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
                        fontSize = (size.value * 0.18f).sp, letterSpacing = (-1).sp,
                        color = Color(0xFFF2F2F3)
                    )
                    Text("%", fontFamily = MonoFamily, fontSize = (size.value * 0.065f).sp, color = Color(0xFF8A8A92))
                }
                Text(label, fontFamily = MonoFamily, fontSize = 11.sp, letterSpacing = 2.sp, color = color)
            }
        }
    }
}

/** Small stat block: grey label over a mono value. */
@Composable
fun GaugeStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = Color(0xFF8A8A92))
        Text(value, fontFamily = MonoFamily, fontSize = 15.sp, color = Color(0xFFF2F2F3))
    }
}
