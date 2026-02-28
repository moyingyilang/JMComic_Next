package com.jmcomic_next.lyqs.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jmcomic_next.lyqs.databinding.FragmentDailyBinding

class DailyFragment : Fragment() {
    private lateinit var binding: FragmentDailyBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDailyBinding.inflate(inflater, container, false)
        return binding.root
    }
}
