package com.jmcomic_next.lyqs.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.jmcomic_next.lyqs.R
import com.jmcomic_next.lyqs.adapter.BannerAdapter
import com.jmcomic_next.lyqs.adapter.CategoryAdapter
import com.jmcomic_next.lyqs.adapter.HomeComicAdapter
import com.jmcomic_next.lyqs.bean.BannerBean
import com.jmcomic_next.lyqs.bean.CategoryBean
import com.jmcomic_next.lyqs.bean.ComicBean
import com.jmcomic_next.lyqs.network.ApiManager
import com.jmcomic_next.lyqs.network.bean.BaseResponse
import com.jmcomic_next.lyqs.network.bean.HomeDataBean
import com.jmcomic_next.lyqs.viewmodel.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// 补全SwipeRefreshLayout完整导入（关键！）
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.graphics.Rect

class HomeFragment : Fragment() {
    // 控件声明
    private lateinit var srlHome: SwipeRefreshLayout
    private lateinit var vpBanner: ViewPager2
    private lateinit var llBannerIndicator: LinearLayout
    private lateinit var rvCategory: RecyclerView
    private lateinit var rvComicList: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var comicAdapter: HomeComicAdapter

    private val bannerList = mutableListOf<BannerBean>()
    private val categoryList = mutableListOf<CategoryBean>()
    private val comicList = mutableListOf<ComicBean>()

    private var currentPage = 1
    private var hasNextPage = true

    private val defaultCategories = listOf(
        CategoryBean("1", "热门推荐"),
        CategoryBean("2", "最新更新"),
        CategoryBean("3", "完结漫画"),
        CategoryBean("4", "分类大全"),
        CategoryBean("5", "排行榜"),
        CategoryBean("6", "独家首发")
    )

    companion object {
        fun newInstance() = HomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewsFindById(view)
        initAdapters()
        initListeners()
        loadHomeData()
    }

    private fun initViewsFindById(view: View) {
        // 绑定控件
        srlHome = view.findViewById(R.id.srl_home)
        vpBanner = view.findViewById(R.id.vp_banner)
        llBannerIndicator = view.findViewById(R.id.ll_banner_indicator)
        rvCategory = view.findViewById(R.id.rv_category)
        rvComicList = view.findViewById(R.id.rv_comic_list)
        tvEmpty = view.findViewById(R.id.tv_empty)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 分类列表布局
        rvCategory.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        rvCategory.isNestedScrollingEnabled = false

        // 漫画列表布局+间距
        rvComicList.layoutManager = GridLayoutManager(requireContext(), 3)
        rvComicList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.set(4, 4, 4, 4)
            }
        })

        // 下拉刷新颜色（方法生效）
        srlHome.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
    }

    private fun initAdapters() {
        bannerAdapter = BannerAdapter(bannerList) {}
        vpBanner.adapter = bannerAdapter

        categoryAdapter = CategoryAdapter(defaultCategories) {}
        rvCategory.adapter = categoryAdapter

        comicAdapter = HomeComicAdapter(requireContext(), comicList) {}
        rvComicList.adapter = comicAdapter
    }

    private fun initListeners() {
        // 下拉刷新监听（方法生效）
        srlHome.setOnRefreshListener {
            refreshHomeData()
        }

        // 轮播图切换监听
        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBannerIndicator(position)
                vpBanner.removeCallbacks(autoScrollRunnable)
                vpBanner.postDelayed(autoScrollRunnable, 3000)
            }
        })

        // 上拉加载监听
        rvComicList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val lastVisiblePos = layoutManager.findLastCompletelyVisibleItemPosition()
                if (lastVisiblePos == comicAdapter.itemCount - 1
                    && hasNextPage
                    && !srlHome.isRefreshing) {
                    loadMoreComics()
                }
            }
        })
    }

    private val autoScrollRunnable = Runnable {
        if (bannerList.isNotEmpty()) {
            val nextPos = (vpBanner.currentItem + 1) % bannerList.size
            vpBanner.currentItem = nextPos
        }
    }

    private fun loadHomeData() {
        srlHome.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 明确泛型类型，解决推断报错
                val response: BaseResponse<HomeDataBean> = ApiManager.apiService.getHomeData()
                if (response.code == 200 && response.data != null) {
                    val homeData = response.data
                    bannerList.clear()
                    categoryList.clear()
                    comicList.clear()
                    bannerList.addAll(homeData.bannerList ?: mutableListOf())
                    categoryList.addAll(homeData.categoryList ?: defaultCategories)
                    comicList.addAll(homeData.comicList ?: mutableListOf())
                    hasNextPage = homeData.hasNextPage ?: false
                } else {
                    categoryList.addAll(defaultCategories)
                }

                withContext(Dispatchers.Main) {
                    updateUI()
                    initBannerIndicator()
                    vpBanner.postDelayed(autoScrollRunnable, 3000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (comicList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    }
                    categoryList.addAll(defaultCategories)
                    updateUI()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    srlHome.isRefreshing = false
                }
            }
        }
    }

    private fun refreshHomeData() {
        currentPage = 1
        hasNextPage = true
        bannerList.clear()
        categoryList.clear()
        comicList.clear()
        vpBanner.removeCallbacks(autoScrollRunnable)
        loadHomeData()
    }

    private fun loadMoreComics() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 明确泛型类型，解决推断报错
                val response: BaseResponse<MutableList<ComicBean>> = ApiManager.apiService.getComicByCategory("1", ++currentPage)
                if (response.code == 200 && response.data != null && response.data.isNotEmpty()) {
                    val newData = response.data
                    comicList.addAll(newData)
                    withContext(Dispatchers.Main) {
                        comicAdapter.notifyItemRangeInserted(comicList.size - newData.size, newData.size)
                    }
                } else {
                    hasNextPage = false
                    currentPage--
                }
            } catch (e: Exception) {
                e.printStackTrace()
                currentPage--
            }
        }
    }

    private fun initBannerIndicator() {
        llBannerIndicator.removeAllViews()
        if (bannerList.isEmpty()) return

        bannerList.forEachIndexed { index, _ ->
            val indicator = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(16.dpToPx(), 16.dpToPx())
                setImageResource(R.drawable.shape_circle)
                setColorFilter(
                    if (index == 0) ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                    else ContextCompat.getColor(requireContext(), R.color.white).withAlpha(180)
                )
                val margin = 8.dpToPx()
                (layoutParams as LinearLayout.LayoutParams).setMargins(margin, 0, margin, 0)
            }
            llBannerIndicator.addView(indicator)
        }
    }

    private fun updateBannerIndicator(selectedPos: Int) {
        val childCount = llBannerIndicator.childCount
        if (childCount == 0 || selectedPos >= childCount) return

        for (i in 0 until childCount) {
            val indicator = llBannerIndicator.getChildAt(i) as ImageView
            indicator.setColorFilter(
                if (i == selectedPos) ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                else ContextCompat.getColor(requireContext(), R.color.white).withAlpha(180)
            )
        }
    }

    private fun updateUI() {
        bannerAdapter.notifyDataSetChanged()
        categoryAdapter.notifyDataSetChanged()
        comicAdapter.notifyDataSetChanged()
        tvEmpty.visibility = if (comicList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    private fun Int.withAlpha(alpha: Int) = this and 0x00FFFFFF or (alpha shl 24)

    override fun onDestroyView() {
        super.onDestroyView()
        vpBanner.removeCallbacks(autoScrollRunnable)
    }
}
