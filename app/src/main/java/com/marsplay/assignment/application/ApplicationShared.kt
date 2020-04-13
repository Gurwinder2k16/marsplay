package com.marsplay.assignment.application

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.marsplay.assignment.R
import com.marsplay.assignment.di.component.DaggerViewModelComponent
import com.marsplay.assignment.network.networkutil.apiConfig.NetworkUtilites
import com.marsplay.assignment.utiles.ProgressBarDialog
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject


class ApplicationShared : Application(), HasAndroidInjector, CameraXConfig.Provider  {

    init {
        mCurrentInstance = this
    }

    /*For Activity Life Cycle Maintian*/
    var mActivity: AppCompatActivity? = null
    private var mProgressDialog: ProgressBarDialog? = null
    private var mAlertDialog: AlertDialog? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        var mCurrentInstance: ApplicationShared? = null

        fun getApplicationContext(): Context {
            return mCurrentInstance!!.applicationContext
        }
    }

    fun showAlert(pMessage: String, pContext: Context?) {
        if (pContext != null) {
            if (this.mAlertDialog == null || !this.mAlertDialog!!.isShowing) {
                val builder = AlertDialog.Builder(pContext)
                builder.setTitle(pContext.getString(R.string.app_name))
                        .setMessage(pMessage)
                        .setCancelable(false)
                        .setPositiveButton(pContext.resources.getString(R.string.ok))
                        { dialog, _ ->
                            dialog.dismiss()
                        }
                mAlertDialog = builder.create()
                mAlertDialog!!.show()
            }
        }
    }

    fun setActivity(pActivity: AppCompatActivity) {
        mActivity = pActivity
    }

    fun getContext(): Context? {
        when (mActivity == null) {
            true -> return applicationContext
        }
        return mActivity
    }

    fun getmActivity(): AppCompatActivity? {
        return mActivity
    }

    fun isConnected(): Boolean {
        if (mActivity != null && !mActivity!!.isFinishing) {
            if (!NetworkUtilites.isInternetAvailable(mActivity!!) && !mActivity!!.isFinishing) {
                showAlert(mActivity!!.resources.getString(R.string.check_connection), mActivity)
                return false
            }
        } else {
            return false
        }
        return true
    }

    fun showProgress(pContext: Context? = getmActivity()) {
        // If our activity is going to finish then we don't need to show any progress.
        if (pContext != null) {
            val activity = pContext as AppCompatActivity?
            if (activity!!.isFinishing)
                return
            if (this.mProgressDialog == null) {
                mProgressDialog = ProgressBarDialog.newInstance()
                mProgressDialog?.isCancelable = false
            }
            when (mProgressDialog?.isVisible) {
                false -> {
                    mProgressDialog?.show(
                            activity.supportFragmentManager,
                            activity.javaClass.simpleName
                    )
                }
            }
        }
    }

    fun hideProgressDialog() {
        if (this.mProgressDialog != null && this.mProgressDialog!!.isVisible) {
            this.mProgressDialog!!.dismiss()
            this.mProgressDialog = null
        }
    }

    @Inject lateinit var androidInjector : DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        DaggerViewModelComponent
                .builder()
                .application(this)
                .build()
                .inject(this)
    }
    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }
}
