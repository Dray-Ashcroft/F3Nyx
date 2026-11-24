package com.dking.crocapp.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dking.crocapp.R
import com.dking.crocapp.croc.CrocTransferState
import com.dking.crocapp.ui.components.EngineBadge
import com.dking.crocapp.ui.components.GlassCard
import com.dking.crocapp.ui.components.TransferProgressCard
import com.dking.crocapp.ui.components.formatBytes
import com.dking.crocapp.ui.components.progressBorder

/* Shared Show QR / Scan QR pieces used by Transmit and Retrieve. */

@Composable
fun QrRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val gold = MaterialTheme.colorScheme.secondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            enabled = enabled,
            shape = androidx.compose.foundation.shape.CircleShape,
            color = gold.copy(alpha = if (enabled) 0.14f else 0.06f),
            contentColor = if (enabled) gold else gold.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = if (enabled) 0.32f else 0.12f)),
            // The label sits below; screen readers still hear the button's name.
            modifier = Modifier.size(58.dp).semantics { contentDescription = label }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
        )
    }
}

@Composable
fun HelpDot(open: Boolean, onToggle: () -> Unit) {
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 360f else 0f, label = "helpTurn")
    val cs = MaterialTheme.colorScheme
    val desc = stringResource(if (open) R.string.receive_qr_help_hide else R.string.receive_qr_help_show)
    androidx.compose.material3.Surface(
        onClick = onToggle,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (open) cs.inverseSurface else androidx.compose.ui.graphics.Color.Transparent,
        contentColor = if (open) cs.inverseOnSurface else cs.onSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.size(30.dp).semantics { contentDescription = desc }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.Info, contentDescription = null,
                modifier = Modifier.size(17.dp).graphicsLayer { rotationZ = turn }
            )
        }
    }
}

@Composable
fun QrHint(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceContainerHighest.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = cs.secondary, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold, color = cs.onSurface))
                append(title)
                pop()
                append(" — ")
                append(body)
            },
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant
        )
    }
}

/** Phone "Show QR": the code as a big QR; closes itself when the sender connects. */
@Composable
fun ShowQrDialog(
    title: String,
    code: String,
    qrData: String,
    waiting: String,
    steps: String,
    onNewCode: () -> Unit,
    onCopy: () -> Unit,
    onClose: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ShowQrContent(title, code, qrData, waiting, steps, onNewCode, onCopy, onClose)
    }
}

@Composable
fun ShowQrContent(
    title: String,
    code: String,
    qrData: String,
    waiting: String,
    steps: String,
    onNewCode: () -> Unit,
    onCopy: () -> Unit,
    onClose: () -> Unit
) {
    run {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val qr = minOf(maxWidth - 96.dp, maxHeight * 0.42f, 320.dp)
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        GlassRoundButton(
                            icon = Icons.Rounded.Close,
                            label = stringResource(R.string.action_close),
                            onClick = onClose
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    QrCodeImage(
                        data = qrData,
                        size = qr, padding = 16.dp, expandable = false, showExpandHint = false
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        code,
                        fontFamily = com.dking.crocapp.ui.theme.MonoFamily,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.height(12.dp))
                    WaitingDot(waiting)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        LabeledRound(Icons.Rounded.Refresh, stringResource(R.string.panel_new_code_now), onNewCode)
                        LabeledRound(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy), onCopy)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        steps,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingDot(text: String) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "wait")
    val a by t.animateFloat(
        1f, 0.3f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "waitDot"
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(9.dp).graphicsLayer { alpha = a }
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(androidx.compose.ui.graphics.Color(0xFF30C26B))
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LabeledRound(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassRoundButton(icon = icon, label = label, onClick = onClick, size = 48.dp)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
