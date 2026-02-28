package com.jmcomic_next.lyqs.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jmcomic_next.lyqs.R
import com.jmcomic_next.lyqs.bean.ChapterBean

class ChapterAdapter(
    private val chapterList: List<ChapterBean>,
    private val onChapterClick: (ChapterBean) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    inner class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChapterTitle: TextView = itemView.findViewById(R.id.tv_chapter_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapterList[position]
        holder.tvChapterTitle.text = chapter.title
        holder.itemView.setOnClickListener { onChapterClick(chapter) }
    }

    override fun getItemCount() = chapterList.size
}
