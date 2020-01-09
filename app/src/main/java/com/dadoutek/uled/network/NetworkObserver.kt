package com.dadoutek.uled.network

import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException


abstract class NetworkObserver<T> : Observer<T> {

    override fun onSubscribe(d: Disposable) {
    }

    override fun onError(e: Throwable) {
        Log.e("zcl", "zcl_NetworkObserver******onError${e.localizedMessage}")
        //HTTP错误
        when (e) {
            is HttpException -> ToastUtils.showLong(R.string.network_error)
            is ConnectException -> ToastUtils.showLong(R.string.network_unavailable)  //均视为网络错误
            is SocketTimeoutException -> ToastUtils.showLong(R.string.network_time_out)  //请求超时
            is ServerException -> {
                if (e.message != null && e.message != "null")
                    ToastUtils.showLong(e.message)  //服务器接口报错
            }
            else -> //未知错误
                ToastUtils.showLong(R.string.unknown_network_error)
        }
    }

    override fun onComplete() {
    }
}