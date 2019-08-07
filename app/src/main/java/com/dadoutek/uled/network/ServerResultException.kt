package com.dadoutek.uled.network

import com.dadoutek.uled.R
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.tellink.TelinkLightApplication

/**
 * Created by Saw on 2017/1/13 0013.
 */

object ServerResultException {
    fun handleException(response: Response<*>) {
        val code = response.errorCode

        when (code) {
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
            NetworkStatusCode.BAD_GATEWAY -> {
                //服务器异常
                throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.server_exception))
            }
            NetworkStatusCode.ERROR_NOT_VERSION->{
                //当前最新版本
                throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.the_last_version))
            }
            NetworkStatusCode.ERROR_NO_PASSOWRD->{
                throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.no_password))
            }
            NetworkStatusCode.ERROR_NO_PASSOWRD->{
                throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.no_password))
            }
            NetworkStatusCode.ERROR_CANCEL_AUHORIZE->{
                throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.cancel_authorization))
            }
            NetworkStatusCode.ERROR_EXPIRED_AUHORIZE->{
                throw  ServerException(TelinkLightApplication.getInstance().getString(R.string.authorization_exprize))
            }
            else -> {
                throw RuntimeException(response.message)
            }
        }
    }
}
