package com.liglig.base.base

import android.content.Context
import androidx.lifecycle.LifecycleService
import com.alibaba.android.arouter.facade.template.IProvider

/**
 * created by liglig on 2021/4/20 0020
 * Description:
 */
open abstract class BaseService : LifecycleService() ,IProvider{
    abstract fun <T> set(context: Context,vararg t: T)


}