package com.jmcomic_next.lyqs.bean

data class HomeComicResponse(
    val list: List<ComicBean> = emptyList(),
    val hasMore: Boolean = false,
    val page: Int = 1,
    val total: Int = 0
)
