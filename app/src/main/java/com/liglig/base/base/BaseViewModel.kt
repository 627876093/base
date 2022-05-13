package com.liglig.base.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * created by liglig on 2021/4/9 0009
 * Description:
 */
open class BaseViewModel(app: Application) : AndroidViewModel(app) {
    var baseLiveData = BaseLiveData()
}