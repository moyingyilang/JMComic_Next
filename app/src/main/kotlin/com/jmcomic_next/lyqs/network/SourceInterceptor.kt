package com.jmcomic_next.lyqs.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 资源源拦截器 - 配合SourceManager使用，预留扩展能力
 */
class SourceInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // 如需加公共参数/请求头，直接在这里写，目前直接放行
        return chain.proceed(chain.request())
    }
}
