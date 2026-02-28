package com.jmcomic_next.lyqs.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jmcomic_next.lyqs.bean.BannerBean
import com.jmcomic_next.lyqs.config.JMConfig
import com.jmcomic_next.lyqs.databinding.ItemHomeBannerBinding
import com.jmcomic_next.lyqs.manager.SourceManager

class BannerAdapter(
    private val bannerList: List<BannerBean>,
    private val onBannerClick: (BannerBean) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerVH>() {

    inner class BannerVH(val binding: ItemHomeBannerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerVH {
        val binding = ItemHomeBannerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BannerVH(binding)
    }

    override fun onBindViewHolder(holder: BannerVH, position: Int) {
        val banner = bannerList[position]
        val bannerUrl = "${SourceManager.currentSource.staticCdn}${JMConfig.IMAGE_PREFIX}${banner.imgUrl}"
        Glide.with(holder.itemView.context)
            .load(bannerUrl)
            .centerCrop()
            .into(holder.binding.ivBanner)
        holder.itemView.setOnClickListener { onBannerClick(banner) }
    }

    override fun getItemCount() = bannerList.size
}
