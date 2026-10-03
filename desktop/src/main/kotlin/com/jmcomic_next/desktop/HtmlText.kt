package com.jmcomic_next.desktop

/**
 * 评论内容里的 HTML 转纯文本（从 Android 端 `ui/HtmlText.kt` 原样搬过来）。
 *
 * 为什么需要：JM 的评论内容是 HTML（`<br>` 换行、`<a>` 链接、`<img>` 表情等），
 * 直接渲染会把标签也显示给用户。Android 端就是这么处理的，桌面端此前漏了这一步。
 *
 * 处理方式与 Android 端一致：`<br>` 换行，其余标签去掉，常见实体还原，压缩连续空白。
 * **不做完整 HTML 解析** —— 目标是「别让用户看到标签」，而不是还原网页排版。
 *
 * 一个容易做错的细节（Android 端注释里点明了）：剥完标签什么都不剩时返回 null。
 * 如果那时回退成原文，就会把 `<div></div>` 这种「标签」又还给用户；
 * 调用方拿到 null 就不渲染这一段。
 */
internal fun String?.plainText(): String? {
    if (this.isNullOrBlank()) return null
    val text = this
        .replace(BR, "\n")
        .replace(TAG, "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
    return text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .takeIf { it.isNotBlank() }
}

private val BR = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
private val TAG = Regex("""<[^>]+>""")
