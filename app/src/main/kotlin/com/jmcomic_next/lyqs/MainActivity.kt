package com.jmcomic_next.lyqs

import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.jmcomic_next.lyqs.databinding.ActivityMainBinding
import com.jmcomic_next.lyqs.fragment.CanteenFragment
import com.jmcomic_next.lyqs.fragment.CategoryFragment
import com.jmcomic_next.lyqs.fragment.DailyFragment
import com.jmcomic_next.lyqs.fragment.DiscussFragment
import com.jmcomic_next.lyqs.fragment.HomeFragment
import com.jmcomic_next.lyqs.fragment.ProfileFragment
import com.jmcomic_next.lyqs.fragment.SettingFragment
import com.jmcomic_next.lyqs.fragment.VideoFragment
import com.jmcomic_next.lyqs.manager.SourceManager
import com.jmcomic_next.lyqs.network.ApiManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val APP_TITLE = "JMComic_Next"
    private var currentFragment: Fragment? = null

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

        binding.toolbar.setOnClickListener {
            switchFragment(HomeFragment(), true)
            binding.bottomNav.selectedItemId = R.id.nav_home
        }

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment(), true)
                R.id.nav_category -> switchFragment(CategoryFragment(), true)
                R.id.nav_video -> switchFragment(VideoFragment(), true)
                R.id.nav_discuss -> switchFragment(DiscussFragment(), true)
                R.id.nav_profile -> switchFragment(ProfileFragment(), false)
            }
            true
        }

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
        }

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
                    R.id.action_change_source -> {
                        val sourcePopup = PopupMenu(this, view)
                        SourceManager.sourceList.forEachIndexed { index, source ->
                            sourcePopup.menu.add(0, index, index, source.name)
                        }
                        sourcePopup.setOnMenuItemClickListener { sourceItem ->
                            val source = SourceManager.sourceList[sourceItem.itemId]
                            SourceManager.switchSource(source.id)
                            ApiManager.updateRetrofit()
                            true
                        }
                        sourcePopup.show()
                        true
                    }
                    R.id.action_canteen -> {
                        switchFragment(CanteenFragment(), true)
                        true
                    }
                    R.id.action_daily -> {
                        switchFragment(DailyFragment(), true)
                        true
                    }
                    R.id.action_setting -> {
                        switchFragment(SettingFragment(), true)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun hideStatusBar() {
        window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        window.insetsController?.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    fun switchFragment(fragment: Fragment, showToolbar: Boolean) {
        if (currentFragment?.javaClass == fragment.javaClass) return
        binding.toolbar.visibility = if (showToolbar) View.VISIBLE else View.GONE
        supportActionBar?.title = if (showToolbar) APP_TITLE else ""
        val transaction = supportFragmentManager.beginTransaction()
        currentFragment?.let { transaction.hide(it) }
        if (!fragment.isAdded) {
            transaction.add(R.id.fragment_container, fragment)
        } else {
            transaction.show(fragment)
        }
        transaction.commit()
        currentFragment = fragment
    }
}
