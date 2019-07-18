package com.dadoutek.uled.network

import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException


abstract class NetworkObserver<t>() : Observer<t> {

    override fun onSubscribe(d: Disposable) {

    }

    override fun onError(e: Throwable) {

        //HTTP错误
        if (e is HttpException) {
            ToastUtils.showShort(R.string.network_error)
        } else if (e is ConnectException) {
            ToastUtils.showShort(R.string.network_unavailable)  //均视为网络错误
        } else if (e is SocketTimeoutException) {
            ToastUtils.showShort(R.string.network_time_out)  //请求超时
        } else if (e is ServerException) {
            ToastUtils.showShort(e.message)  //服务器接口报错
        }  else {
            //未知错误
            ToastUtils.showShort(R.string.unknown_network_error)
        }
    }


    override fun onComplete() {
    }
}