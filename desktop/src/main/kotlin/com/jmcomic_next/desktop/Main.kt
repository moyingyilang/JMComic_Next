package com.jmcomic_next.desktop

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
    // 把 stdout/stderr 同时写进 ~/jmcomic-next.log，便于远程读日志定位问题
    Log.init()
    // 组合期抛出的异常默认只进 AWT 的日志，容器里看不到；这里显式打到 stderr
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        System.err.println("[崩溃] 线程 ${t.name}：")
        e.printStackTrace()
    }
    runApp()
}

private fun runApp() = application {
    Window(
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

    LaunchedEffect(Unit) {
        // 无显示环境下唯一能看运行过程的通道就是标准输出：容器里没有窗口管理器，
        // 截图只能证明"窗口在那儿"，证明不了"数据到了没有"。
        runCatching {
            // 引导（主机发现 + 图床主机）必须在任何登录/列表请求之前完成
            repository.bootstrap()
            status = "正在加载首页…"
            val page = repository.latest(1)
            items = page.items
            page.items.take(3).forEach { System.err.println("[界面] 作品：${it.name} · ${it.author}") }
            status = "首页 ${page.items.size} 条" + if (page.hidden > 0) "（屏蔽规则挡掉 ${page.hidden} 条）" else ""
        }.onFailure { if (it is kotlinx.coroutines.CancellationException) return@onFailure
            status = "加载失败：${it.message}"
            System.err.println("[界面] $status")
        }
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

    Row(Modifier.fillMaxSize()) {
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
                    is Screen.Page if s.route == "block" -> BlockScreen(repository)
                    is Screen.Page if s.route == "appearance" -> AppearanceScreen()
                    is Screen.Page if s.route == "search" -> SearchScreen(repository, onOpenComic = openComic)
                    is Screen.Page if s.route == "favorites" -> FavoriteScreen(repository, onOpenComic = openComic)
                    is Screen.Page if s.route == "history" -> HistoryScreen(repository, onOpenComic = openComic)
                    is Screen.Page if s.route == "tracking" -> TrackingScreen(repository, onOpenComic = openComic)

                    is Screen.Page -> {
                        val title = NAV_ITEMS.firstOrNull { it.first == s.route }?.second ?: s.route
                        PageShell(title = title, planned = PAGE_PLANS[s.route] ?: "（待补）")
                    }
                }
            }
        }
    }
}
