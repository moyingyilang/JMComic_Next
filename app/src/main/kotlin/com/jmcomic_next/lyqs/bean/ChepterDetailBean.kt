package com.jmcomic_next.lyqs.bean

data class ChapterDetailBean(
    val id: String, // 章节ID
    val comicId: String, // 所属漫画ID
    val images: MutableList<String> // 漫画图片URL列表
)
