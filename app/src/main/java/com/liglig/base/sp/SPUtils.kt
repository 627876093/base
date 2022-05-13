package com.liglig.base.sp

import android.content.Context
import android.content.SharedPreferences
import java.util.*


object SPUtils {
    private var sp: SharedPreferences? = null

    private fun getSp(context: Context): SharedPreferences {
        if (sp == null) {
            sp = context.getSharedPreferences("SPUtils", Context.MODE_PRIVATE)
        }
        return sp!!
    }


    fun putToken(context: Context,str:String) {
        putString(context,  SpConstants.LOGINCOOKIE,str);
    }

    open fun getToken(context: Context): String? {
        return getToken(context, "");
    }

    open fun getToken(context: Context,defaultValue:String): String? {
        return getString(context,  SpConstants.LOGINCOOKIE, defaultValue);
    }


    fun getLanguage4Url(context: Context): String {
        val language = getLanguage(context)
        val lang: String
        lang = when (language) {
            "en" -> "en_US"
            "cht", "chs" -> "zh_TW"
            else -> "en_US"
        }
        return lang
    }

    fun getLanguage(context: Context): String? {
        var language: String = getString(context,  SpConstants.LOGINPWD, "default")
        if ("default" == language) {
            language = getDefaultLanguage(context)
        }
        return language
    }

    private fun getDefaultLanguage(context: Context): String {
        val language: String
        //需求.除了简体和繁体 其余全部英文
        language = if (Locale.getDefault().getLanguage().equals("zh")) {
            "cht"
        } else {
            "en"
        }
        /*  switch (Locale.getDefault().getCountry()) {
            case "CN"://大陆地区
            case "SG"://新加坡
                language = "chs";
                break;
            case "HK"://香港
            case "MO"://澳门
            case "TW"://台湾
                language = "cht";
                break;
            case "US"://美国
            case "GB"://英国
            case "AU"://澳大利亚
            default://使用英语的国家和地区多达上百个，未匹配到的一律显示英语
                language = "en";
                break;
        }*/putLanguage(context, language)
        return language
    }

    fun putLanguage(context: Context?, language: String?) {
        putString(context!!, SpConstants.LOGINPWD,  language)
    }


    /**
     * 存入字符串
     *
     * @param context 上下文
     * @param key     字符串的键
     * @param value   字符串的值
     */
    fun putString(context: Context, key: String?, value: String?) {
        val preferences = getSp(context)
        //存入数据
        val editor = preferences.edit()
        editor.putString(key, value)
        editor.commit()
    }

    /**
     * 获取字符串
     *
     * @param context 上下文
     * @param key     字符串的键
     * @return 得到的字符串
     */
    fun getString(context: Context, key: String?): String? {
        val preferences = getSp(context)
        return preferences.getString(key, "")
    }

    /**
     * 获取字符串
     *
     * @param context 上下文
     * @param key     字符串的键
     * @param value   字符串的默认值
     * @return 得到的字符串
     */
    fun getString(
        context: Context,
        key: String?,
        value: String
    ): String {
        val preferences = getSp(context)
        var str=preferences.getString(key, value)
        if (str==null){
            return value
        }
        return str
    }


    /**
     * 保存布尔值
     *
     * @param context 上下文
     * @param key     键
     * @param value   值
     */
    fun putBoolean(context: Context, key: String?, value: Boolean) {
        val sp = getSp(context)
        val editor = sp.edit()
        editor.putBoolean(key, value)
        editor.commit()
    }

    /**
     * 获取布尔值
     *
     * @param context  上下文
     * @param key      键
     * @param defValue 默认值
     * @return 返回保存的值
     */
    fun getBoolean(
        context: Context,
        key: String?,
        defValue: Boolean
    ): Boolean {
        val sp = getSp(context)
        return sp.getBoolean(key, defValue)
    }

    /**
     * 保存long值
     *
     * @param context 上下文
     * @param key     键
     * @param value   值
     */
    fun putLong(context: Context, key: String?, value: Long) {
        val sp = getSp(context)
        val editor = sp.edit()
        editor.putLong(key, value)
        editor.commit()
    }

    /**
     * 获取long值
     *
     * @param context  上下文
     * @param key      键
     * @param defValue 默认值
     * @return 保存的值
     */
    fun getLong(context: Context, key: String?, defValue: Long): Long {
        val sp = getSp(context)
        return sp.getLong(key, defValue)
    }

    /**
     * 保存int值
     *
     * @param context 上下文
     * @param key     键
     * @param value   值
     */
    fun putInt(context: Context, key: String?, value: Int) {
        val sp = getSp(context)
        val editor = sp.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    /**
     * 保存float值
     *
     * @param context 上下文
     * @param key     键
     * @param value   值
     */
    fun putFloat(context: Context, key: String?, value: Float) {
        val sp = getSp(context)
        val editor = sp.edit()
        editor.putFloat(key, value)
        editor.commit()
    }

    /**
     * 获取long值
     *
     * @param context  上下文
     * @param key      键
     * @param defValue 默认值
     * @return 保存的值
     */
    fun getInt(context: Context, key: String?, defValue: Int): Int {
        val sp = getSp(context)
        return sp.getInt(key, defValue)
    }


    /**
     * 获取float值
     *
     * @param context  上下文
     * @param key      键
     * @param defValue 默认值
     * @return 保存的值
     */
    fun getFloat(context: Context, key: String?, defValue: Float): Float {
        val sp = getSp(context)
        return sp.getFloat(key, defValue)
    }


    /**
     * 删除某个数据
     * @param context
     * @param tag
     */
    fun removeData(context: Context, tag: String?) {
        val editor = getSp(context).edit()
        editor.remove(tag)
        editor.commit()
    }

    /**
     * 删除所有数据
     * @param context
     */
    fun clearAllData(context: Context) {
        val sharedPreferences = getSp(context)
        if (sharedPreferences != null) {
            sp!!.edit().clear().commit()
        }
    }

}