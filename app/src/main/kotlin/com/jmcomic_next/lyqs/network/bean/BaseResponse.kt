package com.jmcomic_next.lyqs.network.bean
/**
 * 通用基类返回 - 唯一版本，无重复
 */
data class BaseResponse<T>(
    val code: Int, // 200=成功，其他失败，适配你项目
    val msg: String,
    val data: T?
)
