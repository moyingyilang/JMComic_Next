package com.jmcomic_next.lyqs.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jmcomic_next.lyqs.bean.ComicBean
import com.jmcomic_next.lyqs.config.JMConfig
import com.jmcomic_next.lyqs.databinding.ItemHomeComicBinding
import com.jmcomic_next.lyqs.manager.SourceManager

class HomeComicAdapter(
    private val context: Context,
    private val comicList: List<ComicBean>,
    private val onComicClick: (ComicBean) -> Unit
) : RecyclerView.Adapter<HomeComicAdapter.ComicVH>() {

    inner class ComicVH(val binding: ItemHomeComicBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicVH {
        val binding = ItemHomeComicBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ComicVH(binding)
    }

    override fun onBindViewHolder(holder: ComicVH, position: Int) {
        val comic = comicList[position]
        holder.binding.tvComicName.text = comic.name
        holder.binding.tvAuthor.text = comic.author
        holder.binding.tvUpdateTime.text = comic.updateTime

        val coverUrl = "${SourceManager.currentSource.staticCdn}${JMConfig.IMAGE_PREFIX}${comic.coverImg}"
        Glide.with(context)
            .load(coverUrl)
            .centerCrop()
            .into(holder.binding.ivComicCover)

        holder.itemView.setOnClickListener { onComicClick(comic) }
    }

    override fun getItemCount() = comicList.size
}
