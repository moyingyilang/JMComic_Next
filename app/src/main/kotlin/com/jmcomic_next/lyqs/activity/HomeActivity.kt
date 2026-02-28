package com.jmcomic_next.lyqs.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentTransaction
import com.jmcomic_next.lyqs.R
import com.jmcomic_next.lyqs.fragment.HomeFragment
import com.jmcomic_next.lyqs.manager.SourceManager
import com.jmcomic_next.lyqs.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initImmersiveStatusBar() // 沉浸式状态栏
        SourceManager.init(this.applicationContext)
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fl_home_container, HomeFragment.newInstance())
        transaction.commit()
        initBottomNav()
    }

    // 修复废弃警告：用AndroidX WindowInsets替代FLAG_TRANSLUCENT_STATUS
    private fun initImmersiveStatusBar() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        // 隐藏状态栏
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        // 状态栏文字黑色（适配浅色背景）
        windowInsetsController.isAppearanceLightStatusBars = true
        // 让布局延伸到状态栏下方（沉浸式核心效果）
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBarInsets.top, 0, 0)
            insets
        }
    }

    private fun initBottomNav() {
        binding.bnvHome.selectedItemId = R.id.menu_home
        binding.bnvHome.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home, R.id.menu_cate, R.id.menu_mine -> true
                else -> false
            }
        }
    }
}
