package com.jmcomic_next.lyqs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jmcomic_next.lyqs.databinding.ItemComicBinding

class ComicAdapter(
    private val onClick: (Comic) -> Unit
) : ListAdapter<Comic, ComicAdapter.ViewHolder>(ComicDiffCallback()) {

    inner class ViewHolder(private val binding: ItemComicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comic: Comic) {
            binding.tvTitle.text = comic.title
            // 封面占位图（后续替换成真实漫画封面）
            binding.ivCover.setImageResource(R.drawable.ic_launcher_foreground)
            binding.root.setOnClickListener { onClick(comic) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemComicBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ComicDiffCallback : DiffUtil.ItemCallback<Comic>() {
    override fun areItemsTheSame(oldItem: Comic, newItem: Comic): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Comic, newItem: Comic): Boolean {
        return oldItem == newItem
    }
}
