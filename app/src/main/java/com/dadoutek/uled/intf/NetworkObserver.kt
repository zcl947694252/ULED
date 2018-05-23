package com.dadoutek.uled.intf

import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.model.Response
import com.google.gson.JsonParseException
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import org.json.JSONException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.text.ParseException


abstract class NetworkObserver<t>() : Observer<Response<t>> {

    override fun onSubscribe(d: Disposable) {

    }

    override fun onNext(t: Response<t>) {
        if (t.errorCode != NetworkStatusCode.OK)
            when (t.errorCode) {
                NetworkStatusCode.ERROR_RUNTIME_TOKEN -> {
                    //token 过期
                    throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.login_timeout))
                }
                NetworkStatusCode.ERROR_CONTROL_ACCOUNT_NOT -> {
                    //账户不存在
                    throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.account_not_exist))
                }
                NetworkStatusCode.ERROR_CONTROL_PASSWORD -> {
                    //密码错误
                    throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.name_or_password_error))
                }
                NetworkStatusCode.ERROR_CONTROL_ACCOUNT_EXIST -> {
                    //账户已经存在
                    throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.account_exist))
                }
                NetworkStatusCode.ERROR_CONTROL_TOKEN -> {
                    //Token验证错误
                    throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.login_timeout))
                }
                else -> {
                    throw RuntimeException(TelinkLightApplication.getInstance().getString(R.string.unknown_network_error))
                }


            }
    }

    override fun onError(e: Throwable) {
        val ex: ServerException
        //HTTP错误
        if (e is HttpException) {
            ToastUtils.showShort(R.string.network_error)

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


    override fun onComplete() {
    }
}