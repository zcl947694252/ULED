package com.dadoutek.uled.intf

import com.dadoutek.uled.R
import com.google.gson.JsonParseException


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
        val ex: ApiException
        if (e is HttpException) {             //HTTP错误
            ex = ApiException(e, HTTP_ERROR)
            when (e.code()) {
                UNAUTHORIZED, FORBIDDEN, NOT_FOUND, REQUEST_TIMEOUT, GATEWAY_TIMEOUT, INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE -> ex.setDisplayMessage(App.getInstance().getString(R.string.network_error))  //均视为网络错误
                else -> ex.setDisplayMessage(App.getInstance().getString(R.string.network_error))
            }
            return ex
        } else if (e is ServerException) {    //服务器返回的错误
            ex = ApiException(e, e.getCode())
            ex.setDisplayMessage(e.getMsg())
            return ex
        } else if (e is JsonParseException
                || e is JSONException
                || e is ParseException) {
            ex = ApiException(e, REQUEST_ARGS)
            ex.setDisplayMessage(App.getInstance().getString(R.string.data_format_error))

            //均视为解析错误
            return ex
        } else if (e is ConnectException) {
            ex = ApiException(e, NOT_FOUND)
            ex.setDisplayMessage(App.getInstance().getString(R.string.network_unavailable))  //均视为网络错误
            return ex
        } else if (e is SocketTimeoutException) {
            ex = ApiException(e, REQUEST_TIMEOUT)
            ex.setDisplayMessage(App.getInstance().getString(R.string.network_time_out))  //请求超时
            return ex
        } else {
            ex = ApiException(e, BUSY)
            ex.setDisplayMessage(App.getInstance().getString(R.string.unkown_error))
            //未知错误
            return ex
        }
    }
}
