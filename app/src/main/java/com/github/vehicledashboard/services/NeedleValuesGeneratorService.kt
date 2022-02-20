package com.github.vehicledashboard.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class NeedleValuesGeneratorService : Service() {
    private var generatorService: INeedleValuesGeneratorServiceImpl? = null

    override fun onCreate() {
        super.onCreate()
        generatorService = INeedleValuesGeneratorServiceImpl()
    }

    override fun onBind(intent: Intent?): IBinder? =
        generatorService

    override fun onDestroy() {
        generatorService = null
        super.onDestroy()
    }
}