package com.twinndroid

import android.app.Application

class TwinnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UpdateWorker.createChannel(this)
        UpdateWorker.schedule(this)
    }
}
