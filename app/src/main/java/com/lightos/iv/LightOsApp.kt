package com.lightos.iv

import android.app.Application
import com.lightos.iv.agent.tools.allTools
import com.lightos.iv.data.AppRepository
import com.lightos.iv.data.CalendarRepository
import com.lightos.iv.data.DeviceRepository
import com.lightos.iv.data.NotesRepository
import com.lightos.iv.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** App-scoped singletons. Repositories outlive the activity and back the view-model. */
class LightOsApp : Application() {
    lateinit var notesRepo: NotesRepository
        private set
    lateinit var appRepo: AppRepository
        private set
    lateinit var calendarRepo: CalendarRepository
        private set
    lateinit var deviceRepo: DeviceRepository
        private set
    lateinit var settingsRepo: SettingsRepository
        private set
    lateinit var focus: FocusController
        private set

    val toolSet by lazy { allTools() }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        settingsRepo = SettingsRepository(this)
        notesRepo = NotesRepository(this)
        appRepo = AppRepository(this).also { it.start() }
        calendarRepo = CalendarRepository(this)
        deviceRepo = DeviceRepository(this)
        focus = FocusController(this, settingsRepo)

        // First run: choose home words from what is actually installed.
        if (settingsRepo.slots.value == null) {
            appScope.launch {
                appRepo.loaded.first { it }
                if (settingsRepo.slots.value == null) settingsRepo.setSlots(appRepo.defaultSlots())
            }
        }
    }
}
