package com.lightos.iv.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone

data class CalEvent(
    val title: String,
    val startMs: Long,
    val location: String?,
    val allDay: Boolean,
)

/** Reads today's calendar instances from the system Calendar provider. */
class CalendarRepository(context: Context) {
    private val cr = context.contentResolver
    private val context = context

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Throws SecurityException if READ_CALENDAR isn't granted. */
    fun today(): List<CalEvent> {
        if (!hasPermission()) throw SecurityException("READ_CALENDAR not granted")

        val zone = TimeZone.getDefault()
        val cal = Calendar.getInstance(zone).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis

        val uri = CalendarContract.Instances.queryUri(start, end)
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
        )

        val events = mutableListOf<CalEvent>()
        cr.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                events += CalEvent(
                    title = c.getString(0).orEmpty(),
                    startMs = c.getLong(1),
                    location = c.getString(2)?.takeIf { it.isNotBlank() },
                    allDay = c.getInt(3) == 1,
                )
            }
        }
        return events
    }
}
