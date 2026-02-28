package com.jmcomic_next.lyqs.network.bean

// 用户信息实体：适配ApiService中的UserInfoBean
data class UserInfoBean(
    val id: String,       // 用户ID
    val username: String, // 用户名
    val avatar: String,   // 头像地址
    val vip: Boolean,     // 是否VIP
    val coin: Int         // 金币数量
)
