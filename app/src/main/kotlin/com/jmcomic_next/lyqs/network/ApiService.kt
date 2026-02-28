package com.jmcomic_next.lyqs.network

import com.jmcomic_next.lyqs.bean.CategoryBean
import com.jmcomic_next.lyqs.bean.ComicBean
import com.jmcomic_next.lyqs.bean.ChapterBean
import com.jmcomic_next.lyqs.bean.ChapterDetailBean
import com.jmcomic_next.lyqs.network.bean.BaseResponse
import com.jmcomic_next.lyqs.network.bean.HomeDataBean
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    // 首页数据（已有）
    @GET("home")
    suspend fun getHomeData(): BaseResponse<HomeDataBean>

    // 分类漫画（已有）
    @GET("comic/category")
    suspend fun getComicByCategory(
        @Query("categoryId") categoryId: String,
        @Query("page") page: Int = 1
    ): BaseResponse<MutableList<ComicBean>>

    // 1. 漫画详情接口
    @GET("comic/detail")
    suspend fun getComicDetail(
        @Query("comicId") comicId: String
    ): BaseResponse<ComicBean>

    // 2. 漫画章节目录接口
    @GET("comic/chapter")
    suspend fun getComicChapter(
        @Query("comicId") comicId: String
    ): BaseResponse<MutableList<ChapterBean>>

    // 3. 章节详情（图片列表）接口
    @GET("comic/chapter/detail")
    suspend fun getChapterDetail(
        @Query("chapterId") chapterId: String
    ): BaseResponse<ChapterDetailBean>
}
