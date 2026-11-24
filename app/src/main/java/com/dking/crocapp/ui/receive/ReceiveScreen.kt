package com.dking.crocapp.ui.receive

import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dking.crocapp.ui.components.GlassCard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.border
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.dking.crocapp.R
import com.dking.crocapp.croc.CrocTransferState
import com.dking.crocapp.ui.components.EngineBadge
import com.dking.crocapp.ui.components.TransferProgressCard
import com.dking.crocapp.ui.components.formatBytes
import com.dking.crocapp.ui.components.progressBorder

private const val MAX_VISIBLE_FILES = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel = viewModel(),
    onOpenScanner: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Smart-panel mode: a big self-refreshing QR instead of the code form.
    if (uiState.panelMode) {
        PanelReceiveScreen(
            viewModel = viewModel,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToSettings = onNavigateToSettings
        )
        return
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var hideCodePhrase by remember { mutableStateOf(false) }
    var showCodeMenu by remember { mutableStateOf(false) }
    var showAllFiles by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showQrHelp by remember { mutableStateOf(false) }
    val codeFocusRequester = remember { FocusRequester() }
    var highlightCodeInput by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.codePhrase) {
        if (uiState.codePhrase.isNotBlank()) {
            highlightCodeInput = false
        }
    }

    // SAF folder picker for session-level location override
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Take persistent read/write permission
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            viewModel.setSessionOverrideLocation(uri)
        }
    }

    val isTransferActive = uiState.transferState is CrocTransferState.Preparing ||
            uiState.transferState is CrocTransferState.WaitingForPeer ||
            uiState.transferState is CrocTransferState.Transferring
    val isTransferFinished = uiState.transferState is CrocTransferState.Completed ||
            uiState.transferState is CrocTransferState.Error ||
            uiState.transferState is CrocTransferState.Cancelled
    val isLegacyFallback = uiState.transferState is CrocTransferState.LegacyFallbackAvailable
    val isWaitingForQr = uiState.transferState is CrocTransferState.Preparing ||
            uiState.transferState is CrocTransferState.WaitingForPeer
    // The QR closes by itself once the sender connects (or anything else ends the wait).
    LaunchedEffect(uiState.transferState) {
        if (showQr && !isWaitingForQr && uiState.transferState !is CrocTransferState.Idle) showQr = false
    }
    if (showQr && uiState.codePhrase.isNotBlank()) {
        ShowQrDialog(
            code = uiState.codePhrase,
            onNewCode = { viewModel.showQrAndWait() },
            onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(uiState.codePhrase))
                android.widget.Toast.makeText(context, context.getString(R.string.receive_code_copied), android.widget.Toast.LENGTH_SHORT).show()
            },
            onClose = { showQr = false; viewModel.stopWaitingForQr() }
        )
    }
    val showTransferSection = isTransferActive || isTransferFinished || isLegacyFallback
    val canReceive = uiState.codePhrase.isNotBlank()
    val fabLabel = when (uiState.transferState) {
        is CrocTransferState.Completed -> stringResource(R.string.receive_again)
        is CrocTransferState.LegacyFallbackAvailable -> stringResource(R.string.action_retry_legacy)
        is CrocTransferState.Error, CrocTransferState.Cancelled -> stringResource(R.string.action_retry)
        else -> stringResource(R.string.nav_receive)
    }

    // Animated progress for the code card border
    val transferProgress = when (val state = uiState.transferState) {
        is CrocTransferState.Transferring -> state.fileCountProgress
        is CrocTransferState.Completed -> 1f
        else -> 0f
    }
    val animatedBorderProgress by animateFloatAsState(
        targetValue = transferProgress,
        animationSpec = tween(400),
        label = "codeBorderProgress"
    )
    val showCodeBorder = false // progress now lives in the Stage card
    val borderColor = MaterialTheme.colorScheme.tertiary

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            com.dking.crocapp.ui.components.AppTopBar(onHistory = onNavigateToHistory, onSettings = onNavigateToSettings)
        },
        bottomBar = {
            // ③ Full-width main action (same behaviour as the old floating button:
            // with no code yet it points you to the code field).
            if (!isTransferActive) {
                com.dking.crocapp.ui.components.RegisterDockAction(
                    key = "receive",
                    text = fabLabel,
                    icon = if (isLegacyFallback) Icons.Rounded.History else Icons.Rounded.Download,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        if (isLegacyFallback) {
                            viewModel.retryWithLegacy()
                        } else {
                            if (uiState.codePhrase.isBlank()) {
                                highlightCodeInput = true
                                codeFocusRequester.requestFocus()
                            } else {
                                if (isTransferFinished) {
                                    viewModel.dismissTransferResult()
                                }
                                viewModel.startReceive()
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // ──── Card 1: code and where files go ────
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.extraLarge
            ) {
            androidx.compose.runtime.CompositionLocalProvider(com.dking.crocapp.ui.components.LocalFlatCards provides true) {
            // ──── Code Entry Card — with animated progress border ────
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (showCodeBorder) {
                            Modifier.progressBorder(
                                progress = animatedBorderProgress,
                                color = borderColor,
                                cornerRadius = 28.dp
                            )
                        } else if (highlightCodeInput && uiState.codePhrase.isBlank()) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.error,
                                shape = MaterialTheme.shapes.extraLarge
                            )
                        } else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.dking.crocapp.ui.components.StepTitle(
                        number = 1,
                        title = stringResource(R.string.send_secret_code)
                    ) {
                        if (uiState.defaultCodePhrase.isNotBlank() && uiState.defaultCodePhrase == uiState.codePhrase) {
                            Text(
                                text = stringResource(R.string.label_default),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        EngineBadge(engine = uiState.activeEngine)
                        HelpDot(open = showQrHelp, onToggle = { showQrHelp = !showQrHelp })
                        Box {
                            IconButton(onClick = { showCodeMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more_options))
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = showCodeMenu,
                                onDismissRequest = { showCodeMenu = false }
                            ) {
                                if (com.dking.crocapp.BuildConfig.BUNDLES_LEGACY_ENGINE) androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (uiState.activeEngine == com.dking.crocapp.croc.CrocEngine.LEGACY) stringResource(R.string.menu_use_latest_engine)
                                            else stringResource(R.string.menu_use_legacy_engine)
                                        )
                                    },
                                    enabled = !isTransferActive,
                                    onClick = { showCodeMenu = false; viewModel.toggleEngine() },
                                    leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_save)) },
                                    enabled = !isTransferActive && uiState.codePhrase.isNotBlank(),
                                    onClick = { showCodeMenu = false; viewModel.saveCurrentCode() },
                                    leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.receive_clear_all)) },
                                    enabled = !isTransferActive && (uiState.codePhrase.isNotBlank() || uiState.receivedFiles.isNotEmpty()),
                                    onClick = { showCodeMenu = false; viewModel.resetTransfer() },
                                    leadingIcon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(if (hideCodePhrase) stringResource(R.string.receive_show_code) else stringResource(R.string.receive_hide_code)) },
                                    onClick = { showCodeMenu = false; hideCodePhrase = !hideCodePhrase },
                                    leadingIcon = {
                                        Icon(
                                            if (hideCodePhrase) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Two ways to use a QR: show ours (the sender scans it) or scan the sender's.
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally)
                    ) {
                        QrRoundButton(
                            icon = Icons.Rounded.QrCode,
                            label = stringResource(R.string.receive_show_qr),
                            enabled = !isTransferActive || isWaitingForQr,
                            onClick = {
                                if (!isWaitingForQr) viewModel.showQrAndWait()
                                showQr = true
                            }
                        )
                        QrRoundButton(
                            icon = Icons.Rounded.QrCodeScanner,
                            label = stringResource(R.string.receive_scan_sender_qr),
                            enabled = !isTransferActive,
                            onClick = onOpenScanner
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showQrHelp,
                        enter = androidx.compose.animation.expandVertically() + fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            QrHint(Icons.Rounded.QrCode, stringResource(R.string.receive_show_qr), stringResource(R.string.receive_hint_show))
                            QrHint(Icons.Rounded.QrCodeScanner, stringResource(R.string.receive_scan_sender_qr), stringResource(R.string.receive_hint_scan))
                        }
                    }
                    Text(
                        text = stringResource(R.string.receive_or_type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.codePhrase,
                        onValueChange = {
                            highlightCodeInput = false
                            viewModel.updateCodePhrase(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(codeFocusRequester),
                        isError = highlightCodeInput && uiState.codePhrase.isBlank(),
                        enabled = !isTransferActive,
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.receive_code_placeholder)) },
                        shape = MaterialTheme.shapes.large,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = com.dking.crocapp.ui.theme.MonoFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        visualTransformation = if (hideCodePhrase) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    viewModel.updateCodePhrase(clip)
                                }) {
                                    Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.receive_paste))
                                }
                                if (uiState.codePhrase.isNotBlank() && !isTransferActive) {
                                    IconButton(onClick = { viewModel.updateCodePhrase("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
                                    }
                                }
                            }
                        }
                    )

                    // Saved codes — scrollable 2-row horizontal grid
                    if (uiState.savedCodePhrases.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.savedCodePhrases.chunked(2).forEach { column ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    column.forEach { savedCode ->
                                        SavedCodeChipWithActions(
                                            code = savedCode,
                                            enabled = !isTransferActive,
                                            onUse = { viewModel.startReceiveWithCode(savedCode) },
                                            onCopy = {
                                                clipboardManager.setText(
                                                    androidx.compose.ui.text.AnnotatedString(savedCode)
                                                )
                                            },
                                            onShare = {
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_TEXT, savedCode)
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "croc code")
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share code"))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            // ──── ② Where files are saved ────
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.dking.crocapp.ui.components.StepTitle(
                        number = 2,
                        title = stringResource(R.string.receive_save_to)
                    )
                    // ── Save location row ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            if (uiState.sessionOverrideUri != null) {
                                Text(
                                    text = stringResource(R.string.receive_save_to_session),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = uiState.receiveLocationLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (uiState.sessionOverrideUri != null) {
                            IconButton(
                                onClick = { viewModel.clearSessionOverrideLocation() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.RestartAlt,
                                    contentDescription = stringResource(R.string.receive_reset_location),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            enabled = !isTransferActive,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.receive_change_location),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                }
            }
            } // CompositionLocalProvider
            } // Card 1

            // Friendly hint while nothing is happening yet
            if (uiState.transferState is CrocTransferState.Idle && uiState.receivedFiles.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.receive_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ──── Card 2: the Stage (progress, result, suit) ────
            AnimatedVisibility(
                visible = showTransferSection,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransferProgressCard(
                        state = uiState.transferState,
                        isSending = false,
                        onCancel = { viewModel.cancelTransfer() },
                        onRetryLegacy = { viewModel.retryWithLegacy() },
                        onSwitchToLegacy = { viewModel.switchToLegacyForNextReceive() },
                        activeEngine = uiState.activeEngine
                    )
                    if (isTransferFinished) {
                        OutlinedButton(
                            onClick = { viewModel.dismissTransferResult() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                }
            }

            // ──── Received Files ────
            if (uiState.receivedFiles.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.receive_received_files),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${uiState.receivedFiles.size} file${if (uiState.receivedFiles.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val filesToShow = if (showAllFiles) uiState.receivedFiles
                        else uiState.receivedFiles.take(MAX_VISIBLE_FILES)

                        filesToShow.forEach { file ->
                            ReceivedFileRow(
                                file = file,
                                onOpen = { openReceivedFile(context, file) },
                                onShare = { shareReceivedFile(context, file) }
                            )
                        }

                        if (uiState.receivedFiles.size > MAX_VISIBLE_FILES) {
                            val remaining = uiState.receivedFiles.size - MAX_VISIBLE_FILES
                            AssistChip(
                                onClick = { showAllFiles = !showAllFiles },
                                label = {
                                    Text(
                                        if (showAllFiles) "Show less"
                                        else "+$remaining more file${if (remaining > 1) "s" else ""}"
                                    )
                                }
                            )
                        }

                        // Share all · open the Downloads folder
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
                        ) {
                            RoundAction(
                                icon = Icons.Rounded.Share,
                                label = stringResource(R.string.action_share_all),
                                onClick = { shareAllReceivedFiles(context, uiState.receivedFiles) }
                            )
                            RoundAction(
                                icon = Icons.Rounded.FolderOpen,
                                label = stringResource(R.string.action_open_folder),
                                onClick = { openDownloadsFolder(context) }
                            )
                        }
                    }
                }
            }

            // ──── Received Text ────
            val completedState = uiState.transferState as? CrocTransferState.Completed
            if (completedState?.receivedText != null) {
                val receivedText = completedState.receivedText ?: ""
                val isUrl = receivedText.trim().let {
                    it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.receive_received_text),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatBytes(completedState.totalBytes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isUrl) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(receivedText.trim())
                                                )
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.FolderOpen,
                                            contentDescription = "Open URL",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(
                                            androidx.compose.ui.text.AnnotatedString(receivedText)
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ContentPaste,
                                        contentDescription = "Copy text",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = receivedText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 12,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun ReceivedFileRow(
    file: ReceivedFile,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.savedLocation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOpen, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = stringResource(R.string.action_open_file),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onShare, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Rounded.Share,
                contentDescription = stringResource(R.string.action_share_file),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedCodeChipWithActions(
    code: String,
    enabled: Boolean,
    onUse: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = onUse,
            enabled = enabled,
            label = {
                Text(
                    code,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier.combinedClickable(
                enabled = enabled,
                onClick = onUse,
                onLongClick = { showMenu = true }
            )
        )

        androidx.compose.material3.DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(stringResource(R.string.receive_on_code)) },
                onClick = {
                    showMenu = false
                    onUse()
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(stringResource(R.string.action_copy)) },
                onClick = {
                    showMenu = false
                    onCopy()
                },
                leadingIcon = {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(stringResource(R.string.action_share)) },
                onClick = {
                    showMenu = false
                    onShare()
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}


/** Round glass action with a small caption underneath (completed-transfer actions). */
@Composable
private fun RoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        com.dking.crocapp.ui.components.GlassRoundButton(icon = icon, label = label, onClick = onClick, size = 52.dp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
private fun QrRoundButton(
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
            modifier = Modifier.size(58.dp)
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
private fun HelpDot(open: Boolean, onToggle: () -> Unit) {
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
private fun QrHint(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
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
private fun ShowQrDialog(code: String, onNewCode: () -> Unit, onCopy: () -> Unit, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val qr = minOf(maxWidth - 96.dp, maxHeight * 0.42f, 320.dp)
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.receive_qr_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        com.dking.crocapp.ui.components.GlassRoundButton(
                            icon = Icons.Rounded.Close,
                            label = stringResource(R.string.action_close),
                            onClick = onClose
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    com.dking.crocapp.ui.components.QrCodeImage(
                        data = com.dking.crocapp.util.QrCodeParser.sendDeepLink(code, once = true),
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
                    WaitingDot()
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        LabeledRound(Icons.Rounded.Refresh, stringResource(R.string.panel_new_code_now), onNewCode)
                        LabeledRound(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy), onCopy)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.receive_qr_steps),
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
private fun WaitingDot() {
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
        Text(stringResource(R.string.receive_qr_waiting), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LabeledRound(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        com.dking.crocapp.ui.components.GlassRoundButton(icon = icon, label = label, onClick = onClick, size = 48.dp)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
