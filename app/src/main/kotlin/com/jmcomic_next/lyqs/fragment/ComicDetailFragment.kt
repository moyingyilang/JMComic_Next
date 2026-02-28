package com.jmcomic_next.lyqs.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // 补全Glide导入（关键！）
import com.jmcomic_next.lyqs.R
import com.jmcomic_next.lyqs.adapter.ChapterAdapter
import com.jmcomic_next.lyqs.bean.ChapterBean
import com.jmcomic_next.lyqs.bean.ComicBean
import com.jmcomic_next.lyqs.network.ApiManager
import com.jmcomic_next.lyqs.network.bean.BaseResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComicDetailFragment : Fragment() {
    companion object {
        fun newInstance(comicId: String) = ComicDetailFragment().apply {
            arguments = Bundle().apply { putString("comicId", comicId) }
        }
    }

    private lateinit var comicId: String
    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvUpdate: TextView
    private lateinit var tvDesc: TextView
    private lateinit var rvChapterList: RecyclerView
    private lateinit var btnStartRead: Button

    private lateinit var chapterAdapter: ChapterAdapter
    private val chapterList = mutableListOf<ChapterBean>()
    private var comicInfo: ComicBean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        comicId = arguments?.getString("comicId") ?: ""
        return inflater.inflate(R.layout.fragment_comic_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadComicDetail()
        loadChapterList()
    }

    private fun initViews(view: View) {
        ivCover = view.findViewById(R.id.iv_comic_cover)
        tvTitle = view.findViewById(R.id.tv_comic_title)
        tvAuthor = view.findViewById(R.id.tv_comic_author)
        tvStatus = view.findViewById(R.id.tv_comic_status)
        tvUpdate = view.findViewById(R.id.tv_comic_update)
        tvDesc = view.findViewById(R.id.tv_comic_desc)
        rvChapterList = view.findViewById(R.id.rv_chapter_list)
        btnStartRead = view.findViewById(R.id.btn_start_read)

        // 章节列表布局
        rvChapterList.layoutManager = LinearLayoutManager(requireContext())
        chapterAdapter = ChapterAdapter(chapterList) { chapter ->
            // 点击章节跳转到阅读页
            startReading(chapter.id)
        }
        rvChapterList.adapter = chapterAdapter

        // 开始阅读按钮：跳转到最新章节
        btnStartRead.setOnClickListener {
            comicInfo?.lastChapterId?.let { startReading(it) }
        }
    }

    // 加载漫画详情
    private fun loadComicDetail() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: BaseResponse<ComicBean> = ApiManager.apiService.getComicDetail(comicId)
                if (response.code == 200 && response.data != null) {
                    comicInfo = response.data
                    withContext(Dispatchers.Main) {
                        updateComicInfo()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 更新漫画详情UI（修复所有字段引用！）
    private fun updateComicInfo() {
        comicInfo?.let { comic -> // 显式指定it为comic，避免类型推断错误
            tvTitle.text = comic.name // 旧：it.title → 新：comic.name
            tvAuthor.text = "作者：${comic.author}"
            tvStatus.text = comic.status
            tvUpdate.text = comic.updateTime // 旧：it.updateInfo → 新：comic.updateTime
            tvDesc.text = comic.description
            // 加载封面（修复cover → coverImg，补全Glide调用）
            Glide.with(requireContext()) // 显式传入requireContext()
                 .load(comic.coverImg) // 旧：it.cover → 新：comic.coverImg
                 .into(ivCover) // 现在不会报Unresolved reference了
        }
    }

    // 加载章节列表
    private fun loadChapterList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: BaseResponse<MutableList<ChapterBean>> = ApiManager.apiService.getComicChapter(comicId)
                if (response.code == 200 && response.data != null) {
                    chapterList.clear()
                    chapterList.addAll(response.data.sortedBy { it.order }) // 按序号排序
                    withContext(Dispatchers.Main) {
                        chapterAdapter.notifyDataSetChanged()
                        // 控制开始阅读按钮状态
                        btnStartRead.isEnabled = chapterList.isNotEmpty()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 跳转到章节阅读页
    private fun startReading(chapterId: String) {
        val fragment = ChapterReadFragment.newInstance(chapterId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
