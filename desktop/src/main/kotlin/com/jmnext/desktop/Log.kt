package com.jmnext.desktop

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime

/**
 * 运行日志（1.9.x 调试用）。
 *
 * 为什么要有它：开发容器里跑不起图形界面，界面上的问题只能靠日志判断；
 * 而"图片加载失败"这类问题，光有 URL 是查不出原因的 —— 需要 HTTP 状态码、
 * 异常类型、解码结果。所以：
 *
 *  1. 把 System.out / System.err **同时**写到终端与日志文件（tee），
 *     这样已有的日志和任何未捕获异常都会自动落盘，不必逐处改调用；
 *  2. 提供一个固定路径，测试者跑完把文件发回来即可；
 *  3. 图片加载点单独插桩（状态码/字节数/耗时/解码结果）。
 *
 * 日志里不含账号与密码 —— 登录相关只记结果与错误消息，不记凭据。
 */
object Log {

    /** 日志文件放在用户主目录，容器里对应 /root/jmnext.log。 */
    private val file: File by lazy {
        File(System.getProperty("user.home") ?: ".", "jmnext.log")
    }

    fun init() {
        val stream = runCatching {
            PrintStream(FileOutputStreamTee(file), true, Charsets.UTF_8)
        }.getOrNull() ?: return

        System.setOut(PrintStream(TeeStream(System.out, stream), true, Charsets.UTF_8))
        System.setErr(PrintStream(TeeStream(System.err, stream), true, Charsets.UTF_8))
        line("日志开始", "文件=${file.absolutePath}")
    }

    /** 统一格式：时间 + 标签 + 内容。图片加载与关键动作都走这里。 */
    fun line(tag: String, message: String) {
        val ts = LocalDateTime.now().toString().substring(11, 23)
        System.err.println("[$ts][$tag] $message")
    }

    /** 把异常打到日志里（含类型与消息，便于区分超时/404/解码失败）。 */
    fun error(tag: String, message: String, t: Throwable? = null) {
        line(tag, if (t == null) message else "$message | ${t.javaClass.simpleName}: ${t.message}")
    }

    private class FileOutputStreamTee(f: File) : OutputStream() {
        private val out = java.io.FileOutputStream(f, true)
        override fun write(b: Int) { out.write(b); out.flush() }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); out.flush() }
    }

    private class TeeStream(private val a: OutputStream, private val b: OutputStream) : OutputStream() {
        override fun write(x: Int) { runCatching { a.write(x); a.flush() }; runCatching { b.write(x); b.flush() } }
        override fun write(x: ByteArray, off: Int, len: Int) {
            runCatching { a.write(x, off, len); a.flush() }
            runCatching { b.write(x, off, len); b.flush() }
        }
    }
}
