package com.dadoutek.uled.network

object NetworkStatusCode {
    const val BUSY = -1
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val REQUEST_TIMEOUT = 408
    const val INTERNAL_SERVER_ERROR = 500
    const val BAD_GATEWAY = 502
    const val SERVICE_UNAVAILABLE = 503
    const val GATEWAY_TIMEOUT = 504
    const val HTTP_ERROR = 1003


    const val OK = 0
    //成功

    const val ERROR_PARAMETER = 61000
    //参数不合法

    const val ERROR_PARAM_TYPE = 61001
    //参数类型不合法

    const val ERROR_PARAM_DEVICE_CHANNEL = 61002
    //设备渠道错误

    const val ERROR_PARAM_JSON = 61003
    //json参数转化异常

    const val ERROR_PARAM_DATA_DEFINE = 61004
    //没有数据定义

    const val ERROR_PARAM_CMD_DEFINE = 61005
    //没有命令定义

    const val ERROR_PARAM_DEVICE_TYPE = 61006
    //不合法的设备类型

    const val ERROR_PARAM_CFG_DEFINE = 61007
    //没有配置参数定义


    const val ERROR_PARAMETER_MISS = 62000
    //参数缺失

    const val ERROR_PARAMETER_MISS_FILE = 62001
    //文件缺失

    const val ERROR_MISS_IMG = 62002


    const val ERROR_RUNTIME = 63000
    //操作超时

    const val ERROR_RUNTIME_SERVER = 63001
    //服务器操作超时

    const val ERROR_RUNTIME_TOKEN = 63002
    //token 过期


    const val ERROR_LIMIT = 64000
    //超过限制

    const val ERROR_LIMIT_PERMISSION = 64001
    //权限限制

    const val ERROR_LIMIT_EXT = 64002
    //文件格式限制

    const val ERROR_LIMIT_CHANNEL_PERMISSION = 64003
    //没有对该渠道的权限

    const val ERROR_LIMIT_TIME_RANGE_OVERLAPPING = 64004
    //timeRange 类型不允许时间重叠

    const val ERROR_LIMIT_TYPE = 64005
    //类型限制

    const val ERROR_LIMIT_NOTIFY = 64006
    //通知类型限制

    const val ERROR_CONTROL = 65000
    //操作失败

    const val ERROR_CONTROL_DEVICE_REGISTER = 65001
    //设备注册失败

    const val ERROR_CONTROL_DEVICE_NOT_FIND = 65002
    //设备未找到

    const val ERROR_CONTROL_VER = 65003
    //验证码

    const val ERROR_CONTROL_ACCOUNT_NOT = 20001
    //账户不存在

    const val ERROR_CONTROL_PASSWORD = 20002
    //密码错误

    const val ERROR_CONTROL_ACCOUNT_EXIST = 20000
    //账户已经存在

    const val ERROR_CONTROL_TOKEN = 65007
    //Token验证错误

    const val ERROR_CONTROL_THIRD_PART = 65008
    //第三方调用失败

    const val ERROR_CONTROL_NOT_CHANNEL = 65009
    //未找到名下的渠道

    const val ERROR_CONTROL_CMD_DEFINE = 65010
    //该命令未被定义

    const val ERROR_CONTROL_CMD_INPUT = 65011
    //该命令参数不足

    const val ERROR_CONTROL_CMD_INPUT_TYPE = 65012
    //该命令参数类型错误

    const val ERROR_CONTROL_BIND_UNIQUE = 65013
    //deviceId已经绑定过其他sn码

    const val ERROR_CONTROL_BIND_SN = 65014
    //该sn码已经绑定过其他设备

    const val ERROR_CONTROL_ALREADY_SHIP = 65015
    //设备已经发货

    const val ERROR_CONTROL_SECRET = 65016
    //密钥加密校验失败

    const val ERROR_CONTROL_SALT = 65017
    //需要先获取salt值

    const val ERROR_CONTROL_CMD_INPUT_LIMIT = 65018
    //该命令参数值限制

    const val ERROR_NOT_FIND = 66000
    //资源未找到

    const val ERROR_NOT_FIND_SN = 66001
    //SN码未找到

    const val ERROR_NOT_FIND_CHANNEL = 66002
    //渠道号未找到

    const val ERROR_NOT_FIND_CMD = 66003
    //指令未找到

    const val ERROR_NOT_FIND_KEY = 66004
    //指定的key未找到

    const val ERROR_NOT_VERSION=50001
    //最新版本

}