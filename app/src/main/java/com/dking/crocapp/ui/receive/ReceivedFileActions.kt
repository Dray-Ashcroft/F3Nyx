package com.dking.crocapp.ui.receive

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dking.crocapp.R
import com.dking.crocapp.data.db.TransferHistory
import java.io.File

fun doesFileUriExist(context: Context, uri: Uri): Boolean {
    return try {
        when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val path = uri.path ?: return false
                File(path).exists()
            }
            ContentResolver.SCHEME_CONTENT -> {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
            }
            else -> false
        }
    } catch (_: Exception) {
        false
    }
}

fun openReceivedFile(context: Context, file: ReceivedFile): Boolean {
    if (!doesFileUriExist(context, file.uri)) {
        Toast.makeText(context, context.getString(R.string.history_file_not_found), Toast.LENGTH_SHORT).show()
        return false
    }
    return openTransferUri(context, file.uri, file.mimeType)
}

fun shareReceivedFile(context: Context, file: ReceivedFile): Boolean {
    if (!doesFileUriExist(context, file.uri)) {
        Toast.makeText(context, context.getString(R.string.history_file_not_found), Toast.LENGTH_SHORT).show()
        return false
    }
    return shareTransferUri(context, file.uri, file.mimeType, file.name)
}

fun openHistoryTransfer(context: Context, transfer: TransferHistory): Boolean {
    val uri = transfer.fileUri?.let(Uri::parse) ?: return false
    if (!doesFileUriExist(context, uri)) {
        Toast.makeText(context, context.getString(R.string.history_file_not_found), Toast.LENGTH_SHORT).show()
        return false
    }
    val mimeType = transfer.mimeType ?: "application/octet-stream"
    return openTransferUri(context, uri, mimeType)
}

private fun openTransferUri(
    context: Context,
    uri: Uri,
    mimeType: String
): Boolean {
    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(Intent.createChooser(openIntent, context.getString(R.string.action_open_file)))
        true
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.history_file_not_found), Toast.LENGTH_SHORT).show()
        false
    }
}

private fun shareTransferUri(
    context: Context,
    uri: Uri,
    mimeType: String,
    subject: String
): Boolean {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    return try {
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.action_share_file)))
        true
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.history_file_not_found), Toast.LENGTH_SHORT).show()
        false
    }
}


/** Share every received file in one go (ACTION_SEND_MULTIPLE). */
fun shareAllReceivedFiles(context: Context, files: List<ReceivedFile>): Boolean {
    val existing = files.filter { doesFileUriExist(context, it.uri) }
    if (existing.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.history_file_not_found), Toast.LENGTH_SHORT).show()
        return false
    }
    if (existing.size == 1) return shareReceivedFile(context, existing.first())
    val types = existing.map { it.mimeType }.distinct()
    val type = when {
        types.size == 1 -> types.first()
        types.all { it.substringBefore('/') == types.first().substringBefore('/') } -> types.first().substringBefore('/') + "/*"
        else -> "*/*"
    }
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        this.type = type
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(existing.map { it.uri }))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share_file)))
        true
    } catch (_: Exception) {
        false
    }
}

/** Open the system Downloads view, where the F3Nyx folder lives. */
fun openDownloadsFolder(context: Context): Boolean {
    return try {
        context.startActivity(
            Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.history_file_not_found), Toast.LENGTH_SHORT).show()
        false
    }
}
