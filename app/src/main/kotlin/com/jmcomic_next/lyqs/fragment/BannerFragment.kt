package com.jmcomic_next.lyqs.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.jmcomic_next.lyqs.R
import com.jmcomic_next.lyqs.databinding.FragmentBannerBinding

class BannerFragment : Fragment() {
    private var _binding: FragmentBannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var bannerAdapter: BannerAdapter
    private val bannerList = mutableListOf<BannerBean>()
    private val handler = Handler(Looper.getMainLooper())
    private val bannerRunnable = Runnable { autoPlay() }
    private val BANNER_INTERVAL = 3000L
    private var currentPos = 0
    private val indicatorList = mutableListOf<ImageView>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBannerData()
        initIndicator()
        initViewPager2()
        startAutoPlay()
    }

    private fun initBannerData() {
        bannerList.clear()
        repeat(5) { bannerList.add(BannerBean(R.mipmap.ic_launcher_round)) }
    }

    private fun initViewPager2() {
        bannerAdapter = BannerAdapter(requireContext(), bannerList) { pos ->
            toast("点击轮播图：第${pos+1}张漫画")
        }
        binding.viewPagerBanner.adapter = bannerAdapter
        binding.viewPagerBanner.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPagerBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPos = position
                if (indicatorList.isNotEmpty() && bannerList.isNotEmpty()) {
                    updateIndicator()
                }
            }
        })
        if (bannerList.isNotEmpty()) {
            binding.viewPagerBanner.setCurrentItem(Int.MAX_VALUE / 2, false)
        }
    }

    private fun initIndicator() {
        indicatorList.clear()
        binding.llIndicator.removeAllViews()
        bannerList.forEachIndexed { i, _ ->
            val iv = ImageView(requireContext())
            iv.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(8, 0, 8, 0)
            }
            iv.setImageResource(R.drawable.indicator_dot)
            iv.setColorFilter(if (i == 0) requireContext().getColor(R.color.m3_primary) else requireContext().getColor(R.color.m3_divider))
            indicatorList.add(iv)
            binding.llIndicator.addView(iv)
        }
    }

    private fun updateIndicator() {
        val realPos = currentPos % bannerList.size
        if (realPos < indicatorList.size) {
            indicatorList.forEachIndexed { i, iv ->
                iv.setColorFilter(if (i == realPos) requireContext().getColor(R.color.m3_primary) else requireContext().getColor(R.color.m3_divider))
            }
        }
    }

    private fun startAutoPlay() {
        if (bannerList.isNotEmpty()) {
            handler.postDelayed(bannerRunnable, BANNER_INTERVAL)
        }
    }

    private fun autoPlay() {
        if (bannerList.isNotEmpty()) {
            currentPos++
            binding.viewPagerBanner.setCurrentItem(currentPos, true)
            handler.postDelayed(bannerRunnable, BANNER_INTERVAL)
        }
    }

    private fun pauseAutoPlay() {
        handler.removeCallbacks(bannerRunnable)
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        startAutoPlay()
    }

    override fun onPause() {
        super.onPause()
        pauseAutoPlay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pauseAutoPlay()
        handler.removeCallbacksAndMessages(null)
        indicatorList.clear()
        _binding = null
    }

    data class BannerBean(val imgRes: Int)

    inner class BannerAdapter(
     private val context: Context,
     private val bannerList: MutableList<BannerBean>,
     private val onItemClick: (Int) -> Unit
 ) : RecyclerView.Adapter<BannerAdapter.BannerVH>() {
     override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerVH {
         val binding = FragmentBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
         return BannerVH(binding)
     }
     override fun onBindViewHolder(holder: BannerVH, position: Int) {
         val realPos = position % bannerList.size
         holder.itemView.setOnClickListener { onItemClick(realPos) }
     }
     override fun getItemCount(): Int {
         return if (bannerList.isEmpty()) 0 else Int.MAX_VALUE
     }
     inner class BannerVH(val binding: FragmentBannerBinding) : RecyclerView.ViewHolder(binding.root)
 }
}
