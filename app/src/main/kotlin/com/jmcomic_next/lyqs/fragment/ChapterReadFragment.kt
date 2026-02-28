package com.jmcomic_next.lyqs.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.jmcomic_next.lyqs.R
import com.jmcomic_next.lyqs.adapter.ChapterImageAdapter
import com.jmcomic_next.lyqs.bean.ChapterDetailBean
import com.jmcomic_next.lyqs.network.ApiManager
import com.jmcomic_next.lyqs.network.bean.BaseResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChapterReadFragment : Fragment() {
    companion object {
        fun newInstance(chapterId: String) = ChapterReadFragment().apply {
            arguments = Bundle().apply { putString("chapterId", chapterId) }
        }
    }

    private lateinit var chapterId: String
    private lateinit var vpReader: ViewPager2
    private lateinit var imageAdapter: ChapterImageAdapter
    private val imageList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        chapterId = arguments?.getString("chapterId") ?: ""
        return inflater.inflate(R.layout.fragment_chapter_read, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vpReader = view.findViewById(R.id.vp_comic_reader)
        imageAdapter = ChapterImageAdapter(imageList)
        vpReader.adapter = imageAdapter
        loadChapterImages()
    }

    // 加载章节图片
    private fun loadChapterImages() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: BaseResponse<ChapterDetailBean> = ApiManager.apiService.getChapterDetail(chapterId)
                if (response.code == 200 && response.data != null) {
                    imageList.clear()
                    imageList.addAll(response.data.images)
                    withContext(Dispatchers.Main) {
                        imageAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
