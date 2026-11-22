package com.dking.crocapp.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Effects: Full / Minimal" plus the once-only flags for the boot screen and
 * the long Jarvis/Friday intro. Plain SharedPreferences: tiny, synchronous.
 */
object UiEffects {
    private const val FILE = "fenyx_ui"
    private const val KEY_FULL = "effects_full"
    private const val KEY_BOOT_DAY = "last_boot_day"

    /** true = Full effects, false = Minimal. Observable from Compose. */
    var full by mutableStateOf(true)
        private set
    private var loaded = false

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(context: Context) {
        if (loaded) return
        full = prefs(context).getBoolean(KEY_FULL, true)
        loaded = true
    }

    fun setFull(context: Context, value: Boolean) {
        full = value
        prefs(context).edit().putBoolean(KEY_FULL, value).apply()
    }

    /** Boot screen: only with Full effects, and only on the first launch of the day. */
    fun consumeBootForToday(context: Context): Boolean {
        load(context)
        if (!full) return false
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val p = prefs(context)
        if (p.getString(KEY_BOOT_DAY, null) == today) return false
        p.edit().putString(KEY_BOOT_DAY, today).apply()
        return true
    }

    /** Suit cue: full version the first finished transfer of the day, short after. */
    fun consumeSuitFullForToday(context: Context): Boolean {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val p = prefs(context)
        if (p.getString("suit_day", null) == today) return false
        p.edit().putString("suit_day", today).apply()
        return true
    }

    /** Long mode intro: only with Full effects, and only the first time each mode is summoned. */
    fun consumeFirstIntro(context: Context, friday: Boolean): Boolean {
        load(context)
        if (!full) return false
        val key = if (friday) "seen_friday" else "seen_jarvis"
        val p = prefs(context)
        if (p.getBoolean(key, false)) return false
        p.edit().putBoolean(key, true).apply()
        return true
    }
}
