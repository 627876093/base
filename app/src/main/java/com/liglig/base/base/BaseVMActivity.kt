package com.liglig.base.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.*
import com.gyf.immersionbar.ImmersionBar
import com.liglig.base.BR
import com.liglig.base.utils.ActivityManager
import com.liglig.base.view.IBaseView
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * created by liglig on 2021/4/9 0009
 * Description: BaseActivity
 */
abstract class BaseVMActivity<VB : ViewDataBinding, VM : BaseViewModel> : AppCompatActivity() ,
    IBaseView  {

     lateinit var binding: VB
     lateinit var viewModel: VM
        val mHandler: Handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        //统一设置activity竖屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        initDataBinding()
        initViewModel()

        ActivityManager.addActivity(this) //创建Activity入栈管理

        statusBaySetting()

        initView()
        initData()
    }


    //封装反射ViewBind
    open fun initDataBinding() {
        val type: Type = this.javaClass.genericSuperclass
        if (type is ParameterizedType) {
            //如果支持泛型
            try {
                //获得泛型中的实际类型，可能会存在多个泛型，[0]也就是获得T的type
                val clazz: Class<VB> =
                    (type as ParameterizedType).actualTypeArguments[0] as Class<VB>
                //反射inflate
                val method: Method = clazz.getMethod("inflate", LayoutInflater::class.java)
                //方法调用，获得ViewBinding实例
                binding = method.invoke(null, layoutInflater) as VB
            } catch (e: Exception) {
                e.printStackTrace()
            }
            assert(binding != null)
            setContentView(binding.root)
        }
    }


    /**
     * activity系统状态栏封装，并设置默认样式
     */
    open fun statusBaySetting() {
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                ImmersionBar.with(this@BaseVMActivity)
                    .statusBarColor(setStatusBarColor())
                    .statusBarDarkFont(isDarkFont())
                    .fitsSystemWindows(true)
                    .init()
            }
        })
    }

    protected abstract fun setStatusBarColor():Int

    /**
     * 设置状态栏颜色，默认黑色
     */
    open fun isDarkFont(): Boolean = true

    private fun initViewModel() {
        viewModel = createViewModel()
        binding.setVariable(getBindingVariable(), viewModel)
        binding.lifecycleOwner = this
    }

    /**
     * 创建viewModel
     */
    private fun createViewModel(): VM {
//        return ViewModelProvider(this).get(genericTypeViewModel())
        return ViewModelProvider(this,ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(genericTypeViewModel())
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

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.removeActivity(this) //销毁Activity移出栈
    }
}