package com.lightos.iv.data

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class AppEntry(val name: String, val packageName: String)

/** Enumerates launchable apps via PackageManager. The LAUNCHER intent is in Android 11+'s
 *  default visibility set, so this works on modern devices without broad package access. */
class AppRepository(context: Context) {
    private val pm = context.packageManager

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    fun load() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = try {
            pm.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }
        _apps.value = resolved
            .map { AppEntry(it.activityInfo.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    /** Returns a launch intent for the package, or null if it isn't launchable. */
    fun launchIntentFor(packageName: String): Intent? =
        pm.getLaunchIntentForPackage(packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
}
