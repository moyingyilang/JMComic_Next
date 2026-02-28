package com.jmcomic_next.lyqs.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jmcomic_next.lyqs.bean.BannerBean
import com.jmcomic_next.lyqs.config.JMConfig
import com.jmcomic_next.lyqs.manager.SourceManager
import com.jmcomic_next.lyqs.databinding.ItemHomeBannerBinding

class HomeBannerAdapter(
    private val context: Context,
    private val bannerList: MutableList<BannerBean>,
    private val onBannerClick: (BannerBean) -> Unit
) : RecyclerView.Adapter<HomeBannerAdapter.BannerVH>() {

    inner class BannerVH(val binding: ItemHomeBannerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerVH {
        val binding = ItemHomeBannerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BannerVH(binding)
    }

    override fun onBindViewHolder(holder: BannerVH, position: Int) {
        val bean = bannerList[position]
        // 拼接完整图片地址：CDN+图片前缀+图片路径
        val fullImgUrl = "${SourceManager.currentSource.staticCdn}${JMConfig.IMAGE_PREFIX}${bean.imgUrl}"
        // Glide正常加载，无注释
        Glide.with(context)
            .load(fullImgUrl)
            .into(holder.binding.ivBanner)
        // 点击事件
        holder.itemView.setOnClickListener { onBannerClick(bean) }
    }

    override fun getItemCount() = bannerList.size
}
