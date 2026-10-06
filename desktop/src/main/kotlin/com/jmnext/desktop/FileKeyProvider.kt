package com.jmnext.desktop

import com.jmnext.data.auth.SecretKeyProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclFileAttributeView
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * [SecretKeyProvider] 的桌面实现：AES 密钥存放在用户配置目录下的文件里。
 *
 * **强度说明（如实写清）**：这比不上 Android 的 Keystore —— 那里密钥不导出、
 * 由硬件或系统保护；这里密钥就是一串字节，**能被同一个用户下的任意进程取走**。
 * 桌面端在"不改用系统凭据库"的前提下能做到的是：
 *
 * 1. 限制文件权限（POSIX 用 600；Windows 用 NTFS ACL 移除宽泛主体）；
 * 2. 与数据分离存放（密文在 java.util.prefs / 注册表，密钥在配置目录）。
 *
 * 仍**挡不住**同用户进程 —— 要挡那一步必须把密钥交给系统凭据库（Windows DPAPI、
 * Linux libsecret、macOS Keychain），那是本接口预留的后续实现（见 issue #12）。
 */
class FileKeyProvider(private val dir: File) : SecretKeyProvider {

    override fun aesKey(alias: String): SecretKey? = runCatching {
        val file = File(dir, "$alias.key")
        if (file.exists() && file.length() > 0) {
            hardenPermissions(file)          // 老用户的文件也要收紧（下次启动即生效）
            SecretKeySpec(file.readBytes(), "AES")
        } else {
            val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            dir.mkdirs()
            file.writeBytes(key.encoded)
            hardenPermissions(file)          // 仅本人可读写（在支持 POSIX 权限的平台上生效）
            key
        }
    }.getOrNull()

    /**
     * 收紧密钥文件权限。
     *
     * - POSIX（Linux/macOS）：`File.setReadable/setWritable` 等价 `chmod 600`；
     * - Windows：上面两个方法**只映射到 DOS 只读属性，不会设置 NTFS ACL**，
     *   所以这里再显式移除**宽泛主体**（Users / Authenticated Users / Everyone 等）的访问项。
     *
     * 策略刻意保守：只删"宽泛主体"，不动所有者 / SYSTEM / Administrators；
     * 任何异常都只打印告警、**不改变原 ACL**（避免把文件搞成谁都读不了）。
     * 系统语言不同会导致主体名字不同，因此这里只匹配一小组常见名称；
     * 匹配不到就什么都不做，绝不会误删系统账户的访问项。
     */
    private fun hardenPermissions(file: File) {
        file.setReadable(false, false)
        file.setReadable(true, true)
        file.setWritable(false, false)
        file.setWritable(true, true)

        runCatching {
            val path = file.toPath()
            val view = Files.getFileAttributeView(path, AclFileAttributeView::class.java)
                ?: return@runCatching                    // 非 Windows 或不支持 ACL：正常返回
            val broad = listOf(
                "users", "authenticated users", "everyone",
                "jeder", "benutzer", "tout le monde", "todos", "utilisateurs"
            )
            val kept = mutableListOf<AclEntry>()
            val dropped = mutableListOf<String>()
            for (entry in view.acl) {
                val name = entry.principal().name.substringAfterLast('\\').lowercase()
                if (broad.any { name == it }) dropped += entry.principal().name else kept += entry
            }
            if (dropped.isNotEmpty()) {
                view.acl = kept
                println("[凭据] 已收紧密钥文件 ACL，移除宽泛主体：${dropped.joinToString()}")
            }
        }.onFailure { println("[凭据] ACL 收紧失败（未改动原 ACL）：${it.message}") }
    }
}
