package com.jmnext.desktop

import com.jmnext.data.JmRepository
import com.jmnext.data.TagBlockResolver
import com.jmnext.data.TagCache
import com.jmnext.data.prefs.BlockStoreApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 桌面端的标签级屏蔽（与 Android 的 `JmApp.tagBlocker` 用的是**同一份**共享层实现）。
 *
 * 为什么需要它：共享层的 `BlockRules.hides()` 只按标题/作者/分类过滤，**标签不在其中**
 * （源码注释写明"标签不在这里（接口不给）"）；标签要逐条取作品详情才知道，所以 Android 用
 * `TagBlockResolver` + `TagCache` 在客户端补这一层。桌面端此前完全没有这套 ——
 * 这也意味着**单加一个「允许一次」按钮不会有任何变化**（没有东西被标签挡住）。
 *
 * 三条取舍照 Android（其注释写明了理由）：并发 3（不跟用户正在看的列表抢带宽）、
 * 标签缓存落盘（否则每滚一次列表就把同一批作品的详情重取一遍）、
 * 规则跟随 [BlockStoreApi]（用户加了标签规则立刻生效，且只用缓存重算、不重新请求）。
 * **没有任何标签规则时，一个请求都不会发**（resolver 内部首行就 return）。
 *
 * 限制（如实记）：这是"顺手补一层屏蔽"，不是完整的标签治理；桌面端目前只在部分列表页接了
 * （见各页的调用点），没接的页面不会因为标签被隐藏。
 */
object TagBlocker {
    private const val KEY_CACHE = "cache"

    private val prefs by lazy { PreferencesKeyValueStore("jm_tag_cache") }
    private var resolver: TagBlockResolver? = null

    /** 幂等：App 启动时调一次即可（与 Android 的 by lazy 单例同义）。 */
    fun init(repository: JmRepository, blockStore: BlockStoreApi) {
        if (resolver != null) return
        val r = TagBlockResolver(
            fetchTags = { id -> runCatching { repository.album(id).tags.toSet() }.getOrNull() },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            maxParallel = 3,
            cache = TagCache().apply { load(prefs.getString(KEY_CACHE, null)) },
            onPersist = { text -> prefs.putString(KEY_CACHE, text) },
        )
        r.setRules(blockStore.snapshot())
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            blockStore.state.collect { r.setRules(it) }
        }
        Log.line("屏蔽", "标签级屏蔽已启用（并发 3、缓存落盘、规则跟随屏蔽名单）")
        resolver = r
    }

    /** 被标签规则隐藏的作品 id；未初始化时为 null（页面据此不做任何过滤）。 */
    val hidden: StateFlow<Set<String>>? get() = resolver?.hidden

    /** 把列表里出现的作品交给它去补标签（只有存在标签规则时才真正发请求）。 */
    fun request(id: String) {
        resolver?.request(id)
    }

    /** 「允许一次」：只作用于本次会话的这批 id，不写回屏蔽规则（照 Android）。 */
    fun allowOnce(ids: Set<String>) {
        resolver?.allowOnce(ids)
    }

    /** 该作品命中了哪些被屏蔽标签（用于提示"含已屏蔽标签：xxx"）。 */
    fun blockedTagsOf(id: String): Set<String> = resolver?.blockedBy?.value?.get(id).orEmpty()
}
