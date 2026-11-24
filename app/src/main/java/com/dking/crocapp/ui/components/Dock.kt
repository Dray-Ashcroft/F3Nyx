package com.dking.crocapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The main action a home screen offers to the dock (round red button on the right). */
data class DockAction(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean,
    val onClick: () -> Unit
)

/** Home screens register their main action here; the dock shows the one for the visible tab. */
object DockActions {
    val actions = mutableStateMapOf<String, DockAction>()
}

/**
 * Registers [key]'s main action while this composable is in composition.
 * Drop-in for the old full-width action bar: renders nothing itself.
 */
@Composable
fun RegisterDockAction(
    key: String,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") containerColor: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") contentColor: Color = Color.Unspecified
) {
    val click by rememberUpdatedState(onClick)
    SideEffect { DockActions.actions[key] = DockAction(icon, text, enabled) { click() } }
    DisposableEffect(key) { onDispose { DockActions.actions.remove(key) } }
}

private val GlassFill = Color(0xB81C1C20)
private val GlassEdge = Color(0x12FFFFFF)
// Light-mode glass: frosted white with a soft grey edge.
private val GlassFillLight = Color(0xD9FFFFFF)
private val GlassEdgeLight = Color(0x1F000000)

@Composable
private fun isLight() = MaterialTheme.colorScheme.surface.luminance() > 0.5f

/** Round glass button: grey (secondary) or red (primary). Icon only; [label] is read by screen readers. */
@Composable
fun GlassRoundButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    size: Dp = if (primary) 64.dp else 56.dp
) {
    val a by animateFloatAsState(if (enabled) 1f else 0.45f, tween(200), label = "enabled")
    val light = isLight()
    val monet = com.dking.crocapp.ui.theme.LocalMonet.current
    val p = MaterialTheme.colorScheme.primary
    val fill = when {
        primary && monet -> Brush.linearGradient(listOf(p.copy(alpha = 0.95f), androidx.compose.ui.graphics.lerp(p, Color.Black, 0.35f).copy(alpha = 0.95f)))
        primary -> Brush.linearGradient(listOf(Color(0xEBE5484D), Color(0xEB8F1C16)))
        light -> Brush.linearGradient(listOf(Color(0xF2FFFFFF), Color(0xE6F1F1F4)))
        else -> Brush.linearGradient(listOf(Color(0xCC2C2C32), Color(0xCC1E1E22)))
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = if (primary && monet) MaterialTheme.colorScheme.onPrimary else if (primary || !light) Color.White else Color(0xFF2A2A30),
        border = BorderStroke(1.dp, when { primary -> Color(0x2EFFFFFF); light -> GlassEdgeLight; else -> Color(0x1AFFFFFF) }),
        modifier = modifier
            .size(size)
            .alpha(a)
            .shadow(if (primary) 14.dp else 8.dp, CircleShape, ambientColor = if (primary) (if (monet) p else Color(0xFFB3261E)) else Color.Black, spotColor = if (primary) (if (monet) p else Color(0xFFB3261E)) else Color.Black)
            .semantics { contentDescription = label }
    ) {
        Box(Modifier.background(fill), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(if (primary) 28.dp else 24.dp))
        }
    }
}

/**
 * Bottom dock: a small glass pill with the two tabs as round icon buttons on the
 * left, and the visible screen's main action as a round red button on the right.
 */
@Composable
fun HomeDock(
    selected: Int,
    onSelect: (Int) -> Unit,
    transmitLabel: String,
    retrieveLabel: String,
    actionKey: String,
    modifier: Modifier = Modifier
) {
    val action = DockActions.actions[actionKey]
    val light = isLight()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val density = LocalDensity.current
        val drop = rememberDroplet()
        val ring = if (light) Color(0xFF6B6B73) else Color(0xFFB0B0B8)
        fun tap(i: Int) {
            if (i != selected) {
                with(density) { drop.drop(Offset((6 + i * 60 + 26).dp.toPx(), (6 + 26).dp.toPx()), i) }
            }
            onSelect(i)
        }
        Row(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(40.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .background(if (light) GlassFillLight else GlassFill, RoundedCornerShape(40.dp))
                .border(1.dp, if (light) GlassEdgeLight else GlassEdge, RoundedCornerShape(40.dp))
                .clip(RoundedCornerShape(40.dp))
                .dropletRings(drop, ring, with(density) { 64.dp.toPx() })
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabCircle(Icons.Rounded.ArrowUpward, transmitLabel, selected == 0, Modifier.dropletSquash(drop, 0)) { tap(0) }
            TabCircle(Icons.Rounded.ArrowDownward, retrieveLabel, selected == 1, Modifier.dropletSquash(drop, 1)) { tap(1) }
        }
        AnimatedVisibility(
            visible = action != null,
            enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.8f),
            exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.8f)
        ) {
            if (action != null) {
                GlassRoundButton(
                    icon = action.icon,
                    label = action.label,
                    onClick = action.onClick,
                    enabled = action.enabled,
                    primary = true,
                    size = 64.dp
                )
            }
        }
    }
}

@Composable
private fun TabCircle(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = when { !selected -> Color.Transparent; com.dking.crocapp.ui.theme.LocalMonet.current -> MaterialTheme.colorScheme.secondaryContainer; isLight() -> Color(0xFFECECEF); else -> Color(0xFF2A2A30) },
        contentColor = when { selected && com.dking.crocapp.ui.theme.LocalMonet.current -> MaterialTheme.colorScheme.onSecondaryContainer; selected && isLight() -> Color(0xFF1B1B1F); selected -> Color(0xFFF2F2F3); isLight() -> Color(0xFF6B6B73); else -> Color(0xFF8A8A92) },
        border = if (selected) BorderStroke(1.dp, if (isLight()) GlassEdgeLight else Color(0x1AFFFFFF)) else null,
        modifier = modifier
            .size(52.dp)
            .semantics { contentDescription = label }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}
