package com.liglig.base.base

import androidx.lifecycle.MutableLiveData

/**
 * created by liglig on 2021/4/9 0009
 * Description:
 */
open class BaseLiveData {

    /** 请求成功通知  */
    val loadSuccess by lazy { MutableLiveData<Int>() }

    /** 请求失败通知  */
    val loadFail by lazy { MutableLiveData<Int>() }

    /** 刷新 */
    val refresh by lazy { MutableLiveData<Int>() }

    /** 加载更多 */
    val loadMore by lazy { MutableLiveData<Int>() }

    /** 切换到空布局 */
    val switchToEmpty by lazy { MutableLiveData<Int>() }

    /** 切换到列表布局 */
    val switchToList by lazy { MutableLiveData<Int>() }
}