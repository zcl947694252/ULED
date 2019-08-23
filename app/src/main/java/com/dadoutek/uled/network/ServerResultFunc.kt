package com.dadoutek.uled.network

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
            if (response.t == null) {
                response.t = "" as T
            }
        } else {
            if (response.errorCode == ERROR_CANCEL_AUHORIZE || response.errorCode == ERROR_REGION_NOT_EXIST) {
                val b = response.errorCode == ERROR_CANCEL_AUHORIZE || response.errorCode == ERROR_REGION_NOT_EXIST
                SharedPreferencesHelper.putBoolean(TelinkApplication.getInstance().mContext, Constant.IS_SHOW_REGION_DIALOG, b)
            }else{
                ServerResultException.handleException(response)
            }
        }
        return response.t
    }
}