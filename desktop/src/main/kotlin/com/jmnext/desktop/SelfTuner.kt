package com.jmnext.desktop

import com.jmnext.selftune.PageSample
import com.jmnext.selftune.SelfTune
import com.jmnext.selftune.SelfTuneStore
import com.jmnext.selftune.Tunables

/**
 * 桌面端的自学习调参器（算法核心在 shared，与 Android 共用同一份实现）。
 *
 * 它调什么：预取深度（`prefetchDepth`，2 到 12）、缓存预算、重试退避这些**可变因素** ——
 * 拿同一套算法在用户设备上自己试、自己挑，用户不需要知道它存在（"暗中优化"）。
 *
 * 学习状态用 [SelfTuneStore] 落盘（这里接桌面端的 [PreferencesKeyValueStore]）：
 * 重启后接着上一代，不会每次从头学。
 *
 * 并发说明（重要）：桌面阅读页的预取是**串行**的（ReaderScreen 里写明"只做串行预取：并发会让更靠后的页先到，
 * 反而没用"），Android 侧也没有并发上限 —— 所以 SELFTUNE 的 `prefetchConcurrency` 这个旋钮
 * **两端都没有对应物，不接线**；不为了"用满四个旋钮"而给它造一个假的落点。
 *
 * 证据边界：接线只到"参数被读取、样本被记录"。**"学习是否真的改善体验"必须靠真机数据**，
 * 本环境做不了 GUI 运行时验证，不许用"已接上"暗示效果。
 */
object SelfTuner {
    private const val KEY_STATE = "state"
    private const val KEY_ENABLED = "enabled"

    private val prefs by lazy { PreferencesKeyValueStore("jm_selftune") }

    private val store = object : SelfTuneStore {
        override fun read(): String? = prefs.getString(KEY_STATE, null)
        override fun write(text: String) { prefs.putString(KEY_STATE, text) }
    }

    /** 默认开启（用户的取向是"暗中优化"）；关掉后 [SelfTune] 一律返回默认值。 */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.putBoolean(KEY_ENABLED, value)

    val tune: SelfTune by lazy {
        SelfTune(
            store = store,
            enabled = enabled,
            logger = { Log.line("自学习", it) },
        )
    }

    /** 当前这一窗口建议的预取深度（阅读页读它来定"往前取几页"）。 */
    val prefetchDepth: Int get() = tune.params().asInt(Tunables.prefetchDepth)

    /** 每页结束时喂一个样本（窗口满了算法会自己换代并挑选参数）。 */
    fun onPage(sample: PageSample) {
        tune.onPage(sample)
    }

    /** 取消不算失败：单独统计（照 PageSample 的注释）。 */
    fun onCancellation() {
        tune.onCancellation()
    }
}
