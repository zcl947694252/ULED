package com.dadoutek.uled.network

import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException


abstract class NetworkObserver<t>: Observer<t> {

    override fun onSubscribe(d: Disposable) {
    }
    override fun onError(e: Throwable) {
        Log.e("zcl", "zcl_NetworkObserver******onError${e.localizedMessage}")
        //HTTP错误
        when (e) {
            is HttpException -> ToastUtils.showShort(R.string.network_error)
            is ConnectException -> ToastUtils.showShort(R.string.network_unavailable)  //均视为网络错误
            is SocketTimeoutException -> ToastUtils.showShort(R.string.network_time_out)  //请求超时
            is ServerException -> ToastUtils.showShort(e.message)  //服务器接口报错
            else -> //未知错误
                ToastUtils.showShort(R.string.unknown_network_error)
        }
    }
    override fun onComplete() {
    }
}