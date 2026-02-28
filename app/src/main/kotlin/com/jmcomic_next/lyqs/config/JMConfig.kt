package com.jmcomic_next.lyqs.config

object JMConfig {
    val API_MAIN = listOf(
        "https://api.jmcomic.com",
        "https://api.jmcomic1.com",
        "https://api.jmcomic2.com",
        "https://api-jp.jmcomic.com",
        "https://api-cf.jmcomic.com"
    )

    val STATIC_CDN = listOf(
        "https://static.jmcomic.com",
        "https://static-cf.jmcomic.com",
        "https://img.jmcomic.com",
        "https://jm-media.azureedge.net"
    )

    val WEB_ORIGIN = listOf(
        "https://www.jmcomic.com",
        "https://18comic.org",
        "https://18comic.vip"
    )

    const val DOMAIN_DISCOVER = "https://jm-discover.com/api/domain/list"
    const val UPDATE_API = "https://update.jmcomic.com/api/v1"
    const val REPORT_API = "https://report.jmcomic.com"

    const val API_PREFIX = "/api/v3"
    const val IMAGE_PREFIX = "/media"
    const val CHAPTER_PREFIX = "/photo"
    const val USER_PREFIX = "/user"
    const val UPLOAD_PREFIX = "/upload"
    const val DOWNLOAD_PREFIX = "/download"
}
