package com.dking.crocapp.ui.send

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dking.crocapp.CrocApp
import com.dking.crocapp.croc.CodePhraseGenerator
import com.dking.crocapp.croc.CrocBinaryManager
import com.dking.crocapp.croc.CrocEngine
import com.dking.crocapp.croc.CrocProcess
import com.dking.crocapp.croc.CrocTransferState
import com.dking.crocapp.data.db.TransferHistory
import com.dking.crocapp.data.db.TransferStatus
import com.dking.crocapp.data.db.TransferType
import com.dking.crocapp.data.preferences.UserPreferencesRepository
import com.dking.crocapp.util.QrCodeParser
import com.dking.crocapp.util.StorageCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val localPath: String? = null
)

enum class SendMode { FILES, FOLDER, TEXT }
enum class DeliveryMode { DIRECT, STORE }

data class SendUiState(
    val selectedFiles: List<SelectedFile> = emptyList(),
    val selectedBytes: Long = 0,
    val codePhrase: String = "",
    val defaultCodePhrase: String = "",
    val savedCodePhrases: List<String> = emptyList(),
    val sendMode: SendMode = SendMode.FILES,
    val deliveryMode: DeliveryMode = DeliveryMode.DIRECT,
    val storeExpiration: String = "1d",
    val storeDownloads: Int = 1,
    val textToSend: String = "",
    val transferState: CrocTransferState = CrocTransferState.Idle,
    val activeEngine: CrocEngine = CrocEngine.CURRENT,
    // Folder mode
    val selectedFolderName: String? = null,
    val selectedFolderFileCount: Int = 0,
    val selectedFolderSize: Long = 0,
    val selectedFolderPath: String? = null
) {
    // Backward compat helper
    val isTextMode: Boolean get() = sendMode == SendMode.TEXT
    val isStoreMode: Boolean get() = deliveryMode == DeliveryMode.STORE

    val hasContent: Boolean
        get() = when (sendMode) {
            SendMode.FILES -> selectedFiles.isNotEmpty()
            SendMode.FOLDER -> selectedFolderPath != null
            SendMode.TEXT -> textToSend.isNotBlank()
        }
}

class SendViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CrocApp
    private val prefsRepo = UserPreferencesRepository(application)
    private val binaryManager = CrocBinaryManager(application)
    private val crocProcess = CrocProcess(application, binaryManager, prefsRepo)

    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    init {
        com.dking.crocapp.service.TransferNotifier.onCancelSend = { cancelTransfer() }
        viewModelScope.launch {
            crocProcess.state.collect { state ->
                _uiState.update { it.copy(transferState = state) }
                com.dking.crocapp.service.TransferNotifier.onState(getApplication(), state, sending = true)

                // Save to history on completion and clean up staging cache
                if (state is CrocTransferState.Completed) {
                    saveToHistory(state)
                    StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
                } else if (state is CrocTransferState.StoreCompleted) {
                    saveStoreToHistory(state)
                    StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
                } else if (state is CrocTransferState.Error) {
                    if (_uiState.value.sendMode != SendMode.FOLDER) {
                        StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
                    }
                }
            }
        }

        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        codePhrase = if (state.codePhrase.isBlank()) {
                            prefs.defaultCodePhrase.ifBlank { CodePhraseGenerator.generate() }
                        } else state.codePhrase,
                        defaultCodePhrase = prefs.defaultCodePhrase,
                        savedCodePhrases = prefs.savedCodePhrases,
                        storeExpiration = state.storeExpiration.ifBlank { prefs.defaultStoreExpiration },
                        storeDownloads = if (state.storeDownloads == 1) prefs.defaultStoreDownloads else state.storeDownloads
                    )
                }
            }
        }
    }

    fun addFiles(uris: List<Uri>) {
        val context = getApplication<CrocApp>()
        val newFiles = uris.mapNotNull { uri ->
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                var name = "unknown"
                var size = 0L
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) name = it.getString(nameIdx)
                        if (sizeIdx >= 0) size = it.getLong(sizeIdx)
                    }
                }
                SelectedFile(uri = uri, name = name, size = size.coerceAtLeast(0))
            } catch (e: Exception) {
                null
            }
        }
        _uiState.update { state ->
            val merged = (state.selectedFiles + newFiles).distinctBy { it.uri.toString() }
            state.copy(
                selectedFiles = merged,
                selectedBytes = merged.sumOf { it.size }
            )
        }
    }

    fun removeFile(file: SelectedFile) {
        _uiState.update { state ->
            val updatedFiles = state.selectedFiles.filter { it.uri != file.uri }
            state.copy(
                selectedFiles = updatedFiles,
                selectedBytes = updatedFiles.sumOf { it.size }
            )
        }
    }

    fun clearFiles() {
        _uiState.update { it.copy(selectedFiles = emptyList(), selectedBytes = 0) }
        StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
    }

    fun addFolder(treeUri: Uri) {
        // Copying a large folder on the main thread freezes the app (ANR), so stage it on IO.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                addFolderBlocking(treeUri)
            } catch (e: Exception) {
                crocProcess.reportError("Could not prepare folder: ${e.message ?: "unknown error"}")
            }
        }
    }

    private fun addFolderBlocking(treeUri: Uri) {
        val context = getApplication<CrocApp>()
        val treeDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val folderName = treeDoc.name ?: "folder"

        if (_uiState.value.deliveryMode == DeliveryMode.STORE) {
            val fileList = mutableListOf<Uri>()
            fun collectUris(doc: DocumentFile) {
                doc.listFiles().forEach { child ->
                    if (child.isDirectory) {
                        collectUris(child)
                    } else if (child.isFile) {
                        fileList.add(child.uri)
                    }
                }
            }
            collectUris(treeDoc)
            if (fileList.isNotEmpty()) {
                addFiles(fileList)
            }
            return
        }

        // Stage the entire folder tree into cache so croc can access it as a filesystem path
        val stagingDir = File(context.cacheDir, "croc-send-folder/$folderName").apply {
            deleteRecursively()
            mkdirs()
        }

        var fileCount = 0
        var totalSize = 0L

        fun copyTree(doc: DocumentFile, destDir: File) {
            doc.listFiles().forEach { child ->
                if (child.isDirectory) {
                    val subDir = File(destDir, child.name ?: "dir").apply { mkdirs() }
                    copyTree(child, subDir)
                } else if (child.isFile) {
                    val destFile = File(destDir, child.name ?: "file")
                    try {
                        context.contentResolver.openInputStream(child.uri)?.use { input ->
                            FileOutputStream(destFile).use { output -> input.copyTo(output) }
                        }
                        fileCount++
                        totalSize += destFile.length()
                    } catch (_: Exception) { }
                }
            }
        }

        copyTree(treeDoc, stagingDir)

        _uiState.update {
            it.copy(
                selectedFolderName = folderName,
                selectedFolderFileCount = fileCount,
                selectedFolderSize = totalSize,
                selectedFolderPath = stagingDir.absolutePath
            )
        }
    }

    fun clearFolder() {
        val path = _uiState.value.selectedFolderPath
        if (path != null) {
            File(path).deleteRecursively()
        }
        _uiState.update {
            it.copy(
                selectedFolderName = null,
                selectedFolderFileCount = 0,
                selectedFolderSize = 0,
                selectedFolderPath = null
            )
        }
        StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
    }

    fun updateCodePhrase(code: String) {
        val c = normalizeCodePhrase(code)
        if (c != panelCode) panelCode = null // typed or picked another code: forget the panel
        _uiState.update { it.copy(codePhrase = c) }
    }

    fun useCodePhrase(code: String) {
        val c = normalizeCodePhrase(code)
        if (c != panelCode) panelCode = null // typed or picked another code: forget the panel
        _uiState.update { it.copy(codePhrase = c) }
    }

    /**
     * Code scanned from a panel's QR. The panel is the receiver, so this is a
     * direct (not stored) send; switch to Direct if Store was selected.
     */
    fun useScannedCode(raw: String) {
        val code = QrCodeParser.parseCode(raw)
        if (code.isBlank()) return
        setDeliveryMode(DeliveryMode.DIRECT)
        val normalized = normalizeCodePhrase(code)
        // A panel keeps its code for 10 minutes; a phone's Show QR is good for one transfer.
        if (QrCodeParser.isOneTimeLink(raw)) {
            panelCode = null
        } else {
            panelCode = normalized
            panelCodeAt = System.currentTimeMillis()
        }
        _uiState.update { it.copy(codePhrase = normalized) }
    }

    // A panel keeps its code for 10 minutes, so after a send we keep using it:
    // "send more" works without scanning again.
    private var panelCode: String? = null
    private var panelCodeAt = 0L
    private fun livePanelCode(): String? {
        val c = panelCode ?: return null
        return if (System.currentTimeMillis() - panelCodeAt < 10 * 60_000L) c else null.also { panelCode = null }
    }

    fun saveCurrentCode() {
        viewModelScope.launch {
            prefsRepo.saveCodePhrase(_uiState.value.codePhrase)
        }
    }

    fun regenerateCode() {
        panelCode = null
        _uiState.update { it.copy(codePhrase = CodePhraseGenerator.generate()) }
    }

    fun setSendMode(mode: SendMode) {
        if (_uiState.value.sendMode != mode) {
            val transferState = _uiState.value.transferState
            if (transferState is CrocTransferState.Completed ||
                transferState is CrocTransferState.StoreCompleted ||
                transferState is CrocTransferState.Error ||
                transferState is CrocTransferState.Cancelled ||
                transferState is CrocTransferState.LegacyFallbackAvailable
            ) {
                crocProcess.reset()
            }
            _uiState.update { it.copy(sendMode = mode) }
        }
    }

    fun toggleTextMode() {
        val currentMode = _uiState.value.sendMode
        val nextMode = if (currentMode == SendMode.TEXT) SendMode.FILES else SendMode.TEXT
        setSendMode(nextMode)
    }

    fun updateTextToSend(text: String) {
        _uiState.update { it.copy(textToSend = text) }
    }

    fun setSharedText(text: String) {
        setSendMode(SendMode.TEXT)
        _uiState.update { it.copy(textToSend = text) }
    }

    fun setDeliveryMode(mode: DeliveryMode) {
        if (_uiState.value.deliveryMode != mode) {
            val transferState = _uiState.value.transferState
            if (transferState is CrocTransferState.Completed ||
                transferState is CrocTransferState.StoreCompleted ||
                transferState is CrocTransferState.Error ||
                transferState is CrocTransferState.Cancelled ||
                transferState is CrocTransferState.LegacyFallbackAvailable
            ) {
                crocProcess.reset()
            }
            _uiState.update {
                it.copy(
                    deliveryMode = mode,
                    sendMode = if (mode == DeliveryMode.STORE && it.sendMode == SendMode.FOLDER) SendMode.FILES else it.sendMode,
                    activeEngine = if (mode == DeliveryMode.STORE) CrocEngine.CURRENT else it.activeEngine
                )
            }
        }
    }

    fun setStoreExpiration(expiration: String) {
        _uiState.update { it.copy(storeExpiration = expiration) }
    }

    fun setStoreDownloads(downloads: Int) {
        _uiState.update { it.copy(storeDownloads = downloads) }
    }

    fun revokeCurrentStore(storeId: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = crocProcess.revokeStore(storeId)
            result.onSuccess { msg ->
                app.database.transferHistoryDao().markAsRevokedByStoreId(storeId)
                _uiState.update { it.copy(transferState = CrocTransferState.Cancelled) }
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.message ?: "Revoke failed")
            }
        }
    }

    fun toggleEngine() {
        if (_uiState.value.isStoreMode) return // Store mode requires v11
        val next = if (_uiState.value.activeEngine == CrocEngine.CURRENT) CrocEngine.LEGACY else CrocEngine.CURRENT
        _uiState.update { it.copy(activeEngine = next) }
    }

    fun setEngine(engine: CrocEngine) {
        if (_uiState.value.isStoreMode && engine != CrocEngine.CURRENT) return
        _uiState.update { it.copy(activeEngine = engine) }
    }

    fun startSend(engine: CrocEngine? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.hasContent) return@launch

            val isStore = state.deliveryMode == DeliveryMode.STORE
            val targetEngine = if (isStore) {
                CrocEngine.CURRENT
            } else {
                engine ?: _uiState.value.activeEngine.takeIf { it == CrocEngine.LEGACY }
                    ?: if (prefsRepo.preferencesFlow.first().tryLegacyFirst) {
                        CrocEngine.LEGACY
                    } else {
                        CrocEngine.CURRENT
                    }
            }

            _uiState.update { it.copy(activeEngine = targetEngine) }

            // Always reset before starting a new transfer
            crocProcess.reset()
            kotlinx.coroutines.delay(50)

            if (isStore) {
                val filePaths = when (state.sendMode) {
                    SendMode.FILES -> stageFilesOrReport(state.selectedFiles) ?: return@launch
                    SendMode.FOLDER -> {
                        state.selectedFolderPath?.let { folderPath ->
                            val folder = File(folderPath)
                            if (folder.exists()) {
                                folder.walkTopDown().filter { it.isFile }.map { it.absolutePath }.toList()
                            } else emptyList()
                        } ?: emptyList()
                    }
                    SendMode.TEXT -> {
                        val context = getApplication<CrocApp>()
                        val textFile = File(context.cacheDir, "croc-send/message.txt").apply {
                            parentFile?.mkdirs()
                            writeText(state.textToSend)
                        }
                        listOf(textFile.absolutePath)
                    }
                }
                if (filePaths.isNotEmpty()) {
                    crocProcess.sendStore(
                        filePaths = filePaths,
                        expiration = state.storeExpiration,
                        downloads = state.storeDownloads
                    )
                }
                return@launch
            }

            when (state.sendMode) {
                SendMode.TEXT -> {
                    if (state.textToSend.isNotBlank()) {
                        crocProcess.sendText(state.textToSend, state.codePhrase, engine = targetEngine)
                    }
                }
                SendMode.FILES -> {
                    if (state.selectedFiles.isNotEmpty()) {
                        val filePaths = stageFilesOrReport(state.selectedFiles) ?: return@launch
                        crocProcess.send(filePaths, state.codePhrase, engine = targetEngine)
                    }
                }
                SendMode.FOLDER -> {
                    val folderPath = state.selectedFolderPath
                    if (folderPath != null) {
                        crocProcess.send(listOf(folderPath), state.codePhrase, engine = targetEngine)
                    }
                }
            }
        }
    }

    fun retryWithLegacy() {
        startSend(engine = CrocEngine.LEGACY)
    }

    fun cancelTransfer() {
        crocProcess.cancel()
        StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
    }

    fun dismissTransferResult() {
        crocProcess.reset()
        StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
        val panel = livePanelCode()
        if (panel != null) {
            _uiState.update { it.copy(codePhrase = panel) }
        } else if (_uiState.value.defaultCodePhrase.isBlank()) {
            _uiState.update { it.copy(codePhrase = CodePhraseGenerator.generate()) }
        }
    }

    fun resetSession() {
        crocProcess.reset()
        val path = _uiState.value.selectedFolderPath
        if (path != null) {
            File(path).deleteRecursively()
        }
        StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
        _uiState.update {
            it.copy(
                selectedFiles = emptyList(),
                selectedBytes = 0,
                codePhrase = livePanelCode() ?: it.defaultCodePhrase.ifBlank { CodePhraseGenerator.generate() },
                textToSend = "",
                selectedFolderName = null,
                selectedFolderFileCount = 0,
                selectedFolderSize = 0,
                selectedFolderPath = null
            )
        }
    }

    /**
     * Prepare files for croc off the main thread. Returns null (and shows an error)
     * if staging fails, instead of crashing the app.
     */
    private suspend fun stageFilesOrReport(files: List<SelectedFile>): List<String>? {
        crocProcess.markPreparing()
        return try {
            withContext(Dispatchers.IO) { copyFilesToInternal(files) }
        } catch (e: Exception) {
            StorageCleaner.cleanSendStaging(getApplication<CrocApp>())
            crocProcess.reportError(e.message ?: "Could not prepare files")
            null
        }
    }

    private fun copyFilesToInternal(files: List<SelectedFile>): List<String> {
        val context = getApplication<CrocApp>()
        val sendDir = File(context.cacheDir, "croc-send").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        // Files croc can read directly need no copy (big files: no 2x storage, no wait).
        val direct = files.associateWith { resolveReadablePath(it) }
        val toCopy = files.filter { direct[it] == null }

        val needed = toCopy.sumOf { it.size }
        val free = sendDir.usableSpace
        val margin = 200L * 1024 * 1024
        if (needed > 0 && needed + margin > free) {
            throw IOException(
                "Not enough free space to prepare the file. Needs " +
                    Formatter.formatFileSize(context, needed) + ", only " +
                    Formatter.formatFileSize(context, free) + " free. " +
                    "Move the file to internal storage (Downloads) or free up space."
            )
        }

        return files.map { file ->
            direct[file]?.let { return@map it }
            val dest = File(sendDir, file.name)
            val input = context.contentResolver.openInputStream(file.uri)
                ?: throw IOException("Cannot open ${file.name}")
            input.use { inp ->
                FileOutputStream(dest).use { out -> inp.copyTo(out, 1024 * 1024) }
            }
            dest.absolutePath
        }
    }

    /** Best-effort: map a picked content URI to a real file path the app can read. */
    private fun resolveReadablePath(file: SelectedFile): String? {
        val context = getApplication<CrocApp>()
        val uri = file.uri
        val candidates = mutableListOf<String>()
        try {
            if (uri.scheme == "file") uri.path?.let { candidates.add(it) }
            if (uri.scheme == "content") {
                if (uri.authority == "com.android.externalstorage.documents" &&
                    DocumentsContract.isDocumentUri(context, uri)
                ) {
                    val parts = DocumentsContract.getDocumentId(uri).split(":", limit = 2)
                    if (parts.size == 2 && parts[0].equals("primary", ignoreCase = true)) {
                        @Suppress("DEPRECATION")
                        candidates.add(File(Environment.getExternalStorageDirectory(), parts[1]).absolutePath)
                    }
                }
                context.contentResolver.query(uri, arrayOf("_data"), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex("_data")
                        if (idx >= 0) c.getString(idx)?.let { candidates.add(it) }
                    }
                }
            }
        } catch (_: Exception) { }

        return candidates.firstOrNull { path ->
            val f = File(path)
            f.isFile && (file.size <= 0 || f.length() == file.size) &&
                try { FileInputStream(f).use { it.read() }; true } catch (_: Exception) { false }
        }
    }

    private suspend fun saveStoreToHistory(state: CrocTransferState.StoreCompleted) {
        val uiState = _uiState.value
        val displayFileName = when (uiState.sendMode) {
            SendMode.FILES -> uiState.selectedFiles.joinToString(", ") { it.name }.ifBlank { state.fileNames.joinToString(", ") }
            SendMode.FOLDER -> uiState.selectedFolderName ?: "folder"
            SendMode.TEXT -> "text message"
        }
        val totalSize = if (uiState.selectedBytes > 0L) uiState.selectedBytes
        else if (uiState.selectedFolderSize > 0L) uiState.selectedFolderSize
        else state.totalBytes

        app.database.transferHistoryDao().insert(
            TransferHistory(
                code = state.cliToken.ifBlank { state.storeId },
                type = TransferType.SEND,
                fileName = displayFileName.ifBlank { "Stored Files (${state.fileCount})" },
                fileSize = totalSize,
                status = TransferStatus.COMPLETED,
                isStored = true,
                storeId = state.storeId,
                storeLink = state.browserLink,
                storeToken = state.cliToken,
                expiresAt = state.expiresAt,
                isRevoked = false,
                storeDownloads = state.downloadsLimit
            )
        )
    }

    private suspend fun saveToHistory(state: CrocTransferState.Completed) {
        val code = _uiState.value.codePhrase
        val uiState = _uiState.value

        when (uiState.sendMode) {
            SendMode.FILES -> {
                val files = uiState.selectedFiles
                if (files.isNotEmpty()) {
                    files.forEach { file ->
                        app.database.transferHistoryDao().insert(
                            TransferHistory(
                                code = code,
                                type = TransferType.SEND,
                                fileName = file.name,
                                fileSize = file.size,
                                status = TransferStatus.COMPLETED
                            )
                        )
                    }
                } else {
                    // Fallback: use parser data
                    app.database.transferHistoryDao().insert(
                        TransferHistory(
                            code = code,
                            type = TransferType.SEND,
                            fileName = state.fileNames.firstOrNull() ?: "file",
                            fileSize = state.totalBytes,
                            status = TransferStatus.COMPLETED
                        )
                    )
                }
            }
            SendMode.FOLDER -> {
                app.database.transferHistoryDao().insert(
                    TransferHistory(
                        code = code,
                        type = TransferType.SEND,
                        fileName = uiState.selectedFolderName ?: "folder",
                        fileSize = uiState.selectedFolderSize,
                        status = TransferStatus.COMPLETED
                    )
                )
            }
            SendMode.TEXT -> {
                app.database.transferHistoryDao().insert(
                    TransferHistory(
                        code = code,
                        type = TransferType.SEND,
                        fileName = state.fileNames.firstOrNull() ?: "text",
                        fileSize = state.totalBytes,
                        status = TransferStatus.COMPLETED
                    )
                )
            }
        }
    }

    private fun normalizeCodePhrase(code: String): String {
        return code.trim().replace(" ", "-")
    }
}
