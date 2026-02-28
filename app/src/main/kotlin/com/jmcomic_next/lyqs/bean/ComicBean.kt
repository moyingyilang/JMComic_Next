package com.jmcomic_next.lyqs.bean

data class ComicBean(
    val id: String, // 漫画ID（核心！）
    val name: String, // 对应适配器里的 'name'（之前是title）
    val coverImg: String, // 对应适配器里的 'coverImg'（之前是cover）
    val author: String, // 作者（不变）
    val status: String, // 状态：连载/完结（不变）
    val updateTime: String, // 对应适配器里的 'updateTime'（之前是updateInfo）
    val description: String, // 漫画简介（不变）
    val lastChapterId: String? // 最新章节ID（可选，不变）
)
