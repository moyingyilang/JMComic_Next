package com.jmnext

import com.jmnext.data.UpdateCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 检查更新的版本比较（1.8.0）。
 *
 * 重点是**变体后缀**：本应用的 versionName 是 `1.8.0.lite` 这类，而 GitHub tag 是 `v1.8.0`。
 * 这里最容易犯的错是拿两个字符串直接比 —— 那样永远显示"有新版本"。
 */
class UpdateCheckTest {

    @Test
    fun `variant suffixes are ignored on both sides`() {
        assertEquals(listOf(1, 8, 0), UpdateCheck.parts("1.8.0.lite"))
        assertEquals(listOf(1, 8, 0), UpdateCheck.parts("1.8.0.litedebug"))
        assertEquals(listOf(1, 8, 0), UpdateCheck.parts("1.8.0.debug"))
        assertEquals(listOf(1, 8, 0), UpdateCheck.parts("v1.8.0"))
        // 关键：带后缀的当前版本与干净 tag 相等时，不该报"有更新"
        assertFalse(UpdateCheck.isNewer("v1.8.0", "1.8.0.lite"))
        assertFalse(UpdateCheck.isNewer("v1.8.0", "1.8.0.debug"))
    }

    @Test
    fun `newer versions are detected segment by segment`() {
        assertTrue(UpdateCheck.isNewer("v1.8.1", "1.8.0.lite"))
        assertTrue(UpdateCheck.isNewer("v1.9.0", "1.8.9"))
        assertTrue(UpdateCheck.isNewer("v2.0.0", "1.99.99"))
        // 位数不同也要对：1.8 与 1.8.0 视为相同；1.10 比 1.9 新（不是字符串比较！）
        assertFalse(UpdateCheck.isNewer("v1.8", "1.8.0"))
        assertTrue(UpdateCheck.isNewer("v1.10.0", "1.9.0"))
    }

    @Test
    fun `older or unparseable versions never claim an update`() {
        assertFalse(UpdateCheck.isNewer("v1.7.0", "1.8.0"))
        // 拿不到 tag、或 tag 是别的东西时不报更新：误报会让用户去装一个不存在的版本
        assertFalse(UpdateCheck.isNewer(null, "1.8.0"))
        assertFalse(UpdateCheck.isNewer("", "1.8.0"))
        assertFalse(UpdateCheck.isNewer("latest", "1.8.0"))
        assertFalse(UpdateCheck.isNewer("v1.8.0", null))
    }

    @Test
    fun `release url prefers a full url and falls back to the repo`() {
        assertEquals("https://example.com/x", UpdateCheck.releaseUrl("https://example.com/x"))
        assertEquals(
            "https://github.com/moyingyilang/JMNeXt/releases/tag/v1.8.0",
            UpdateCheck.releaseUrl("v1.8.0"),
        )
        assertEquals(
            "https://github.com/moyingyilang/JMNeXt/releases",
            UpdateCheck.releaseUrl(null),
        )
    }

    @Test
    fun `android asset name matches the release naming and covers both architectures`() {
        assertEquals("Android-full-2.1.7.apk", UpdateCheck.androidAssetName("v2.1.7", lite = false))
        assertEquals("Android-lite-2.1.7.apk", UpdateCheck.androidAssetName("v2.1.7", lite = true))
        // 本机版本带变体后缀时也要归一化，否则拼出的附件名不存在
        assertEquals("Android-lite-2.1.7.apk", UpdateCheck.androidAssetName("2.1.7.lite", lite = true))
    }

    @Test
    fun `desktop asset names prefer the universal package per platform`() {
        val win = UpdateCheck.desktopAssetNames("v2.1.7", "Windows 10", "amd64")
        assertEquals("Windows-universal-2.1.7.exe", win.first())
        assertTrue(win.contains("Windows-x64-2.1.7.zip"))
        val winArm = UpdateCheck.desktopAssetNames("v2.1.7", "Windows 11", "aarch64")
        assertTrue(winArm.contains("Windows-arm64-2.1.7.zip"))
        val linux = UpdateCheck.desktopAssetNames("v2.1.7", "Linux", "aarch64")
        assertEquals("Linux-universal-2.1.7.tar.gz", linux.first())
        assertTrue(linux.contains("Linux-aarch64-2.1.7.tar.gz"))
        val linuxX64 = UpdateCheck.desktopAssetNames("v2.1.7", "Linux", "amd64")
        assertTrue(linuxX64.contains("Linux-x86_64-2.1.7.tar.gz"))
    }

    @Test
    fun `asset url uses the tag with v prefix`() {
        assertEquals(
            "https://github.com/moyingyilang/JMNeXt/releases/download/v2.1.7/Android-full-2.1.7.apk",
            UpdateCheck.assetUrl("v2.1.7", "Android-full-2.1.7.apk"),
        )
        // 传进来的 tag 没有 v 前缀时也要补上
        assertTrue(UpdateCheck.assetUrl("2.1.7", "x").contains("/download/v2.1.7/"))
    }
}
