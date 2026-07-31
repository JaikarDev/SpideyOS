package com.jaikar.spideyos

import android.app.Application
import com.jaikar.spideyos.data.SettingsRepository

class SpideyApp : Application() {
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)
    }

    companion object {
        lateinit var instance: SpideyApp
            private set
    }
}
