package com.liglig.base.utils

import android.text.TextUtils
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * @author liglig
 * Description: 统一日志管理
 */
object LogUtils {


        private var IS_SHOW_LOG = false
        private const val DEFAULT_MESSAGE = "execute"
        private val LINE_SEPARATOR = System.getProperty("line.separator")
        private const val JSON_INDENT = 4
        private const val TAG = "loge"
        private const val E = 0x5
        private const val JSON = 0x7

        @JvmStatic
        fun init(isShowLog: Boolean) {
            IS_SHOW_LOG = isShowLog
        }

        @JvmStatic
        fun e(msg: Any?) {
            printLog(E, TAG, msg)
        }

        @JvmStatic
        fun e(tag: String?, msg: Any?) {
            printLog(E, tag, msg)
        }

        @JvmStatic
        fun json(jsonFormat: String?) {
            printLog(JSON, TAG, jsonFormat)
        }

        @JvmStatic
        fun json(tag: String?, jsonFormat: String?) {
            printLog(JSON, tag, jsonFormat)
        }



        fun printLog(type: Int, tagStr: String?, objectMsg: Any?) {
            val msg: String
            if (!IS_SHOW_LOG) {
                return
            }
            val stackTrace = Thread.currentThread().stackTrace
            val index = 4
            val className = stackTrace[index].fileName
            var methodName = stackTrace[index].methodName
            val lineNumber = stackTrace[index].lineNumber
            val tag = tagStr ?: className
            methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1)
            val stringBuilder = StringBuilder()
            stringBuilder.append("[ (").append(className).append(":").append(lineNumber).append(")#")
                .append(methodName).append(" ] ")
            msg = objectMsg?.toString() ?: "BeFull" //"Log with null Object";
            if (type != JSON) {
                stringBuilder.append(msg)
            }
            val logStr = stringBuilder.toString()
            when (type) {
                E -> Log.e(tag, logStr)
                JSON -> {
                    if (TextUtils.isEmpty(msg)) {
                        Log.e(tag, "Empty or Null json content")
                        return
                    }
                    var message: String? = null
                    try {
                        if (msg.startsWith("{")) {
                            val jsonObject = JSONObject(msg)
                            message = jsonObject.toString(JSON_INDENT)
                        } else if (msg.startsWith("[")) {
                            val jsonArray = JSONArray(msg)
                            message = jsonArray.toString(JSON_INDENT)
                        }
                    } catch (e: JSONException) {
                        e(tag, """${e.cause!!.message}$msg""".trimIndent())
                        return
                    }
                    printLine(tag, true)
                    message = logStr + LINE_SEPARATOR + message
                    val lines = LINE_SEPARATOR?.toRegex()?.let { message.split(it).toTypedArray() }
                    val jsonContent = StringBuilder()
                    if (lines != null) {
                        for (line in lines) {
                            jsonContent.append("║ ").append(line).append(LINE_SEPARATOR)
                        }
                    }
                    //Log.i(tag, jsonContent.toString());
                    if (jsonContent.toString().length > 3200) {
                        Log.w(tag, "jsonContent.length = " + jsonContent.toString().length)
                        val chunkCount = jsonContent.toString().length / 3200
                        var i = 0
                        while (i <= chunkCount) {
                            val max = 3200 * (i + 1)
                            if (max >= jsonContent.toString().length) {
                                Log.w(tag, jsonContent.toString().substring(3200 * i))
                            } else {
                                Log.w(tag, jsonContent.toString().substring(3200 * i, max))
                            }
                            i++
                        }
                    } else {
                        Log.w(tag, jsonContent.toString())
                    }
                    printLine(tag, false)
                }
            }
        }

        fun printLine(tag: String, isTop: Boolean) {
            if (isTop) {
                Log.w(
                    tag,
                    "╔═══════════════════════════════════════════════════════════════════════════════════════"
                )
            } else {
                Log.w(
                    tag,
                    "╚═══════════════════════════════════════════════════════════════════════════════════════"
                )
            }
        }

}
