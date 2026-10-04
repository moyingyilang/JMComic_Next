package com.jmnext.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.jmnext.data.TagBlockResolver

/**
 * 列表标签屏蔽的解析器（1.5.1）。
 *
 * 默认 null：拿不到时列表照常显示，只是不做标签过滤 —— 而不是崩掉或什么都不显示。
 * 实例由 [com.jmnext.JmApp] 持有（它要活过单个页面，缓存才有意义）。
 */
val LocalTagBlocker = staticCompositionLocalOf<TagBlockResolver?> { null }
