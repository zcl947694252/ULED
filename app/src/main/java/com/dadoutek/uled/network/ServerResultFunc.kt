package com.dadoutek.uled.network

import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkStatusCode.ERROR_CANCEL_AUHORIZE
import com.dadoutek.uled.network.NetworkStatusCode.ERROR_REGION_NOT_EXIST
import com.dadoutek.uled.network.NetworkStatusCode.OK
import com.telink.TelinkApplication
import io.reactivex.functions.Function

class ServerResultFunc<T> : Function<Response<T>, T> {
    override fun apply(response: Response<T>): T {

        if (response.errorCode == OK) {
            try {
                response.t as T
            }catch (ex:Exception){
                ex.printStackTrace()
            }
        } else {
            var b = response.errorCode == ERROR_CANCEL_AUHORIZE || response.errorCode == ERROR_REGION_NOT_EXIST
            SharedPreferencesHelper.putBoolean(TelinkApplication.getInstance().mContext, Constant.IS_SHOW_REGION_DIALOG, b)

            when (response.errorCode) {
                NetworkStatusCode.ERROR_CONTROL_ACCOUNT_NOT -> {
                    if (SharedPreferencesHelper.getBoolean(TelinkApplication.getInstance().mContext, Constant.IS_LOGIN, false)){

                        SharedPreferencesHelper.putBoolean(TelinkApplication.getInstance().mContext, Constant.IS_LOGIN, false)
                        ToastUtils.showLong(TelinkApplication.getInstance().mContext.getString(R.string.author_account_receviced))
                        AppUtils.relaunchApp()
                    }else{
                        ServerResultException.handleException(response)
                    }
                }
                NetworkStatusCode.ERROR_CONTROL_ACCOUNT_NOT -> {
                    response.t = "" as T
                }
                else -> {
                    ServerResultException.handleException(response)
                }
            }
        }
        return response.t
    }
}