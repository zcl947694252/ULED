package com.dadoutek.uled.intf

import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Response
import com.google.gson.JsonParseException
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import org.json.JSONException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.text.ParseException


abstract class NetworkObserver<t>() : Observer<t> {

    override fun onSubscribe(d: Disposable) {

    }

    override fun onError(e: Throwable) {
        val ex: ServerException
        //HTTP错误
        if (e is HttpException) {
            ToastUtils.showShort(R.string.network_error)
        } else if (e is ConnectException) {
            ToastUtils.showShort(R.string.network_unavailable)  //均视为网络错误
        } else if (e is SocketTimeoutException) {
            ToastUtils.showShort(R.string.network_time_out)  //请求超时
        } else {
            //未知错误
            ToastUtils.showShort(e.message)
        }
    }


    override fun onComplete() {
    }
}