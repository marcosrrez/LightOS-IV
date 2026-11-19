package com.lightos.iv.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.Collator
import java.util.concurrent.Executors

/**
 * Installed apps via [LauncherApps]: covers work profiles, and pushes installs, removals
 * and updates live through a callback. Loading happens off the main thread.
 */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "lightos-apps").apply { priority = Thread.NORM_PRIORITY - 1 } }
    private val main = Handler(Looper.getMainLooper())

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
    }

    fun start() {
        launcherApps.registerCallback(callback, main)
        reload()
    }

    fun reload() {
        io.execute {
            val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
            val list = try {
                userManager.userProfiles.flatMap { user ->
                    val serial = userManager.getSerialNumberForUser(user)
                    launcherApps.getActivityList(null, user).map { info ->
                        AppEntry(
                            name = info.label.toString().trim(),
                            packageName = info.componentName.packageName,
                            className = info.componentName.className,
                            userSerial = serial,
                        )
                    }
                }
                    .filter { it.packageName != context.packageName }
                    .sortedWith(compareBy(collator) { it.name })
            } catch (_: Exception) {
                emptyList()
            }
            _apps.value = list
            _loaded.value = true
        }
    }

    fun find(packageName: String, className: String? = null): AppEntry? =
        _apps.value.firstOrNull { it.packageName == packageName && (className == null || it.className == className) }
            ?: _apps.value.firstOrNull { it.packageName == packageName }

    /** Launches with the system's clip-reveal from [sourceBounds] when given. */
    fun launch(app: AppEntry, sourceBounds: Rect? = null, options: Bundle? = null): Boolean = try {
        val user = userManager.getUserForSerialNumber(app.userSerial) ?: android.os.Process.myUserHandle()
        launcherApps.startMainActivity(ComponentName(app.packageName, app.className), user, sourceBounds, options)
        true
    } catch (_: Exception) {
        launchIntentFor(app.packageName)?.let { runCatching { context.startActivity(it) }.isSuccess } ?: false
    }

    /** Fallback for packages we only know by name (agent tools, saved slots). */
    fun launchIntentFor(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    fun openAppInfo(app: AppEntry) {
        runCatching {
            val user = userManager.getUserForSerialNumber(app.userSerial) ?: android.os.Process.myUserHandle()
            launcherApps.startAppDetailsActivity(ComponentName(app.packageName, app.className), user, null, null)
        }
    }

    fun uninstall(app: AppEntry) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * First-run home words, built only from what is actually installed: the default
     * dialer and SMS app, Notes, Focus, the maps app and the camera, topped up with Today.
     */
    fun defaultSlots(): List<HomeSlot> {
        val pm = context.packageManager
        fun resolve(intent: Intent): String? =
            pm.resolveActivity(intent, 0)?.activityInfo?.packageName?.takeIf { it != "android" }

        val out = mutableListOf<HomeSlot>()
        runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }
            .getOrNull()?.let { out += HomeSlot.App("Phone", it) }
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }
            .getOrNull()?.let { out += HomeSlot.App("Messages", it) }
        out += HomeSlot.Tool(ToolId.Notes)
        out += HomeSlot.Tool(ToolId.Focus)
        resolve(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")))?.let { out += HomeSlot.App("Maps", it) }
        resolve(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))?.let { out += HomeSlot.App("Camera", it) }
        if (out.size < 6) out += HomeSlot.Tool(ToolId.Today)
        return out.distinctBy { if (it is HomeSlot.App) it.packageName else it.label }.take(6)
    }
}
