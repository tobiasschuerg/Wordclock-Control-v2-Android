package com.tobiasschuerg.wordclock

import android.app.Application
import timber.log.Timber

class WordClockApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
    }

}