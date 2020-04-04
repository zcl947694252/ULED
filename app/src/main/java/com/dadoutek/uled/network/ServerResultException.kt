package com.dadoutek.uled.network

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.tellink.TelinkLightApplication

/**
 * Created by Saw on 2017/1/13 0013.
 */

object ServerResultException {
    fun handleException(response: Response<*>) {
        when (response.errorCode) {
            NetworkStatusCode.ERROR_RUNTIME_TOKEN -> {
                //token 过期
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.login_timeout))
            }
            NetworkStatusCode.ERROR_CONTROL_ACCOUNT_NOT -> {
                //账户不存在
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.account_not_exist))
            }
            NetworkStatusCode.ERROR_CONTROL_PASSWORD -> {
                //密码错误
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.name_or_password_error))
            }
            NetworkStatusCode.ERROR_CONTROL_ACCOUNT_EXIST -> {
                //账户已经存在
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.account_exist))
            }
            NetworkStatusCode.ERROR_CONTROL_TOKEN -> {
                //Token验证错误
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.login_timeout))
            }
            NetworkStatusCode.BAD_GATEWAY -> {
                //服务器异常
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.server_exception))
            }
            NetworkStatusCode.ERROR_NOT_VERSION -> {
                //当前最新版本
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.the_last_version))
            }
            NetworkStatusCode.ERROR_NO_PASSOWRD -> {
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.no_password))
            }
            NetworkStatusCode.ERROR_NO_PASSOWRD -> {
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.no_password))
            }
            NetworkStatusCode.ERROR_CANCEL_AUHORIZE -> {//授权信息不存在或授权者取消了授权: 20019  需要弹框
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.cancel_authorization))
            }
            NetworkStatusCode.ERROR_EXPIRED_AUHORIZE -> { //20025 码已过期
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.authorization_exprize))
            }
            NetworkStatusCode.ERROR_UNAUHORIZE_LEVE -> {// 未收录的授权等级:20020
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.unauthorization_leve))
            }
            NetworkStatusCode.ERROR_REGION_MORE_CODE -> {//单个区域同时只能生成一种码
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.region_one_code))
            }
            NetworkStatusCode.ERROR_CAN_NOT_PARSE -> {//无法解析: 20022
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.can_not_parse))
            }
            NetworkStatusCode.ERROR_CAN_NOT_PARSE_MINE -> { //不能解析自己生成的码: 20023
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.can_not_parse_mine))
            }
            NetworkStatusCode.ERROR_ACCEPT_AUTHORIZATION -> { //该用户已接受授权: 20024
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.accept_authrozation))
            }

            NetworkStatusCode.ERROR_USER_LOCKED -> {//用户被锁定: 20026
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.user_locked))
            }
            NetworkStatusCode.ERROR_CODE_FAILURE -> {//20025 二维码已失效
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.QR_expired))
            }
            NetworkStatusCode.ERROR_PERMISSION_DENFINED -> {// 权限不足,无法操作: 20028
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.permission_denfied))
            }
            NetworkStatusCode.ERROR_REGION_NOT_EXIST -> {//30000  该区域不存在  需要弹框
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.region_not_exist))
            }  NetworkStatusCode.ERROR_BIN_NO_NEW -> {   //没有比当前版本更新bin文件50001
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.no_have_bin))
            }  NetworkStatusCode.ERROR_BIN_NO_BIN -> {//资源服务器上无该bin文件,请联系管理员50002
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.no_this_bin))
            }  NetworkStatusCode.ERROR_NO_NEW_VERSION -> {//没有比当前更新的app版本无需更新60001
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.no_new_app_version))
            }  NetworkStatusCode.ERROR_MESH_ERROR -> {//mesh密码错误70000
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.mesh_error))
            }  NetworkStatusCode.ERROR_UNKOWN_TYPE -> {//未收录的type70001
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.no_have_type))
            }

            NetworkStatusCode.ERROR_MESH_NOT_ENOUGH -> { //mesh地址不够70002
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.ERROR_MESH_NOT_ENOUGH))
            }  NetworkStatusCode.ERROR_GW_OR_ACCOUNT_NOT_EXIST -> {//该网关不存在/账号下无网关 80000
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.ERROR_GW_OR_ACCOUNT_NOT_EXIST))
            }  NetworkStatusCode.ERROR_GW_UNONLIN -> {//该网关不在线/网关全部不在线 80001
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.ERROR_GW_UNONLIN))
            }  NetworkStatusCode.ERROR_GW_BASE64 -> {//base64字符串解码失败 80002
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.ERROR_GW_BASE64))
            }NetworkStatusCode.ERROR_SERVER_BUSYNEWSS -> {//服务忙 501
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.ERROR_SERVER_BUSYNEWSS))
            } NetworkStatusCode.ERROR_SERVER_CRASH -> {//服务器崩溃 502
                throw  ServerException(TelinkLightApplication.getApp().getString(R.string.ERROR_SERVER_CRASH))
            }
            else -> {
                //throw RuntimeException(response.message)
                LogUtils.e("zcl-------RuntimeException---${response.message}")
            }
        }
    }
}
