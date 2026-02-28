package com.jmcomic_next.lyqs.network

import com.jmcomic_next.lyqs.config.JMConfig
import com.jmcomic_next.lyqs.manager.SourceManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// 单例唯一，无重名，所有导入齐全
object RetrofitClient {
    // 初始化OkHttp，SourceInterceptor导入正常+addInterceptor方法正确
    private val okHttpClient: OkHttpClient by lazy {
        val logInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(SourceInterceptor()) // 正确调用拦截器
            .addInterceptor(logInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build() // 正确build
    }

    // 根治Backing field：极简安全写法，不碰底层字段
    val apiService: ApiService by lazy {
        val baseUrl = try {
            // 直接取值，catch兜底，彻底规避初始化判断
            SourceManager.currentSource.apiBase.ifBlank { throw Exception() }
        } catch (e: Exception) {
            "https://api.jmcomic2.com${JMConfig.API_PREFIX}"
        }
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
