package com.jmcomic_next.lyqs.bean

data class ChapterBean(
    val id: String, // 章节ID
    val comicId: String, // 所属漫画ID
    val title: String, // 章节标题（如：第1话 开篇）
    val order: Int // 章节序号（用于排序）
)
