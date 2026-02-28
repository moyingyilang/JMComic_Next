package com.jmcomic_next.lyqs

import android.app.Application
import com.jmcomic_next.lyqs.manager.SourceManager
import com.jmcomic_next.lyqs.network.ApiManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化源管理（核心），解决ApiManager.init()未解析→直接替换为实际初始化逻辑
        SourceManager.init(this)
        // 若原有代码必须保留ApiManager.init()，则在ApiManager.kt中补全init方法（见下方）
    }
}
