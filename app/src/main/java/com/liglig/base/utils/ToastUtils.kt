package com.liglig.base.utils

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar


object ToastUtils {
    private var toast: Toast? = null
    private var countTime = 0
    private var handler: Handler = Handler()

    fun initToast(activity: FragmentActivity?, string: String?) {
        if (activity != null) {
            initToast(activity, string, Toast.LENGTH_SHORT)
        }
    }

    fun initToast(activity: FragmentActivity?, stringId: Int) {
        if (activity != null) {
            initToast(activity, stringId, Toast.LENGTH_SHORT)
        }
    }

    private fun initToast(activity: FragmentActivity, stringId: Int, duration: Int) {
        initToast(activity, activity.getString(stringId), duration)
    }

    @SuppressLint("ShowToast")
    fun initToast(
        activity: FragmentActivity,
        string: String?,
        duration: Int
    ) {
        if (permCheckNotify(activity.applicationContext)) {
            if (toast == null) {
                toast = Toast.makeText(activity.applicationContext, string, duration)
            } else {
                toast!!.setText(string)
                toast!!.duration = duration
            }
            toast!!.setGravity(Gravity.CENTER, 0, 0)
            toast!!.show()
            postClear(duration)
        } else {
            val view: View = activity.window.findViewById(R.id.content)
            if (view != null) {
                showSnack(
                    view,
                    string,
                    if (duration == Toast.LENGTH_SHORT) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
                )
            }
        }
    }

    private fun showSnack(v: View?, string: String?, duration: Int) {
        val snack = v?.let { Snackbar.make(it, string!!, duration) }
        if (snack != null) {
            initSnack(snack.view)
            snack.show()
        }
    }

    private fun permCheckNotify(context: Context?): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            true
        } else NotificationManagerCompat.from(context!!).areNotificationsEnabled()
    }

    private fun initSnack(view: View) {
        //设置居中
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER
        view.layoutParams = lp
//        view.setBackgroundResource(R.drawable.sp2_solid_bk_0a)
//        val tv: TextView = view.findViewById(R.id.snackbar_text)
//        tv.gravity = Gravity.CENTER
    }

    /**
     * Toasts弹文字和弹View不能混用
     */
    fun initToast(activity: FragmentActivity, v: View, duration: Int) {
        if (permCheckNotify(activity.applicationContext)) {
            val toast = Toast(activity.applicationContext)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.duration = duration
            toast.setView(v)
            toast.show()
        } else {
            if (v is ViewGroup) {
                var text: String? = null
                for (i in 0 until (v as ViewGroup).childCount) {
                    val child: View = (v as LinearLayout).getChildAt(i)
                    if (child is TextView) {
                        text = (child as TextView).text.toString()
                        break
                    }
                }
                if (!TextUtils.isEmpty(text)) {
                    val view: View = activity.window.findViewById(R.id.content)
                    if (view != null) {
                        showSnack(
                            view,
                            text,
                            if (duration == Toast.LENGTH_SHORT) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
                        )
                    }
                }
            }
        }
    }


    private fun postClear(duration: Int) {
            handler.removeCallbacks(r)
        countTime = if (duration == Toast.LENGTH_SHORT) 3 else 5
        handler.post(r)
    }

    var r: Runnable = object : Runnable {
        override fun run() {
            if (handler != null) {
                if (countTime <= 0) {
                    toast = null
                } else {
                    countTime--
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    fun toastView(context: Context?, viewId: Int, iconId: Int, text: String?): View? {
        val v: View = LayoutInflater.from(context).inflate(viewId, null, false)
        if (v is LinearLayout) {
            for (i in 0 until (v as LinearLayout).childCount) {
                val child: View = (v as LinearLayout).getChildAt(i)
                if (child is ImageView) {
                    (child as ImageView).setImageResource(iconId)
                } else if (child is TextView) {
                    (child as TextView).text = text
                }
            }
        }
        return v
    }

}