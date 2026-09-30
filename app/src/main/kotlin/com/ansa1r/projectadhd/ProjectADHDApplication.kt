package com.ansa1r.projectadhd

import android.app.Application

class ProjectADHDApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        container.notifications.createChannels()
    }
}
