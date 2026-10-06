package com.jmnext.desktop

import com.jmnext.data.prefs.KeyValueStore
import java.util.prefs.Preferences

/**
 * [KeyValueStore] 的桌面实现，背后是 java.util.prefs。
 *
 * 选它的原因：JDK 自带、Linux 上落在 ~/.java/.userPrefs（Windows 上落注册表），
 * 不需要自己管文件路径与并发写入。与 Android 的 SharedPreferences 一样是
 * 「同一个名字的实例在进程内共用同一份数据」。
 *
 * 节点命名（issue #11）：项目已从 jmcomic_next 更名，新数据一律写入 `com/jmnext/<node>`；
 * 旧节点 `com/jmcomic_next/<node>` 只读、不删，用于兼容老版本与回退。
 */
class PreferencesKeyValueStore(node: String) : KeyValueStore {

    /** 新节点（更名后）。写入一律走这里。 */
    private val prefs: Preferences = Preferences.userRoot().node("com/jmnext/$node")

    /**
     * 旧节点（更名前）。**只读、不删**：老版本仍可读回自己的数据，便于回退。
     * 读到旧值时迁移写入新节点（见 [migrateIfMissing]）。
     */
    private val legacy: Preferences = Preferences.userRoot().node("com/jmcomic_next/$node")

    /**
     * 新节点没有该键、而旧节点有时，把旧值原样搬到新节点。
     *
     * 注意：`Preferences` 内部统一按字符串存储，所以这里搬字符串形态即可，
     * 各 getXxx 的读取语义不变（getBoolean/getInt/getLong 仍按类型解析）。
     */
    private fun migrateIfMissing(key: String) {
        if (key in prefs.keys()) return
        if (key !in legacy.keys()) return
        legacy.get(key, null)?.let { prefs.put(key, it) }
    }

    override fun getString(key: String, def: String?): String? {
        migrateIfMissing(key)
        return prefs.get(key, def)
    }

    override fun putString(key: String, value: String?) {
        if (value == null) prefs.remove(key) else prefs.put(key, value)
    }

    override fun getBoolean(key: String, def: Boolean): Boolean {
        migrateIfMissing(key)
        return prefs.getBoolean(key, def)
    }

    override fun putBoolean(key: String, value: Boolean) = prefs.putBoolean(key, value)

    override fun getInt(key: String, def: Int): Int {
        migrateIfMissing(key)
        return prefs.getInt(key, def)
    }

    override fun putInt(key: String, value: Int) = prefs.putInt(key, value)

    override fun getLong(key: String, def: Long): Long {
        migrateIfMissing(key)
        return prefs.getLong(key, def)
    }

    override fun putLong(key: String, value: Long) = prefs.putLong(key, value)

    override fun remove(key: String) {
        prefs.remove(key)
        legacy.remove(key)      // 见 issue #11：不清旧节点会让旧值被"复活"
    }

    override fun clear() {
        prefs.clear()
        legacy.clear()          // 同上
    }
}
