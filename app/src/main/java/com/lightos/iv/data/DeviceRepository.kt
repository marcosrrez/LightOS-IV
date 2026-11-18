package com.lightos.iv.data

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class DeviceStatus(val batteryPercent: Int, val charging: Boolean, val dnd: Boolean)

/** Battery + Do-Not-Disturb. Uses the sticky battery broadcast for cross-API reliability. */
class DeviceRepository(context: Context) {
    private val context = context.applicationContext

    fun status(): DeviceStatus {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dnd = try {
            nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } catch (e: Exception) {
            false
        }
        return DeviceStatus(percent, charging, dnd)
    }
}
