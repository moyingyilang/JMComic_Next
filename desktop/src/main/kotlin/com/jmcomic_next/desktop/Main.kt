package com.jmcomic_next.desktop
import com.jmcomic_next.lyqs.data.remote.dto.NotificationItem

import kotlinx.coroutines.CancellationException

import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.auth.AuthStore
import com.jmcomic_next.lyqs.data.auth.SecureStore
import com.jmcomic_next.lyqs.data.prefs.BlockStore
import com.jmcomic_next.lyqs.data.prefs.ReadProgressStore
import com.jmcomic_next.lyqs.data.remote.dto.ListItem
import java.io.File

/**
 * 桌面端第一版界面：首页列表（2.0.0）。
 *
 * 这一版的目的是**把最小闭环跑起来**：引导 → 拉首页 → 显示封面与标题。
 * 详情、阅读、搜索等页面在其后铺开。
 *
 * 数据层与 Android 完全同一份（:shared），这里只提供三个平台实现：
 * 键值存储、密钥来源、界面。
 */
private val readProgress by lazy { ReadProgressStore(PreferencesKeyValueStore("jm_read_progress")) }

private val repository: JmRepository by lazy {
    val configDir = File(System.getProperty("user.home"), ".config/jmcomic-next")
    val secure = SecureStore(
        prefs = PreferencesKeyValueStore("jm_secure"),
        keys = FileKeyProvider(File(configDir, "keys")),
    )
    JmRepository.create(
        authStore = AuthStore(secure),
        blockStore = BlockStore(PreferencesKeyValueStore("jm_block")),
        debug = false,
    )
}

fun main() {
    // 必须最先调用：否则在此之前打印的启动信息（版本与构建时间、渲染后端）只进终端、不进日志。
    // 此前"日志里看不到启动行"就是这个原因 —— 不是跑的是旧包。
    Log.init()
    // 渲染后端：用户环境（VNC）建不出 GL 上下文 —— 日志里反复出现
    // "org.jetbrains.skiko.RenderException: Cannot create Linux GL context"，
    // 表现是数据正常加载、画面全黑（用户报"看不到漫画"）。
    // 因此默认用软件渲染（Skia CPU 光栅）保证能看见；要回到 GPU 渲染就设 JMCOMIC_RENDER=GL。
    // 必须在创建第一个 Compose 窗口之前设置，所以放在 main 的最前面。
    if (System.getenv("JMCOMIC_RENDER")?.equals("GL", ignoreCase = true) != true) {
        System.setProperty("skiko.renderApi", "SOFTWARE")
        System.err.println("[启动] 渲染后端：软件渲染（设 JMCOMIC_RENDER=GL 可改回 GPU）")
    }
    // 启动就打出版本与构建时间：一眼分辨手上跑的是哪一版
    // （此前出现过"拿旧安装包测试、以为改动没编译"的误会，这一行就是为它加的）
    val builtAt = runCatching {
        val loc = object {}.javaClass.protectionDomain.codeSource.location
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(File(loc.toURI()).lastModified()))
    }.getOrNull() ?: "未知"
    Log.line("启动", "JMComic_Next 桌面端 版本 $DESKTOP_VERSION，构建时间 $builtAt")
    // 把 stdout/stderr 同时写进 ~/jmcomic-next.log，便于远程读日志定位问题
    // 组合期抛出的异常默认只进 AWT 的日志，容器里看不到；这里显式打到 stderr
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        System.err.println("[崩溃] 线程 ${t.name}：")
        e.printStackTrace()
    }
    runApp()
}

private fun runApp() = application {
    Window(
        // 应用图标：取自 Android 端 mipmap-xxxhdpi 的启动图标（项目自己的资源），
        // 由 scripts 打包脚本同样引用 src/main/resources/icon.png，保持两处一致。
        icon = androidx.compose.ui.res.painterResource("icon.png"),
        onCloseRequest = ::exitApplication,
        title = "JMComic_Next",
        state = rememberWindowState(width = 1100.dp, height = 820.dp),
    ) {
        BlogTheme { App() }
    }
}

@Composable
private fun HomeScreen(onOpen: (ListItem) -> Unit) {
    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var status by remember { mutableStateOf("正在引导…") }
    var page by remember { mutableStateOf(1) }
    var total by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var updatedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    /** 拉第 next 页：next == 1 时重新引导，并刷新列表与状态。 */
    fun reload(next: Int) {
        busy = true
        scope.launch {
            runCatching {
                if (next == 1) repository.bootstrap()
                repository.latest(next)
            }
                .onSuccess { result ->
                    items = if (next == 1) result.items else (items + result.items).distinctBy { it.id }
                    page = next
                    total = result.total
                    val hit = items.count { it.id in updatedIds }
                    status = "首页 ${items.size}/${result.total} 条" +
                        (if (result.hidden > 0) "（屏蔽规则挡掉 ${result.hidden} 条）" else "") +
                        (if (hit > 0) "，其中 $hit 部在追更里有更新" else "")
                    Log.line("首页", status)
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    Log.error("首页", status, it)
                }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        // 「你追的连载里哪些更新了」：数据来自服务端通知（comic_follow 里未读的那批），
        // 照 Android 的 HomeScreen：不要拿阅读时间去猜（既不准又多一次判断）。失败留空，不打扰列表。
        if (repository.auth.isLoggedIn) {
            runCatching {
                repository.notifications(type = NotificationItem.TYPE_COMIC_FOLLOW)
                    .list.filterNot { it.isRead }
                    .flatMap { it.followedUpdates() }
                    .mapNotNull { it.comicIdText }
                    .toSet()
            }
                .onSuccess { updatedIds = it; Log.line("首页", "追更里有更新的作品 ${it.size} 部") }
                .onFailure { Log.line("首页", "追更更新集合读取失败（留空继续）：${it.message}") }
        }
        reload(1)
    }

    LaunchedEffect(status) { System.err.println("[界面] 状态：$status") }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = status,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(168.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // 推荐分区作为整行表头（跨满整行），下面才是最新列表
            item(span = { GridItemSpan(maxLineSpan) }) {
                PromoteHeader(repository = repository, onOpenComic = onOpen)
            }

            items(items, key = { it.id }) { item -> ComicCard(item, onOpen = { onOpen(item) }) }
        }
    }
}

@Composable
private fun ComicCard(item: ListItem, onOpen: () -> Unit) {
    val coverUrl = remember(item.id) { runCatching { repository.coverUrl(item) }.getOrNull() }
    val bitmap = rememberRemoteImage(coverUrl)

    Column(
        modifier = Modifier.clickable { onOpen() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = item.name ?: "(无标题)",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = item.author.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 桌面端的页面栈（2.0.0 第一版：就两个页面，用手写状态机而不是导航库）。 */
private sealed interface Screen {
    data object Home : Screen
    data class Detail(val id: String, val title: String) : Screen
    data class Reader(val comicId: String, val chapterId: String, val chapterIds: List<String> = emptyList()) : Screen
    data object Login : Screen
    data class Page(val route: String) : Screen
}

@Composable
private fun App() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val scope = rememberCoroutineScope()
    val authState by repository.auth.state.collectAsState()
    val navSelection = when (val s = screen) {
        is Screen.Page -> s.route
        else -> "home"
    }

    // 列表页统一的"点作品进详情"，避免在每个分支里重复三段一样的代码
    val openComic: (ListItem) -> Unit = { item ->
        System.err.println("[界面] 打开作品：${item.name} (id=${item.id})")
        screen = Screen.Detail(item.id, item.name.orEmpty())
    }

    Row(Modifier.fillMaxWidth()) {
        SideNav(selected = navSelection, onSelect = { route ->
            System.err.println("[导航] $route")
            screen = if (route == "home") Screen.Home else Screen.Page(route)
        })

        Column(Modifier.weight(1f).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "JMComic_Next",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { screen = Screen.Home },
                )
                Text(
                    text = if (authState.loggedIn) {
                        "已登录：${authState.member?.username ?: authState.member?.uid ?: ""}"
                    } else {
                        "未登录"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    if (authState.loggedIn) {
                        scope.launch { repository.logout(); System.err.println("[登录] 已退出") }
                    } else {
                        screen = Screen.Login
                    }
                }) { Text(if (authState.loggedIn) "退出登录" else "登录") }
            }

            Box(Modifier.weight(1f)) {
                when (val s = screen) {
                    is Screen.Home -> HomeScreen(onOpen = openComic)

                    is Screen.Detail -> DetailScreen(
                        repository = repository,
                        comicId = s.id,
                        onBack = { screen = Screen.Home },
                        onOpenComments = { aid -> screen = Screen.Page("comments:" + aid) },
                        onOpenComic = openComic,
                        progress = readProgress,
                        // 章节顺序由详情页回传（接口下发的是从旧到新），阅读页据此判断上一话/下一话
                        onOpenChapter = { ch, ids ->
                            System.err.println("[界面] 打开章节：sort=${ch.sort} id=${ch.id}（顺序 ${ids.size} 项）")
                            screen = Screen.Reader(comicId = s.id, chapterId = ch.id, chapterIds = ids)
                        },
                    )

                    is Screen.Reader -> ReaderScreen(
                        repository = repository,
                        progress = readProgress,
                        comicId = s.comicId,
                        chapterId = s.chapterId,
                        chapterIds = s.chapterIds,
                        onOpenComments = { aid -> screen = Screen.Page("comments:" + aid) },
                        onBack = { screen = Screen.Home },
                        onSwitchChapter = { id ->
                            System.err.println("[界面] 切换章节 → $id")
                            screen = Screen.Reader(comicId = s.comicId, chapterId = id, chapterIds = s.chapterIds)
                        },
                    )

                    is Screen.Login -> LoginScreen(repository = repository, onDone = { screen = Screen.Home })

                    is Screen.Page if s.route == "random" -> RandomScreen(repository, onOpenComic = openComic)
                    is Screen.Page if s.route == "week" -> WeekScreen(repository, onOpenComic = openComic)

                    is Screen.Page if s.route == "about" -> AboutScreen(repository)
                    is Screen.Page if s.route == "category" -> CategoryScreen(repository, onOpenComic = openComic)
                    is Screen.Page if s.route == "profile" -> ProfileScreen(repository, onGoLogin = { screen = Screen.Login })
                    is Screen.Page if s.route == "notifications" -> NotificationScreen(repository)
                    is Screen.Page if s.route == "creator" -> CreatorScreen(repository)
                    // 评论页用路由字符串带 aid，避免再加一个 Screen 变体
                    is Screen.Page if s.route.startsWith("comments:") -> CommentsScreen(
                        repository = repository,
                        aid = s.route.removePrefix("comments:"),
                        onBack = { screen = Screen.Detail(s.route.removePrefix("comments:"), "") },
                    )
                    is Screen.Page if s.route == "block" -> BlockScreen(repository)
                    is Screen.Page if s.route == "appearance" -> AppearanceScreen()
                    is Screen.Page if s.route == "tags" -> TagsScreen(repository, onSearch = { q ->
                        Log.line("标签", "按标签搜索：$q")
                        screen = Screen.Page("search")
                    })
                    is Screen.Page if s.route == "search" -> SearchScreen(repository, onOpenComic = openComic)
                    is Screen.Page if s.route == "favorites" -> FavoriteScreen(repository, onOpenComic = openComic)
                    is Screen.Page if s.route == "history" -> HistoryScreen(repository, onOpenComic = openComic)
                    // 侧栏「追更」→ 打开收藏页并选中「追更」标签（Android 那边追更就是收藏页里的标签，
                    // 不是独立页面；这样两处入口最终落到同一份 UI，不会各自演化）
                    is Screen.Page if s.route == "tracking" -> FavoriteScreen(repository, onOpenComic = openComic, initialTab = "tracking")

                    is Screen.Page -> {
                        val title = NAV_ITEMS.firstOrNull { it.first == s.route }?.second ?: s.route
                        PageShell(title = title, planned = PAGE_PLANS[s.route] ?: "（待补）")
                    }
                }
            }
        }
    }
}
