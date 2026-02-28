package com.jmcomic_next.lyqs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.jmcomic_next.lyqs.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClickEvents()
    }

    /**
     * 初始化点击事件【新增：我的书架，优化提示语】
     */
    private fun initClickEvents() {
        // 头像点击 - 更换头像/个人信息
        binding.ivAvatar.setOnClickListener {
            toast("编辑个人信息")
        }

        // 我的书架【新增：漫画核心功能】
        binding.llBookshelf.setOnClickListener {
            toast("我的书架 → 暂无漫画，快去添加吧")
        }

        // 我的收藏
        binding.llCollect.setOnClickListener {
            toast("我的收藏 → 暂无收藏")
        }

        // 浏览历史
        binding.llHistory.setOnClickListener {
            toast("浏览历史 → 暂无浏览记录")
        }

        // 设置 - 跳转到设置页面【保留原有联动逻辑，无改动】
        binding.llSetting.setOnClickListener {
            (activity as MainActivity).switchFragment(SettingFragment(), true)
            (activity as MainActivity).setBottomNavSelected(R.id.nav_profile)
        }

        // 关于我们
        binding.llAbout.setOnClickListener {
            toast("JMComic_Next v1.0.0 | 基于Kotlin开发")
        }
    }

    /**
     * 吐司工具类【保留，和项目一致】
     */
    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * 销毁Binding【保留，防止内存泄漏】
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
