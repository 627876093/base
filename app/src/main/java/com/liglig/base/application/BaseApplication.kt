package com.liglig.base.application

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.alibaba.android.arouter.launcher.ARouter
import com.liglig.base.listener.BaseLifecycleObserver
import com.liglig.base.utils.AppUtils
import com.liglig.base.utils.LogUtils
import com.liglig.base.utils.OkHttpUtils

/**
 * created by liglig on
 * Description: application
 */
class  BaseApplication : Application(), BaseLifecycleObserver {

    companion object {
        lateinit var appContext: Context
        var appBackground: Boolean = false
    }


    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        initARouter()
        OkHttpUtils.initOkHttp()
        LogUtils.init(AppUtils.isApkInDebug(this))
        //应用程序的生命周期
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun initARouter() {
        if (AppUtils.isApkInDebug(this)) {
            ARouter.openLog()
            ARouter.openDebug()
        }
        ARouter.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        ARouter.getInstance().destroy()
    }


    override fun onResumeEvent() {
        appBackground = false
        LogUtils.e("应用活跃")
        super.onResumeEvent()
    }

    override fun onStopEvent() {
        appBackground = true
        LogUtils.e("应用切换到后台")
        super.onStopEvent()
    }


}