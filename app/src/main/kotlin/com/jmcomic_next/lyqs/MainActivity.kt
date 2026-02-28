package com.jmcomic_next.lyqs

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.jmcomic_next.lyqs.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val APP_TITLE = "JMComic_Next"
    
    fun setBottomNavSelected(id: Int) {
        binding.bottomNav.selectedItemId = id
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        hideStatusBar()

        if (savedInstanceState == null) {
            switchFragment(HomeFragment(), true)
        }

        // 点击标题回首页
        binding.toolbar.setOnClickListener {
            switchFragment(HomeFragment(), true)
            binding.bottomNav.selectedItemId = R.id.nav_home
        }

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home     -> switchFragment(HomeFragment(), true)
                R.id.nav_category -> switchFragment(CategoryFragment(), true)
                R.id.nav_video    -> switchFragment(VideoFragment(), true)
                R.id.nav_discuss  -> switchFragment(DiscussFragment(), true)
                R.id.nav_profile  -> switchFragment(ProfileFragment(), false)
            }
            true
        }

        // ======================
        // 搜索按钮 + 缩放反馈
        // ======================
        binding.btnSearch.setOnClickListener {
            it.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }.start()

            toast("搜索")
        }

        // ======================
        // 三点按钮 + 缩放反馈 + 菜单动画
        // ======================
        binding.btnMore.setOnClickListener { view ->
            view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }.start()

            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.more_options, popup.menu)

            // 弹出动画
            try {
                val field = PopupMenu::class.java.getDeclaredField("mPopup")
                field.isAccessible = true
                val mPopup = field.get(popup)
                mPopup.javaClass
                    .getDeclaredMethod("setPopupAnimationStyle", Int::class.javaPrimitiveType)
                    .invoke(mPopup, android.R.style.Animation_Dialog)
            } catch (_: Exception) {}

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_change_source -> { toast("换源"); true }
                    R.id.action_canteen       -> { toast("食堂"); true }
                    R.id.action_daily         -> { toast("每日"); true }
                    R.id.action_setting       -> { switchFragment(SettingFragment(), true); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun hideStatusBar() {
        window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        window.insetsController?.systemBarsBehavior =
            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    fun switchFragment(fragment: Fragment, showToolbar: Boolean) {
        binding.toolbar.visibility = if (showToolbar) View.VISIBLE else View.GONE
        supportActionBar?.title = if (showToolbar) APP_TITLE else ""
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
