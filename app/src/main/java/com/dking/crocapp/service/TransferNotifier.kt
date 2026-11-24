package com.dking.crocapp.service

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dking.crocapp.MainActivity
import com.dking.crocapp.R
import com.dking.crocapp.croc.CrocTransferState
import com.dking.crocapp.ui.components.formatBytes

/**
 * Transfer notifications.
 *
 * While a transfer runs: one quiet notification with the percentage, a progress bar,
 * speed, time left and a Cancel action. When it finishes: a "Retrieved" or
 * "Transmitted" notification; for received files it carries Open and Share actions.
 */
object TransferNotifier {

    private const val CH_PROGRESS = "fenyx_progress"
    private const val CH_DONE = "fenyx_done"
    private const val ID_PROGRESS_SEND = 2001
    private const val ID_PROGRESS_RECEIVE = 2002
    private const val ID_DONE_SEND = 2011
    private const val ID_DONE_RECEIVE = 2012

    const val ACTION_CANCEL = "com.drayashcroft.fenyx.CANCEL_TRANSFER"
    const val EXTRA_SENDING = "sending"

    /** Set by the view models so the Cancel action can stop the right transfer. */
    @Volatile var onCancelSend: (() -> Unit)? = null
    @Volatile var onCancelReceive: (() -> Unit)? = null

    private var lastPostAt = 0L
    private var lastPercent = -1

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, context.getString(R.string.notif_channel_progress), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, context.getString(R.string.notif_channel_done), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Called on every transfer-state change from the send or receive view model. */
    fun onState(context: Context, state: CrocTransferState, sending: Boolean) {
        val app = context.applicationContext
        val progressId = if (sending) ID_PROGRESS_SEND else ID_PROGRESS_RECEIVE
        when (state) {
            is CrocTransferState.Transferring -> {
                if (!canPost(app)) return
                val pct = state.progressPercent
                val now = System.currentTimeMillis()
                if (pct == lastPercent && now - lastPostAt < 2_000) return
                if (now - lastPostAt < 500 && pct - lastPercent < 5) return
                lastPercent = pct; lastPostAt = now
                ensureChannels(app)
                val verb = app.getString(if (sending) R.string.notif_transmitting else R.string.notif_retrieving)
                val text = buildString {
                    append(state.fileName)
                    if (state.totalBytes > 0) append(" · ")
                        .append(app.getString(R.string.notif_of, formatBytes(state.bytesTransferred), formatBytes(state.totalBytes)))
                }
                val sub = buildString {
                    if (state.speedBytesPerSec > 0) append(formatBytes(state.speedBytesPerSec)).append("/s")
                    if (state.etaSeconds > 0) {
                        if (isNotEmpty()) append(" · ")
                        append(formatEta(app, state.etaSeconds))
                    }
                }
                val cancel = PendingIntent.getBroadcast(
                    app, if (sending) 1 else 2,
                    Intent(app, TransferActionReceiver::class.java).setAction(ACTION_CANCEL).putExtra(EXTRA_SENDING, sending),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val n = NotificationCompat.Builder(app, CH_PROGRESS)
                    .setSmallIcon(if (sending) android.R.drawable.stat_sys_upload else android.R.drawable.stat_sys_download)
                    .setContentTitle("$verb · $pct%")
                    .setContentText(text)
                    .setSubText(sub.ifBlank { null })
                    .setProgress(100, pct, state.totalBytes <= 0)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)
                    .setContentIntent(openApp(app))
                    .addAction(0, app.getString(R.string.action_cancel), cancel)
                    .build()
                notify(app, progressId, n)
            }
            is CrocTransferState.Completed -> {
                cancel(app, progressId)
                if (sending) postDone(app, sending = true, count = state.fileCount, bytes = state.totalBytes, uris = emptyList())
                // Received files are posted from [onReceivedFiles] once they are saved.
            }
            is CrocTransferState.Error, is CrocTransferState.Cancelled, is CrocTransferState.Idle,
            is CrocTransferState.StoreCompleted, is CrocTransferState.LegacyFallbackAvailable -> cancel(app, progressId)
            else -> {}
        }
    }

    /** Received files are saved: post the done notification with Open and Share. */
    fun onReceivedFiles(context: Context, files: List<Pair<Uri, String>>, bytes: Long) {
        postDone(context.applicationContext, sending = false, count = files.size, bytes = bytes, uris = files)
    }

    private fun postDone(app: Context, sending: Boolean, count: Int, bytes: Long, uris: List<Pair<Uri, String>>) {
        if (!canPost(app)) return
        ensureChannels(app)
        lastPercent = -1
        val title = app.resources.getQuantityString(
            if (sending) R.plurals.notif_transmitted else R.plurals.notif_retrieved, count, count
        )
        val text = if (sending) app.getString(R.string.notif_sent_size, formatBytes(bytes))
        else app.getString(R.string.notif_saved_to, formatBytes(bytes))
        val b = NotificationCompat.Builder(app, CH_DONE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp(app))
        if (!sending && uris.isNotEmpty()) {
            // Open: the file itself when there is one, otherwise the Downloads view.
            val open = if (uris.size == 1) {
                Intent(Intent.ACTION_VIEW).setDataAndType(uris[0].first, uris[0].second)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            b.addAction(0, app.getString(R.string.notif_open), PendingIntent.getActivity(
                app, 10, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
            val share = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).setType(uris[0].second).putExtra(Intent.EXTRA_STREAM, uris[0].first)
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*")
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris.map { it.first }))
            }.apply {
                clipData = ClipData.newUri(app.contentResolver, "files", uris[0].first).also { cd ->
                    uris.drop(1).forEach { cd.addItem(ClipData.Item(it.first)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(share, app.getString(R.string.action_share_file))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            b.addAction(0, app.getString(R.string.notif_share), PendingIntent.getActivity(
                app, 11, chooser, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
        }
        notify(app, if (sending) ID_DONE_SEND else ID_DONE_RECEIVE, b.build())
    }

    private fun formatEta(context: Context, s: Int): String = when {
        s < 60 -> context.getString(R.string.notif_eta_seconds, s)
        s < 3600 -> context.getString(R.string.notif_eta_minutes, (s + 30) / 60)
        else -> context.getString(R.string.notif_eta_hours, s / 3600, (s % 3600) / 60)
    }

    private fun notify(app: Context, id: Int, n: android.app.Notification) {
        try { NotificationManagerCompat.from(app).notify(id, n) } catch (_: SecurityException) {}
    }

    private fun cancel(app: Context, id: Int) {
        NotificationManagerCompat.from(app).cancel(id)
        if (id == ID_PROGRESS_SEND || id == ID_PROGRESS_RECEIVE) lastPercent = -1
    }
}

/** Handles the Cancel action on the progress notification. */
class TransferActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TransferNotifier.ACTION_CANCEL) return
        if (intent.getBooleanExtra(TransferNotifier.EXTRA_SENDING, false)) TransferNotifier.onCancelSend?.invoke()
        else TransferNotifier.onCancelReceive?.invoke()
    }
}
