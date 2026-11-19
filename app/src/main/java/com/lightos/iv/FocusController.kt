package com.lightos.iv

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import com.lightos.iv.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FocusState(val endsAt: Long, val now: Long) {
    val running: Boolean get() = endsAt > now
    val remainingMs: Long get() = (endsAt - now).coerceAtLeast(0L)
    val remainingMinutes: Int get() = ((remainingMs + 59_999L) / 60_000L).toInt()
}

/**
 * The focus timer, owned by the application (not a screen) and keyed to a wall-clock end
 * time persisted in settings, so it survives leaving LightOS and process restarts.
 * The chime plays when the end time passes while LightOS is alive.
 */
class FocusController(private val context: Context, private val settings: SettingsRepository) {
    private val main = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(FocusState(settings.focusEndsAt.value, System.currentTimeMillis()))
    val state: StateFlow<FocusState> = _state.asStateFlow()

    private val ticker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val end = settings.focusEndsAt.value
            _state.value = FocusState(end, now)
            when {
                end == 0L -> Unit
                now >= end -> {
                    settings.setFocusEndsAt(0L)
                    _state.value = FocusState(0L, now)
                    chime()
                }
                else -> main.postDelayed(this, (end - now).coerceAtMost(1_000L))
            }
        }
    }

    init {
        if (settings.focusEndsAt.value > 0L) main.post(ticker)
    }

    fun start(minutes: Int) {
        settings.setFocusEndsAt(System.currentTimeMillis() + minutes * 60_000L)
        main.removeCallbacks(ticker)
        main.post(ticker)
    }

    fun stop() {
        settings.setFocusEndsAt(0L)
        main.removeCallbacks(ticker)
        _state.value = FocusState(0L, System.currentTimeMillis())
    }

    /** Re-reads the clock (e.g. when LightOS comes back to the foreground). */
    fun refresh() {
        main.removeCallbacks(ticker)
        main.post(ticker)
    }

    private fun chime() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
        }
        runCatching {
            val v = context.getSystemService(Vibrator::class.java)
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 120, 40), -1))
        }
    }
}
