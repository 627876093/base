package com.liglig.base.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gyf.immersionbar.ImmersionBar
import com.liglig.base.BR
import com.liglig.base.view.IBaseView
import java.lang.Boolean
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


/**
 * created by liglig on 2021/4/21 0021
 * Description:
 */
abstract class BaseVMFragment<VB : ViewDataBinding, VM : BaseViewModel> : Fragment(),
    IBaseView {

    lateinit var mImmersionBar: ImmersionBar
    lateinit var binding: VB
    lateinit var viewModel: VM
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
        initViewModel()
        initView()
        initData()
    }

    private fun initViewModel() {
        viewModel = createViewModel()
        binding.setVariable(getBindingVariable(), viewModel)
    }

    /**
     * 创建viewModel
     */
    private fun createViewModel(): VM {
        return ViewModelProvider(this).get(genericTypeViewModel())
    }

    /**
     * 获取参数Variable
     */
    private fun getBindingVariable() = BR._all

    /**
     * 获取当前类泛型viewmodel的Class类型
     * @return
     */
    private fun genericTypeViewModel(): Class<VM> {
        return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>
    }

    private fun initImmersionBar() {
        mImmersionBar = ImmersionBar.with(this)
        mImmersionBar.init()
    }

}