package com.jmnext.desktop

import com.jmnext.data.auth.SecretKeyProvider
import java.io.File
import java.nio.file.Files
import java.util.prefs.Preferences

/**
 * 凭据存储相关改动的**可执行自检**（issue #11 / #12）。
 *
 * 为什么要有它：这些改动此前只做了编译验证，而"编译通过"并不等于逻辑对。
 * 这里把关键路径真的跑一遍（口令包裹/解开/错口令/销毁、节点迁移与清除、
 * 无图形环境下首次询问的安全失败路径），并在失败时以非零退出码结束。
 *
 * 运行方式（在 desktop 目录）：
 * ```
 * gradle --offline compileKotlin
 * java -cp build/classes/kotlin/main:../shared/build/classes/kotlin/main:<kotlin-stdlib.jar> \
 *      com.jmnext.desktop.SelfCheckKt
 * ```
 */
object SelfCheck {
    private var pass = 0
    private var fail = 0

    private fun check(what: String, ok: Boolean) {
        println((if (ok) "  通过: " else "  失败: ") + what)
        if (ok) pass++ else fail++
    }

    /** 跑完全部检查并返回失败项数（0 表示全通过）。供 Gradle 测试调用，不退出进程。 */
    @JvmStatic
    fun run(): Int {
        pass = 0
        fail = 0
        println("== 凭据存储自检 ==")
        checkPassphrase()
        checkPreferences()
        checkFreshInstallDecision()
        println("== 结果：通过 " + pass + " 项，失败 " + fail + " 项 ==")
        return fail
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (run() > 0) System.exit(1)
    }

    /** 口令保护：包裹 -> 解开 -> 错口令 fail closed -> forget 删除文件。 */
    private fun checkPassphrase() {
        val dir = Files.createTempDirectory("selfcheck-keys").toFile()
        val good = PassphraseKeyProvider(dir) { "correct-horse".toCharArray() }
        val k1 = good.aesKey(ALIAS)
        check("口令保护：首次调用生成并包裹密钥", k1 != null && File(dir, "$ALIAS.wrapped").length() > 0)

        // 新实例（清掉进程内缓存）用同一口令解开
        val again = PassphraseKeyProvider(dir) { "correct-horse".toCharArray() }
        val k2 = again.aesKey(ALIAS)
        check("口令保护：同一口令可解出同一把密钥",
            k1 != null && k2 != null && k1.encoded.contentEquals(k2.encoded))

        val wrong = PassphraseKeyProvider(dir) { "wrong-pass".toCharArray() }
        check("口令保护：错口令 fail closed（返回 null 而不是明文兜底）", wrong.aesKey(ALIAS) == null)

        good.forget(ALIAS)
        check("口令保护：forget 删除包裹文件（登出即销毁密钥材料）", !File(dir, "$ALIAS.wrapped").exists())
        dir.deleteRecursively()
    }

    /** Preferences：新节点读写、旧节点迁移、clear 两个节点都清（issue #11）。 */
    private fun checkPreferences() {
        val node = "selfcheck_" + System.nanoTime()
        val legacy = Preferences.userRoot().node("com/jmcomic_next/$node")
        legacy.put("jwt", "OLD-CIPHERTEXT")
        legacy.put("lastPage", "42")
        legacy.flush()

        val store = PreferencesKeyValueStore(node)
        check("节点迁移：读到旧节点的值", store.getString("jwt", null) == "OLD-CIPHERTEXT")
        check("节点迁移：整型键按类型读回", store.getInt("lastPage", 0) == 42)
        val newPrefs = Preferences.userRoot().node("com/jmnext/$node")
        check("节点迁移：旧值已写入新节点", newPrefs.get("jwt", null) == "OLD-CIPHERTEXT")
        check("节点迁移：旧节点仍保留（可回退）", legacy.get("jwt", null) == "OLD-CIPHERTEXT")

        store.clear()
        check("clear：新节点已清空", newPrefs.get("jwt", null) == null)
        check("clear：旧节点也已清空（否则登出后旧密文会被复活）", legacy.get("jwt", null) == null)
        runCatching { newPrefs.removeNode(); legacy.removeNode() }
    }

    /** 全新安装 + 无图形环境：询问弹不出来时必须安全落回原实现，并写下"不再询问"标记。 */
    private fun checkFreshInstallDecision() {
        val dir = Files.createTempDirectory("selfcheck-fresh").toFile()
        val fallback = object : SecretKeyProvider {
            override fun aesKey(alias: String) = null
        }
        val chosen = runCatching { PassphraseKeyProvider.maybeEnable(dir, fallback, ALIAS) }.getOrNull()
        check("全新安装：maybeEnable 不抛异常（无图形环境下对话框异常被兜住）", chosen != null)
        check("全新安装：安全落回原实现", chosen === fallback)
        check("全新安装：写下不再询问的标记", File(dir, ".passphrase-declined").exists())
        dir.deleteRecursively()
    }

    private const val ALIAS = "selfcheck_alias"
}

fun main() = SelfCheck.main(emptyArray())
