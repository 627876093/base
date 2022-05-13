package com.liglig.base.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.lang.reflect.Type
import kotlin.jvm.Throws


class GsonUtils {

    companion object{

        private val gson: Gson =GsonBuilder().create()

        fun getGson():Gson= gson

        @Throws(JsonSyntaxException::class)
        fun <T> fromJson(json: String?, typeOfT: Type?): T? {
            return if (json == null) {
                null
            } else gson.fromJson(json, typeOfT)
        }


        /**
         * 如果解析抛异常返回null
         */
        fun <T> fromJson(string: String?, tClass: Class<T>?): T? {
            return try {
                gson.fromJson(string, tClass)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }


        fun toJson(src: Any?): String {
            return gson.toJson(src)
        }
    }
}