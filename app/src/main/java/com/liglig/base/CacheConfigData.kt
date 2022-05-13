package com.liglig.base

import android.os.Environment
import com.liglig.base.application.BaseApplication


/**
 * created by liglig on 2021/4/13 0013
 * Description:
 */
object CacheConfigData {

    //APP开放文件缓存路径
    final val cacheDirOpen = "${Environment.getExternalStorageDirectory().path}cache/"

    //APP专属文件缓存路径
    final val cacheDir = "${BaseApplication.appContext.cacheDir}cache/"

    //保存文件的路径
    const val CACHE_IMAGE_DIR = "aray/cache/devices"

    //保存的文件 采用隐藏文件的形式进行保存
    const val DEVICES_FILE_NAME = ".DEVICES"

}