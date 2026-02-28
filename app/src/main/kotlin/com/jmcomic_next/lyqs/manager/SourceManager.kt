package com.jmcomic_next.lyqs.manager

import android.content.Context
import com.jmcomic_next.lyqs.config.JMConfig

// 源实体：补全id/apiBase，适配所有业务逻辑
data class SourceBean(
    val id: Int,
    val name: String,
    val apiBase: String,  // 拼接好的API基础地址(API_MAIN+API_PREFIX)
    val staticCdn: String // 静态资源CDN地址
)

object SourceManager {
    lateinit var context: Context
    lateinit var currentSource: SourceBean
    val sourceList = mutableListOf<SourceBean>() // 暴露，供外部遍历/选择

    // 初始化：从JMConfig读取源列表，自动拼接API前缀
    fun init(context: Context) {
        this.context = context
        sourceList.clear()
        // 遍历API_MAIN，匹配对应CDN（CDN数量不足时用API地址兜底）
        JMConfig.API_MAIN.forEachIndexed { index, apiUrl ->
            sourceList.add(
                SourceBean(
                    id = index,
                    name = "线路${index + 1}",
                    apiBase = apiUrl + JMConfig.API_PREFIX,
                    staticCdn = JMConfig.STATIC_CDN.getOrElse(index) { apiUrl }
                )
            )
        }
        // 默认选中第一个源（无源时给空值兜底）
        currentSource = sourceList.firstOrNull() ?: SourceBean(0, "默认线路", "", "")
    }

    // 切换线路
    fun switchSource(index: Int) {
        if (index in sourceList.indices) {
            currentSource = sourceList[index]
        }
    }
}
