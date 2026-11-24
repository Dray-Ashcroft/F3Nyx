package com.dking.crocapp.ui.receive

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.dking.crocapp.ui.theme.CrocTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** README image of the phone's Show QR screen (the real composable, outside its dialog). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
class ReadmeShotsQr {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun showQr() {
        rule.setContent {
            CrocTheme(darkTheme = true) {
                com.dking.crocapp.ui.components.ShowQrContent(
                    title = "Retrieve", code = "4821-amber-falcon-river",
                    qrData = com.dking.crocapp.util.QrCodeParser.sendDeepLink("4821-amber-falcon-river", once = true),
                    waiting = "Waiting for the sender…",
                    steps = "On the sending phone: Transmit → pick files → Scan QR. This closes by itself when the transfer starts.",
                    onNewCode = {}, onCopy = {}, onClose = {}
                )
            }
        }
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(bmp)) }
        val out = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }
        File(out, "ShowQr.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

}
