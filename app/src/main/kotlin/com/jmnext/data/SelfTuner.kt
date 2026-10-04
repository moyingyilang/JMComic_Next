package com.jmnext.data

import android.content.Context
import com.jmnext.data.prefs.KeyValueStore
import com.jmnext.data.prefs.SharedPrefsKeyValueStore
import com.jmnext.selftune.PageSample
import com.jmnext.selftune.SelfTune
import com.jmnext.selftune.SelfTuneStore
import com.jmnext.selftune.Tunables

/**
 * Android 端的自学习调参器（算法核心在 shared，与桌面端共用同一份实现）。
 *
 * 它调什么：预取深度这类**可变因素** —— 同一套算法在用户设备上自己试、自己挑，
 * 用户不需要知道它存在（"暗中优化"）。
 *
 * 学习状态用 [SelfTuneStore] 落盘（这里接 Android 的 [SharedPrefsKeyValueStore]）：重启接着上一代。
 *
 * 未初始化时 [prefetchDepth] 返回**默认值**，所以调用方可以先接读取端，行为与今天完全一致
 * （预取窗口不变），等 init 接上之后再开始学习。
 *
 * 证据边界：接线只到"参数被读取、样本被记录"。算法是否真的改善体验必须靠真机数据，
 * 不许用"已接上"暗示效果。
 */
object SelfTuner {
    private const val KEY_STATE = "state"
    private const val KEY_ENABLED = "enabled"
    private const val PREFS_NAME = "jm_selftune"

    private var prefs: KeyValueStore? = null
    private var tune: SelfTune? = null

    /** 幂等：在 Application/最早能拿到 Context 的地方调一次即可。 */
    fun init(context: Context) {
        if (tune != null) return
        val store = SharedPrefsKeyValueStore(context.applicationContext, PREFS_NAME)
        prefs = store
        tune = SelfTune(
            store = object : SelfTuneStore {
                override fun read(): String? = store.getString(KEY_STATE, null)
                override fun write(text: String) { store.putString(KEY_STATE, text) }
            },
            enabled = store.getBoolean(KEY_ENABLED, true),
        )
    }

    /** 默认开启（用户的取向是"暗中优化"）。 */
    var enabled: Boolean
        get() = prefs?.getBoolean(KEY_ENABLED, true) ?: true
        set(value) { prefs?.putBoolean(KEY_ENABLED, value) }

    /** 当前这一窗口建议的预取深度（2 到 12，默认 6）；未初始化时为默认值。 */
    val prefetchDepth: Int
        get() = tune?.params()?.asInt(Tunables.prefetchDepth) ?: Tunables.prefetchDepth.default.toInt()

    fun onPage(sample: PageSample) {
        tune?.onPage(sample)
    }

    fun onCancellation() {
        tune?.onCancellation()
    }
}
