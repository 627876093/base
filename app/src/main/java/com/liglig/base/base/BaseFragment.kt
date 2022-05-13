package com.liglig.base.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gyf.immersionbar.ImmersionBar
import com.liglig.base.BR
import com.liglig.base.view.IBaseView
import java.lang.Boolean
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

abstract class BaseFragment<VB : ViewDataBinding> : Fragment(),
    IBaseView {

    lateinit var mImmersionBar: ImmersionBar
    lateinit var binding: VB
    val mHandler: Handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        initImmersionBar()
        val type: Type = this.javaClass.genericSuperclass
        if (type is ParameterizedType) {
            try {
                val clazz: Class<VB> =
                    (type as ParameterizedType).actualTypeArguments[0] as Class<VB>
                val method = clazz.getMethod(
                    "inflate",
                    LayoutInflater::class.java,
                    ViewGroup::class.java, Boolean.TYPE
                )
                binding = method.invoke(null, this.layoutInflater, container, false) as VB
                binding.lifecycleOwner = this
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        assert(binding != null)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
    }

    private fun initImmersionBar() {
        mImmersionBar = ImmersionBar.with(this)
        mImmersionBar.init()
    }

}