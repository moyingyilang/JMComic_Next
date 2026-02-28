package com.jmcomic_next.lyqs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.jmcomic_next.lyqs.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    // 视图绑定
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 初始化绑定
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 初始化点击事件
        initClickEvents()
    }

    /**
     * 初始化所有点击事件（预留，直接加逻辑即可）
     */
    private fun initClickEvents() {
        // 头像点击
        binding.ivAvatar.setOnClickListener {
            toast("更换头像")
        }

        // 跳转到设置页面（和你现有SettingFragment联动）
        binding.llSetting.setOnClickListener {
            (activity as MainActivity).switchFragment(SettingFragment(), true)
            // 同步底栏选中状态（可选）
            (activity as MainActivity).setBottomNavSelected(R.id.nav_profile)
        }

        // 我的收藏
        binding.llCollect.setOnClickListener {
            toast("我的收藏")
        }

        // 浏览历史
        binding.llHistory.setOnClickListener {
            toast("浏览历史")
        }

        // 关于我们
        binding.llAbout.setOnClickListener {
            toast("JMComic_Next v1.0")
        }

        // 退出登录
        binding.btnLogout.setOnClickListener {
            toast("退出登录成功")
        }
    }

    /**
     * 吐司工具类（和MainActivity保持一致）
     */
    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * 销毁视图，防止内存泄漏
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
