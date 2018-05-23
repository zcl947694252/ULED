package com.dadoutek.uled.intf

import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.NetworkStatusCode.BAD_GATEWAY
import com.dadoutek.uled.intf.NetworkStatusCode.FORBIDDEN
import com.dadoutek.uled.intf.NetworkStatusCode.GATEWAY_TIMEOUT
import com.dadoutek.uled.intf.NetworkStatusCode.HTTP_ERROR
import com.dadoutek.uled.intf.NetworkStatusCode.INTERNAL_SERVER_ERROR
import com.dadoutek.uled.intf.NetworkStatusCode.NOT_FOUND
import com.dadoutek.uled.intf.NetworkStatusCode.REQUEST_TIMEOUT
import com.dadoutek.uled.intf.NetworkStatusCode.SERVICE_UNAVAILABLE
import com.dadoutek.uled.intf.NetworkStatusCode.UNAUTHORIZED
import com.google.gson.JsonParseException
import com.telink.TelinkApplication


import org.json.JSONException

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.text.ParseException

import retrofit2.HttpException


/**
 * Created by 12262 on 2016/5/30.
 */
object ExceptionEngine {

    fun handleException(e: Throwable): ServerException {
        val ex: ServerException
        //HTTP错误
        if (e is HttpException) {
            ex = ServerException(TelinkApplication.getInstance().getString(R.string.network_error))
        } else if (e is ServerException) {    //服务器返回的错误
            ToastUtils.showShort(e.message)
        } else if (e is JsonParseException
                || e is JSONException
                || e is ParseException) {
            ToastUtils.showShort(e.message)

        } else if (e is ConnectException) {
            ToastUtils.showShort(R.string.network_unavailable)  //均视为网络错误
        } else if (e is SocketTimeoutException) {
            ToastUtils.showShort(R.string.network_time_out)  //请求超时
        } else {
            //未知错误
            ToastUtils.showShort(e.message)
        }
    }
}
