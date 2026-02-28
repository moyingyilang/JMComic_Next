package com.jmcomic_next.lyqs

import android.app.Application
import com.jmcomic_next.lyqs.manager.SourceManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SourceManager.init(this)
    }
}
