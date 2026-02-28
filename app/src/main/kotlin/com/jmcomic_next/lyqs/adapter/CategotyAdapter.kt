package com.jmcomic_next.lyqs.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jmcomic_next.lyqs.bean.CategoryBean
import com.jmcomic_next.lyqs.databinding.ItemHomeCategoryBinding

class CategoryAdapter(
    private val categoryList: List<CategoryBean>,
    private val onCategoryClick: (CategoryBean) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryVH>() {

    inner class CategoryVH(val binding: ItemHomeCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        val binding = ItemHomeCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryVH(binding)
    }

    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        val category = categoryList[position]
        holder.binding.category = category
        holder.binding.executePendingBindings()
        holder.itemView.setOnClickListener { onCategoryClick(category) }
    }

    override fun getItemCount() = categoryList.size
}
