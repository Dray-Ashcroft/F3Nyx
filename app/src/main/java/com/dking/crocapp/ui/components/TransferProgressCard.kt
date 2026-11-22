package com.dking.crocapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dking.crocapp.R
import com.dking.crocapp.croc.CrocEngine
import com.dking.crocapp.croc.CrocTransferState

@Composable
fun TransferProgressCard(
    state: CrocTransferState,
    isSending: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onRetryLegacy: (() -> Unit)? = null,
    onSwitchToLegacy: (() -> Unit)? = null,
    activeEngine: CrocEngine = CrocEngine.CURRENT
) {
    val isLegacy = activeEngine == CrocEngine.LEGACY

    // Stealth suit cue once per finished transfer.
    if (state is CrocTransferState.Completed) {
        androidx.compose.runtime.LaunchedEffect(state) {
            SuitCue.play(launch = isSending, source = System.identityHashCode(state))
        }
    }

    // Let the header indicator react while a transfer is live.
    val live = state is CrocTransferState.Preparing || state is CrocTransferState.WaitingForPeer || state is CrocTransferState.Transferring
    androidx.compose.runtime.DisposableEffect(live) {
        if (live) TransferActivity.begin()
        onDispose { if (live) TransferActivity.end() }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (state) {
                is CrocTransferState.Preparing -> {
                    ReactorGauge(progress = null, label = "LINKING", modifier = Modifier.align(Alignment.CenterHorizontally))
                    TransferHeader(
                        icon = if (isSending) Icons.Rounded.CloudUpload else Icons.Rounded.Download,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        title = if (isSending) stringResource(R.string.transfer_preparing_upload) else stringResource(R.string.transfer_preparing_download),
                        subtitle = null,
                        showLegacyBadge = isLegacy
                    )
                }

                is CrocTransferState.WaitingForPeer -> {
                    ReactorGauge(progress = null, label = "LINKING", modifier = Modifier.align(Alignment.CenterHorizontally))
                    TransferHeader(
                        icon = if (isSending) Icons.Rounded.CloudUpload else Icons.Rounded.Download,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        title = if (isSending) stringResource(R.string.transfer_waiting_peer) else stringResource(R.string.transfer_connecting),
                        subtitle = if (isSending) stringResource(R.string.transfer_share_code_hint) else stringResource(R.string.transfer_verifying_code),
                        showLegacyBadge = isLegacy
                    )
                    GlassRoundButton(
                        icon = Icons.Rounded.Close,
                        label = stringResource(R.string.action_cancel),
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is CrocTransferState.Transferring -> {
                    val subtitle = buildString {
                        append("${state.fileName} (${state.currentFile}/${state.totalFiles})")
                        if (state.peerIp.isNotBlank()) {
                            append(" • ${state.peerIp}")
                        }
                    }
                    TransferHeader(
                        icon = if (isSending) Icons.Rounded.CloudUpload else Icons.Rounded.Download,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        title = if (isSending) stringResource(R.string.transfer_uploading) else stringResource(R.string.transfer_downloading),
                        subtitle = subtitle,
                        showLegacyBadge = isLegacy
                    )

                    val animatedProgress by animateFloatAsState(
                        targetValue = state.progress,
                        animationSpec = tween(300),
                        label = "progress"
                    )

                    ReactorGauge(
                        progress = animatedProgress,
                        label = if (isSending) "TRANSMITTING" else "RETRIEVING",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GaugeStat(
                            label = "Speed",
                            value = if (state.speedBytesPerSec > 0) formatBytes(state.speedBytesPerSec) + "/s" else "--",
                            modifier = Modifier.weight(1f)
                        )
                        GaugeStat(
                            label = if (isSending) "Sent" else "Received",
                            value = formatBytes(state.bytesTransferred) + " / " + formatBytes(state.totalBytes),
                            modifier = Modifier.weight(1.4f)
                        )
                        GaugeStat(
                            label = "Left",
                            value = if (state.etaSeconds > 0) "%02d:%02d".format(state.etaSeconds / 60, state.etaSeconds % 60) else "--",
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    GlassRoundButton(
                        icon = Icons.Rounded.Close,
                        label = stringResource(R.string.action_cancel),
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is CrocTransferState.Completed -> {
                    ReactorGauge(progress = 1f, label = "COMPLETE", complete = true, size = 160.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    val count = state.fileCount
                    val subtitle = buildString {
                        append("$count file${if (count != 1) "s" else ""} — ${formatBytes(state.totalBytes)}")
                        if (state.peerIp.isNotBlank()) {
                            append(" • ${state.peerIp}")
                        }
                    }
                    TransferHeader(
                        icon = Icons.Rounded.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        title = if (isSending) stringResource(R.string.transfer_upload_complete) else stringResource(R.string.transfer_download_complete),
                        subtitle = subtitle,
                        titleColor = MaterialTheme.colorScheme.primary,
                        showLegacyBadge = isLegacy
                    )
                }

                is CrocTransferState.LegacyFallbackAvailable -> {
                    TransferHeader(
                        icon = Icons.Rounded.History,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                        title = stringResource(R.string.transfer_legacy_fallback_title),
                        subtitle = if (isSending) {
                            stringResource(R.string.transfer_legacy_fallback_send_desc)
                        } else {
                            stringResource(R.string.transfer_legacy_fallback_receive_desc)
                        },
                        titleColor = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSending) {
                            if (onRetryLegacy != null) {
                                Button(
                                    onClick = onRetryLegacy,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.action_send_legacy),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Receiving flow
                            Button(
                                onClick = { (onSwitchToLegacy ?: onRetryLegacy)?.invoke() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_switch_legacy_receive),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is CrocTransferState.Error -> {
                    TransferHeader(
                        icon = Icons.Rounded.Error,
                        iconTint = MaterialTheme.colorScheme.error,
                        iconBackground = MaterialTheme.colorScheme.errorContainer,
                        title = stringResource(R.string.transfer_failed),
                        subtitle = state.message,
                        titleColor = MaterialTheme.colorScheme.error,
                        showLegacyBadge = isLegacy
                    )
                }

                is CrocTransferState.Cancelled -> {
                    TransferHeader(
                        icon = Icons.Rounded.Cancel,
                        iconTint = MaterialTheme.colorScheme.outline,
                        iconBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
                        title = stringResource(R.string.transfer_cancelled),
                        subtitle = null,
                        showLegacyBadge = isLegacy
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun TransferHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBackground: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String?,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    showLegacyBadge: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showLegacyBadge) {
            Spacer(modifier = Modifier.width(8.dp))
            EngineBadge(engine = CrocEngine.LEGACY)
        }
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}
