package com.jmcomic_next.lyqs.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    // 刷新状态
    val isRefreshing = MutableLiveData<Boolean>(false)
    // 加载更多状态
    val isLoadingMore = MutableLiveData<Boolean>(false)
}
