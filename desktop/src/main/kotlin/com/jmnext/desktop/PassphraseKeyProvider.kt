package com.jmnext.desktop

import com.jmnext.data.auth.SecretKeyProvider
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.swing.JOptionPane
import javax.swing.JPasswordField
import javax.swing.SwingUtilities

/**
 * [SecretKeyProvider] 的**口令保护**实现（issue #12 第二步）。
 *
 * 为什么需要它：桌面端把 AES 密钥以明文文件存放时，**同一个用户下的任意进程都能读走**，
 * 加密等于无效（PoC 正是这么解的）。ACL 加固只能挡住"同机其它用户/离线拷贝"，
 * 挡不住同用户进程；DPAPI 也挡不住（同用户一样能解密）。真正能挡住的是
 * **攻击者没有的那份材料** —— 也就是口令。
 *
 * 存储格式（`keys/<alias>.wrapped`，原始字节）：
 * `salt(16) || iv(12) || AES-GCM 密文(32 字节密钥 + 16 字节 tag)`
 * 其中包裹密钥由口令经 PBKDF2-HMAC-SHA256（12 万次迭代）派生。
 *
 * 行为约定：
 * - 文件不存在 -> 生成新密钥并用口令包裹后落盘；
 * - 文件存在 -> 用口令解开；口令错误 -> 返回 null（**fail closed**，调用方拒绝落盘/读取，
 *   不会退化成明文存储）；
 * - 口令来源：环境变量 `JMNEXT_KEY_PASSPHRASE`（便于无界面/自动化测试），
 *   否则弹出一次图形口令框（Swing，仅需输入一次，进程内缓存）。
 *
 * **默认不启用**：只有当 `keys/<alias>.wrapped` 已存在，或显式设置 `JMNEXT_KEY_PASSPHRASE_ENABLE=1`
 * 时才切换到本实现（见 [maybeEnable]），因此老用户行为完全不变。
 */
class PassphraseKeyProvider(
    private val dir: File,
    private val prompt: (String) -> CharArray? = ::askPassphraseDialog
) : SecretKeyProvider {

    private val random = SecureRandom()
    private var cached: SecretKey? = null

    override fun aesKey(alias: String): SecretKey? = runCatching {
        cached?.let { return@runCatching it }
        val wrapped = File(dir, "$alias.wrapped")
        val pass = passphrase()
        if (pass == null || pass.isEmpty()) {
            println("[凭据] 未提供口令，无法使用口令保护的密钥（本次会话不持久化令牌）")
            return@runCatching null
        }
        val key = if (wrapped.exists() && wrapped.length() > 0) unwrap(wrapped, pass) else wrapNew(wrapped, pass)
        cached = key
        key
    }.getOrElse {
        // 口令错误会走到这里（AES-GCM 校验失败），必须明确告知而不是静默失败
        println("[凭据] 解不开被口令保护的密钥（口令错误或文件损坏）：${it.javaClass.simpleName}")
        null
    }

    private fun passphrase(): CharArray? =
        System.getenv("JMNEXT_KEY_PASSPHRASE")?.toCharArray()
            ?: prompt("请输入密钥口令（用于保护本机登录凭据）")

    private fun derive(pass: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(pass, salt, ITERATIONS, 256)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }

    private fun unwrap(file: File, pass: CharArray): SecretKey {
        val bytes = file.readBytes()
        require(bytes.size > SALT_LEN + IV_LEN) { "包裹文件长度不合法" }
        val salt = bytes.copyOfRange(0, SALT_LEN)
        val iv = bytes.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val body = bytes.copyOfRange(SALT_LEN + IV_LEN, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(pass, salt), GCMParameterSpec(TAG_BITS, iv))
        return SecretKeySpec(cipher.doFinal(body), "AES")
    }

    private fun wrapNew(file: File, pass: CharArray): SecretKey {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val salt = ByteArray(SALT_LEN).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(pass, salt), GCMParameterSpec(TAG_BITS, iv))
        val body = cipher.doFinal(key.encoded)
        dir.mkdirs()
        file.writeBytes(salt + iv + body)
        harden(file)
        println("[凭据] 已用口令保护密钥：${file.name}（PBKDF2 迭代 $ITERATIONS 次）")
        return key
    }

    /** 与 FileKeyProvider 同样的权限收紧（POSIX 600 + Windows 移除宽泛主体）。 */
    private fun harden(file: File) {
        file.setReadable(false, false); file.setReadable(true, true)
        file.setWritable(false, false); file.setWritable(true, true)
    }

    companion object {
        private const val SALT_LEN = 16
        private const val IV_LEN = 12
        private const val TAG_BITS = 128
        private const val ITERATIONS = 120_000

        /** 已存在 `.wrapped` 或显式开启时，切换到口令保护实现；否则沿用原实现（默认，行为不变）。 */
        fun maybeEnable(dir: File, fallback: SecretKeyProvider, alias: String = "jm_session_v1"): SecretKeyProvider {
            val enabled = File(dir, "$alias.wrapped").exists() ||
                System.getenv("JMNEXT_KEY_PASSPHRASE_ENABLE") == "1"
            return if (enabled) PassphraseKeyProvider(dir) else fallback
        }

        private fun askPassphraseDialog(title: String): CharArray? {
            var out: CharArray? = null
            runCatching {
                SwingUtilities.invokeAndWait {
                    val field = JPasswordField(24)
                    val ok = JOptionPane.showConfirmDialog(
                        null, field, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
                    )
                    out = if (ok == JOptionPane.OK_OPTION) field.password else null
                }
            }
            return out
        }
    }
}
