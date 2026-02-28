package com.jmcomic_next.lyqs.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jmcomic_next.lyqs.bean.ComicBean
import com.jmcomic_next.lyqs.config.JMConfig
import com.jmcomic_next.lyqs.manager.SourceManager
import com.jmcomic_next.lyqs.databinding.ItemHomeComicBinding

class HomeComicListAdapter(
    private val context: Context,
    private val comicList: MutableList<ComicBean>,
    private val onComicClick: (ComicBean) -> Unit
) : RecyclerView.Adapter<HomeComicListAdapter.ComicVH>() {

    inner class ComicVH(val binding: ItemHomeComicBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicVH {
        val binding = ItemHomeComicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ComicVH(binding)
    }

    override fun onBindViewHolder(holder: ComicVH, position: Int) {
        val bean = comicList[position]
        // 拼接完整封面地址：CDN+图片前缀+封面路径
        val fullCoverUrl = "${SourceManager.currentSource.staticCdn}${JMConfig.IMAGE_PREFIX}${bean.coverImg}"
        // Glide正常加载，无注释
        Glide.with(context)
            .load(fullCoverUrl)
            .into(holder.binding.ivComicCover)
        // 赋值文本
        holder.binding.tvComicName.text = bean.name
        holder.binding.tvAuthor.text = bean.author
        holder.binding.tvUpdateTime.text = bean.updateTime
        // 点击事件
        holder.itemView.setOnClickListener { onComicClick(bean) }
    }

    override fun getItemCount() = comicList.size
}
