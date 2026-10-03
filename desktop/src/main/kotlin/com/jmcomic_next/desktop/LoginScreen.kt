package com.jmcomic_next.desktop

import kotlinx.coroutines.CancellationException

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.JmRepository
import kotlinx.coroutines.launch

/**
 * 登录页（2.0.0 桌面端）。
 *
 * 为什么先做它：收藏、追更、评论、通知都要登录态，它是这些页面的前置。
 *
 * 提示语与 Android 端保持一致（"请输入用户名""请输入密码"），
 * 登录失败时直接显示服务端原文 —— 官方接口对失败原因的措辞是有用的
 * （例如"无效的用户名和/或密码!"比"登录失败"更能说明问题）。
 */
@Composable
fun LoginScreen(repository: JmRepository, onDone: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("登录", style = MaterialTheme.typography.titleLarge)
        Text(
            "登录后才能使用收藏、追更与评论。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            singleLine = true,
            modifier = Modifier.width(360.dp),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(360.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        runCatching { repository.login(username.trim(), password) }
                            .onSuccess { info ->
                                message = "欢迎回来，${info.username ?: info.uid ?: ""}"
                                System.err.println("[登录] 成功：${info.username} uid=${info.uid}")
                                password = ""
                                onDone()
                            }
                            .onFailure {
                                if (it is CancellationException) return@onFailure
                                message = "登录失败：${it.message}"
                                System.err.println("[登录] 失败：${it.message}")
                            }
                        busy = false
                    }
                },
            ) { Text(if (busy) "登录中…" else "登录") }

            if (repository.auth.isLoggedIn) {
                TextButton(onClick = {
                    scope.launch {
                        repository.logout()
                        System.err.println("[登录] 已退出")
                        message = "已退出登录"
                    }
                }) { Text("退出登录") }
            }
        }

        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
