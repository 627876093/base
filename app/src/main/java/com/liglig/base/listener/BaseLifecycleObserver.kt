package com.liglig.base.listener

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.liglig.base.utils.LogUtils

/**
 * created by liglig on 2021/4/20 0020
 * Description:生命周期管理
 */
open interface BaseLifecycleObserver: LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreateEvent(){}

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStartEvent(){}

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResumeEvent(){}

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPauseEvent(){}

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopEvent(){}

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroyEvent(){}

}