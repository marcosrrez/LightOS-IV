package com.lightos.iv

import android.app.Application
import com.lightos.iv.agent.tools.allTools
import com.lightos.iv.data.AppRepository
import com.lightos.iv.data.CalendarRepository
import com.lightos.iv.data.DeviceRepository
import com.lightos.iv.data.NotesRepository
import com.lightos.iv.data.SettingsRepository

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

    override fun onCreate() {
        super.onCreate()
        notesRepo = NotesRepository(this)
        appRepo = AppRepository(this).also { it.load() }
        calendarRepo = CalendarRepository(this)
        deviceRepo = DeviceRepository(this)
        settingsRepo = SettingsRepository(this)
    }

    val toolSet = allTools()
}
