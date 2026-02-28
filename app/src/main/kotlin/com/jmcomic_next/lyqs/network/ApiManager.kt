package com.jmcomic_next.lyqs.network

import com.jmcomic_next.lyqs.config.JMConfig
import com.jmcomic_next.lyqs.manager.SourceManager
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiManager {
    private lateinit var _apiService: ApiService
    val apiService get() = _apiService

    // 补全init方法
    fun init(context: Context) {
        SourceManager.init(context)
        createApiService()
    }

    // 补全updateRetrofit方法，供MainActivity.kt调用（源切换后更新）
    fun updateRetrofit() {
        createApiService()
    }

    // 内部创建ApiService，适配动态源
    private fun createApiService() {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(SourceInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val baseUrl = try {
            SourceManager.currentSource.apiBase.ifBlank { throw Exception() }
        } catch (e: Exception) {
            "https://api.jmcomic2.com${JMConfig.API_PREFIX}"
        }

        _apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
