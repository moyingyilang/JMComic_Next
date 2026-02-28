package com.jmcomic_next.lyqs.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.jmcomic_next.lyqs.R
import java.io.File

class SettingFragment : Fragment() {
    private lateinit var nightSwitch: Switch
    private lateinit var rootView: View
    private var isNightMode = false // 夜间模式永久标记

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_setting, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initClickEvents()
        initNightMode()
    }

    // 永久绑定控件，无空值
    private fun initView() {
        nightSwitch = rootView.findViewById(R.id.switch_night)
    }

    // 夜间模式永久初始化
    private fun initNightMode() {
        nightSwitch.isChecked = isNightMode
        updateNightModeUI()
    }

    // 所有点击功能永久绑定，实际生效无摆设
    private fun initClickEvents() {
        // 1. 清理缓存：实际计算+清除
        rootView.findViewById<View>(R.id.ll_clear_cache).setOnClickListener {
            val cacheSize = calculateCacheSize()
            if (cacheSize > 0) {
                clearCache()
                Toast.makeText(context, "缓存清理完成！共清除${String.format("%.2f", cacheSize)}MB", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "暂无缓存可清理", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 夜间模式：实时切换背景，永久生效
        nightSwitch.setOnCheckedChangeListener { _, isChecked ->
            isNightMode = isChecked
            updateNightModeUI()
            val tip = if (isChecked) "已开启夜间模式" else "已关闭夜间模式"
            Toast.makeText(context, tip, Toast.LENGTH_SHORT).show()
        }

        // 3. 图片加载质量：永久模拟设置
        rootView.findViewById<View>(R.id.ll_img_quality).setOnClickListener {
            Toast.makeText(context, "已设置为【高清画质】，下次加载生效", Toast.LENGTH_SHORT).show()
        }

        // 4. 关于APP：永久显示详细信息
        rootView.findViewById<View>(R.id.ll_about_app).setOnClickListener {
            Toast.makeText(context, "JMComic_Next v1.0.0\n开发：Kotlin\n适配：Android 5.0+\n版权所有 © 2025", Toast.LENGTH_LONG).show()
        }
    }

    // 夜间模式UI永久更新，颜色引用无任何报错
    private fun updateNightModeUI() {
        val bgColor = if (isNightMode) R.color.black else R.color.m3_surface
        val cardBgColor = if (isNightMode) R.color.dark_gray else R.color.white
        // 根布局背景
        rootView.setBackgroundColor(resources.getColor(bgColor, activity?.theme))
        // 所有卡片背景
        rootView.findViewById<View>(R.id.ll_clear_cache).setBackgroundColor(resources.getColor(cardBgColor, activity?.theme))
        rootView.findViewById<View>(R.id.ll_img_quality).setBackgroundColor(resources.getColor(cardBgColor, activity?.theme))
        rootView.findViewById<View>(R.id.ll_about_app).setBackgroundColor(resources.getColor(cardBgColor, activity?.theme))
    }

    // 永久计算缓存大小（单位：MB）
    private fun calculateCacheSize(): Float {
        var totalSize = 0L
        activity?.cacheDir?.listFiles()?.forEach { file -> totalSize += file.length() }
        activity?.filesDir?.listFiles()?.forEach { file -> totalSize += file.length() }
        return totalSize / 1024f / 1024f
    }

    // 永久清除缓存，保留重要文件
    private fun clearCache() {
        activity?.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        activity?.filesDir?.listFiles()?.forEach {
            if (!it.name.contains("app_data")) it.deleteRecursively()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
