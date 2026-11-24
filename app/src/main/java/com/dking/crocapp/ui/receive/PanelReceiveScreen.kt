package com.dking.crocapp.ui.receive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dking.crocapp.R
import com.dking.crocapp.croc.CrocTransferState
import com.dking.crocapp.ui.components.GaugeStat
import com.dking.crocapp.ui.components.GlassCard
import com.dking.crocapp.ui.components.GlassRoundButton
import com.dking.crocapp.ui.components.QrCodeImage
import com.dking.crocapp.ui.components.ReactorGauge
import com.dking.crocapp.ui.components.SuitCue
import com.dking.crocapp.ui.components.SuitScene
import com.dking.crocapp.ui.components.UiEffects
import com.dking.crocapp.ui.components.formatBytes
import com.dking.crocapp.ui.components.suitPlaying
import com.dking.crocapp.ui.theme.MonoFamily
import com.dking.crocapp.util.QrCodeParser
import kotlinx.coroutines.delay

/**
 * Friday panel: a receive-only screen for classroom smart boards.
 *
 * Two panes. Left, "Connect": the QR, the code and round buttons (Big QR, New code,
 * Animation). Right: the Stage (steps while waiting, the progress ring while a file
 * arrives, the suit landing when it is done) and "Received today" with Files / Text tabs.
 *
 * The panel keeps its code for 10 minutes and listens again within a couple of
 * seconds after each file, so phones can send back to back without rescanning.
 */
@Composable
fun PanelReceiveScreen(
    viewModel: ReceiveViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Run the waiting session only while this screen is showing.
    DisposableEffect(Unit) {
        viewModel.startPanelSession()
        onDispose { viewModel.stopPanelSession() }
    }

    // Clock for the "new code in N min" hint (no need for per-second redraws).
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }

    val state = uiState.transferState
    val code = uiState.codePhrase
    var bigQr by remember { mutableStateOf(false) }

    // Remember the last finished transfer for the Stage, since the panel starts
    // listening again a moment later.
    var lastDoneAt by remember { mutableLongStateOf(0L) }
    var lastDoneName by remember { mutableStateOf("") }
    if (state is CrocTransferState.Completed) {
        LaunchedEffect(state) {
            lastDoneAt = System.currentTimeMillis()
            lastDoneName = state.fileNames.firstOrNull().orEmpty()
            now = lastDoneAt
            if (UiEffects.panelAnimation) SuitCue.play(launch = false, source = System.identityHashCode(state))
        }
    }
    // Refresh "now" once the "Retrieved" note should give way to the steps again.
    LaunchedEffect(lastDoneAt) {
        if (lastDoneAt > 0) { delay(RECENT_MS); now = System.currentTimeMillis() }
    }
    val recentlyDone = state is CrocTransferState.Completed || (lastDoneAt > 0 && now - lastDoneAt < RECENT_MS)
    // The Stage is compact while waiting and grows for progress, the suit and "Retrieved".
    val stageBusy = state is CrocTransferState.Transferring || recentlyDone || SuitCue.request != null
    val latestName = uiState.sessionFiles.firstOrNull()?.name ?: lastDoneName

    val minsLeft = (((uiState.panelCodeIssuedAt + PANEL_CODE_LIFETIME_MS) - now) / 60_000L + 1)
        .coerceIn(1, PANEL_CODE_LIFETIME_MS / 60_000L).toInt()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val narrow = maxWidth < 600.dp
    val margin = if (narrow) 16.dp else 28.dp
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top bar: logo · centred phone|panel switch · status + icons ──
        Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            com.dking.crocapp.ui.components.AppTopBar(
                onHistory = onNavigateToHistory,
                onSettings = onNavigateToSettings,
                margin = margin,
                modifier = Modifier.align(Alignment.Center)
            )
            // Big panels: the status pill sits left of the icons, clear of the centred switch.
            if (!narrow) {
                Box(Modifier.align(Alignment.CenterEnd).padding(end = margin + 96.dp)) {
                    StatusPill(state = state, minsLeft = minsLeft)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = margin, end = margin, top = 20.dp, bottom = 24.dp)
        ) {
            val wide = maxWidth > 760.dp
            val qrSize = if (wide) min(maxHeight - 290.dp, 400.dp).coerceAtLeast(180.dp) else min(maxWidth - 80.dp, 320.dp)

            val connect: @Composable (Modifier) -> Unit = { m ->
                ConnectCard(
                    code = code,
                    qrSize = qrSize,
                    animationOn = UiEffects.panelAnimation,
                    onBigQr = { bigQr = true },
                    onNewCode = { viewModel.refreshPanelCode() },
                    onToggleAnimation = { UiEffects.setPanelAnimation(viewModel.getApplication(), !UiEffects.panelAnimation) },
                    modifier = m
                )
            }
            val stage: @Composable (Modifier) -> Unit = { m ->
                StageCard(
                    busy = stageBusy,
                    state = state,
                    recentlyDone = recentlyDone,
                    latestName = latestName,
                    onCancel = { viewModel.cancelTransfer() },
                    modifier = m
                )
            }
            val received: @Composable (Modifier) -> Unit = { m ->
                ReceivedTodayCard(files = uiState.sessionFiles, texts = uiState.sessionTexts, modifier = m)
            }

            if (wide) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    connect(Modifier.width(qrSize + 64.dp).fillMaxHeight())
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        stage(Modifier.fillMaxWidth().then(if (stageBusy) Modifier.height(260.dp) else Modifier))
                        received(Modifier.fillMaxWidth().weight(1f))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    connect(Modifier.fillMaxWidth())
                    stage(Modifier.fillMaxWidth().then(if (stageBusy) Modifier.height(260.dp) else Modifier))
                    received(Modifier.fillMaxWidth().height(360.dp))
                }
            }
        }
    }

    }
    if (bigQr && code.isNotBlank()) {
        BigQrDialog(code = code, onClose = { bigQr = false })
    }
}

private const val RECENT_MS = 8_000L

@Composable
private fun StatusPill(state: CrocTransferState, minsLeft: Int) {
    val (dot, text) = when (state) {
        is CrocTransferState.Transferring -> Color(0xFFD97706) to stringResource(R.string.panel_pill_receiving)
        is CrocTransferState.Preparing, is CrocTransferState.WaitingForPeer, is CrocTransferState.Completed ->
            Color(0xFF1F8A4C) to stringResource(R.string.panel_pill_ready, minsLeft)
        is CrocTransferState.LegacyFallbackAvailable -> Color(0xFF8A8A92) to stringResource(R.string.panel_connecting)
        else -> Color(0xFF8A8A92) to stringResource(R.string.panel_reconnecting)
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.height(36.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PanelCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
        content = content
    )
}

@Composable
private fun ConnectCard(
    code: String,
    qrSize: Dp,
    animationOn: Boolean,
    onBigQr: () -> Unit,
    onNewCode: () -> Unit,
    onToggleAnimation: () -> Unit,
    modifier: Modifier
) {
    PanelCard(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                stringResource(R.string.panel_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            AnimatedContent(
                targetState = code,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "panelQr"
            ) { shownCode ->
                if (shownCode.isNotBlank()) {
                    QrCodeImage(
                        data = QrCodeParser.sendDeepLink(shownCode),
                        size = qrSize,
                        padding = 14.dp,
                        expandable = false,
                        showExpandHint = false
                    )
                } else {
                    Box(Modifier.size(qrSize))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // Fit the code on one line: mono glyphs are ~0.6 em wide.
                val fit = (maxWidth.value / (code.length.coerceAtLeast(8) * 0.62f)).coerceIn(18f, 34f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.panel_code_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = code,
                        fontFamily = MonoFamily,
                        fontSize = fit.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                RoundWithCaption(Icons.Rounded.Fullscreen, stringResource(R.string.panel_big_qr), onBigQr)
                RoundWithCaption(Icons.Rounded.Refresh, stringResource(R.string.panel_new_code_now), onNewCode)
                RoundWithCaption(
                    if (animationOn) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome,
                    stringResource(if (animationOn) R.string.panel_animation_on else R.string.panel_animation_off),
                    onToggleAnimation
                )
            }
        }
    }
}

@Composable
private fun RoundWithCaption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassRoundButton(icon = icon, label = label, onClick = onClick, size = 52.dp)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StageCard(
    busy: Boolean,
    state: CrocTransferState,
    recentlyDone: Boolean,
    latestName: String,
    onCancel: () -> Unit,
    modifier: Modifier
) {
    PanelCard(modifier) {
        Row(
            // Idle: hug the content, so "Received today" keeps the rest of the column.
            modifier = Modifier.fillMaxWidth().then(if (busy) Modifier.fillMaxHeight() else Modifier)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val playing = suitPlaying()
            when {
                state is CrocTransferState.Transferring -> {
                    ReactorGauge(
                        progress = state.progress,
                        label = stringResource(R.string.gauge_receiving),
                        size = 190.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            state.fileName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GaugeStat(
                                label = stringResource(R.string.stat_speed),
                                value = if (state.speedBytesPerSec > 0) formatBytes(state.speedBytesPerSec) + "/s" else "--",
                                modifier = Modifier.weight(1f)
                            )
                            GaugeStat(
                                label = stringResource(R.string.stat_received),
                                value = formatBytes(state.bytesTransferred) + " / " + formatBytes(state.totalBytes),
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                        GlassRoundButton(icon = Icons.Rounded.Close, label = stringResource(R.string.action_cancel), onClick = onCancel, size = 48.dp)
                    }
                }
                playing || recentlyDone -> {
                    // The suit lands here, inside the Stage, never over the code.
                    Box(
                        modifier = Modifier.width(200.dp).fillMaxHeight().clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playing) SuitScene(modifier = Modifier.fillMaxSize(), suitW = 118.dp, bottomGap = 6.dp)
                        else Icon(
                            Icons.Rounded.CheckCircle, contentDescription = null,
                            tint = Color(0xFF30C26B), modifier = Modifier.size(72.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.panel_received),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF1F8A4C)
                        )
                        Text(
                            latestName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.panel_ready_next),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Waiting: one calm line; the steps open only when (i) is pressed.
                    WaitingHelp(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun WaitingHelp(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val helpShow = stringResource(R.string.panel_help_show)
    val helpHide = stringResource(R.string.panel_help_hide)
    val turn by androidx.compose.animation.core.animateFloatAsState(
        if (open) 180f else 0f,
        androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "infoTurn"
    )
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "dot").animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2000)),
        label = "pulse"
    )
    Column(modifier = modifier.animateContentSize(androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
                if (UiEffects.full) Box(
                    Modifier.size((10 + 12 * pulse).dp).clip(CircleShape)
                        .background(Color(0xFF1F8A4C).copy(alpha = 0.35f * (1f - pulse)))
                )
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF1F8A4C)))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.panel_waiting), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.panel_waiting_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                onClick = { open = !open },
                shape = CircleShape,
                color = if (open) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (open) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(44.dp).semantics {
                    contentDescription = if (open) helpHide else helpShow
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Info, contentDescription = null,
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = turn }
                    )
                }
            }
        }
        if (open) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Step(1, stringResource(R.string.panel_step_1))
                Step(2, stringResource(R.string.panel_step_2))
                Step(3, stringResource(R.string.panel_step_3))
            }
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        com.dking.crocapp.ui.components.StepNumber(n)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReceivedTodayCard(files: List<ReceivedFile>, texts: List<String>, modifier: Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableIntStateOf(0) }
    PanelCard(modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.panel_received_today),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Downloads/F3Nyx",
                    fontFamily = MonoFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Two tabs: Files | Text
            Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TabPill(stringResource(R.string.panel_tab_files, files.size), tab == 0, Modifier.weight(1f)) { tab = 0 }
                    TabPill(stringResource(R.string.panel_tab_text, texts.size), tab == 1, Modifier.weight(1f)) { tab = 1 }
                }
            }
            if (tab == 0) {
                if (files.isEmpty()) EmptyNote(stringResource(R.string.panel_none_files))
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(files, key = { it.uri.toString() }) { file ->
                        ReceivedFileRow(
                            file = file,
                            onOpen = { openReceivedFile(context, file) },
                            onShare = { shareReceivedFile(context, file) }
                        )
                    }
                }
            } else {
                if (texts.isEmpty()) EmptyNote(stringResource(R.string.panel_none_text))
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(texts) { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(start = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { clipboard.setText(AnnotatedString(t)) }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.panel_copy_text))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (!selected) Color.Transparent else if (com.dking.crocapp.ui.theme.LocalMonet.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseSurface,
        contentColor = if (!selected) MaterialTheme.colorScheme.onSurfaceVariant else if (com.dking.crocapp.ui.theme.LocalMonet.current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.inverseOnSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        textAlign = TextAlign.Center
    )
}

/** Full-screen QR with the code in very large letters, for the back of the class. */
@Composable
private fun BigQrDialog(code: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val landscape = maxWidth > maxHeight
                val qr = if (landscape) min(maxHeight - 80.dp, maxWidth * 0.55f) else min(maxWidth - 48.dp, maxHeight * 0.55f)
                val codeSize = if (landscape) 72.sp else 40.sp
                val words: @Composable () -> Unit = {
                    Column(horizontalAlignment = if (landscape) Alignment.Start else Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.panel_scan_with),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            code.replace('-', '\n'),
                            fontFamily = MonoFamily,
                            fontSize = codeSize,
                            lineHeight = codeSize * 1.1f,
                            fontWeight = FontWeight.Medium,
                            textAlign = if (landscape) TextAlign.Start else TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (landscape) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QrCodeImage(data = QrCodeParser.sendDeepLink(code), size = qr, padding = 18.dp, expandable = false, showExpandHint = false)
                        words()
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QrCodeImage(data = QrCodeParser.sendDeepLink(code), size = qr, padding = 18.dp, expandable = false, showExpandHint = false)
                        words()
                    }
                }
                GlassRoundButton(
                    icon = Icons.Rounded.Close,
                    label = stringResource(R.string.action_close),
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
                )
            }
        }
    }
}
