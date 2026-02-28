package com.jmcomic_next.lyqs.network.bean
import com.jmcomic_next.lyqs.bean.BannerBean
import com.jmcomic_next.lyqs.bean.CategoryBean
import com.jmcomic_next.lyqs.bean.ComicBean
/**
 * 首页数据Bean - 唯一版本，字段名改完和你接口一致！
 */
data class HomeDataBean(
    // ********** 这里改！改成你接口返回的轮播图/分类/漫画列表字段名 **********
    val bannerList: MutableList<BannerBean>?, // 比如接口返bannerList就用这个，返banner就改banner
    val categoryList: MutableList<CategoryBean>?, // 接口返categoryList/categories都可以，改完和接口一致
    val comicList: MutableList<ComicBean>?, // 接口返comicList/comics都可以，改完和接口一致
    val hasNextPage: Boolean? // 接口返hasNextPage/hasMore都可以，改完和接口一致
)
