package com.liglig.base.base

import android.app.Dialog
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager


abstract class BaseDialog :DialogFragment(){

    protected var onItemClickListener: OnItemClickListener? = null
    protected var onShowListener: OnShowListener? = null
    protected var onDismissListener: OnDismissListener? = null

    open fun setOnItemClickListener(onItemClickListener: OnItemClickListener): BaseDialog {
        this.onItemClickListener = onItemClickListener
        return this
    }

    interface OnItemClickListener {
        fun onItemClick(v: View)
    }

    open fun setOnShowListener(onShowListener: OnShowListener): BaseDialog {
        this.onShowListener = onShowListener
        return this
    }

    interface OnShowListener {
        fun onShow()
    }

    open fun setOnDismissListener(onDismissListener: OnDismissListener): BaseDialog {
        this.onDismissListener = onDismissListener
        return this
    }

    interface OnDismissListener {
        fun onDismiss()
    }

    open fun show(context: FragmentActivity): BaseDialog {
        return show(context.supportFragmentManager)
    }

    open fun show(manager: FragmentManager): BaseDialog {
        try {
            val dialog: Dialog? = dialog
            if (dialog == null || !dialog.isShowing) {

                show(manager, "dialog")
            }
            if (onShowListener != null) {
                onShowListener!!.onShow()
            }
        } catch (e: Exception) {
        }
        return this
    }

    override fun dismiss() {
        try {
            if (onDismissListener != null) {
                onDismissListener!!.onDismiss()
            }
            super.dismiss()
        } catch (e: Exception) {
        }
    }
}