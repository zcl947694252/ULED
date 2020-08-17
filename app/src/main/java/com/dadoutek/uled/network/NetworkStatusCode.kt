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

    const val ERROR_CLIEN=50000
    //服务器异常

    const val ERROR_NO_PASSOWRD = 20007
    //没有密码

    const val ERROR_CANCEL_AUHORIZE= 20019
    //授权用户取消授权
    const val ERROR_UNAUHORIZE_LEVE= 20020
    //未收录的授权等级:
    const val ERROR_REGION_MORE_CODE = 20021
    //单个区域同时只能生成一种码
     const val ERROR_CAN_NOT_PARSE = 20022
    //无法解析: 20022
     const val ERROR_CAN_NOT_PARSE_MINE = 20023
    //不能解析自己生成的码: 20023
    const val ERROR_ACCEPT_AUTHORIZATION = 20024
    //该用户已接受授权: 20024
    const val ERROR_EXPIRED_AUHORIZE= 20025
    //20025 码已过期
    const val ERROR_USER_LOCKED= 20026
    //用户被锁定: 20026
    const val ERROR_CODE_FAILURE = 20027
    //20025 二维码已失效
    const val ERROR_PERMISSION_DENFINED = 20028
    // 权限不足,无法操作: 20028
    const val ERROR_REGION_NOT_EXIST = 30000
    //不存在该类型bin文件
    const val ERROR_BIN_NOT_EXIST = 50000
    //没有比当前版本更新bin文件
    const val ERROR_BIN_NO_NEW = 50001
    //资源服务器上无该bin文件,请联系管理员
    const val ERROR_BIN_NO_BIN = 50002
    //没有比当前更新的app版本无需更新
    const val ERROR_NO_NEW_VERSION = 60001
    //mesh密码错误
    const val ERROR_MESH_ERROR = 70000
    //未收录的type
    const val ERROR_UNKOWN_TYPE = 70001
    //mesh地址不够
    const val ERROR_MESH_NOT_ENOUGH= 70002
    //该网关不存在/账号下无网关 80000
    const val ERROR_GW_OR_ACCOUNT_NOT_EXIST= 80000
    //该网关不在线/网关全部不在线 80001
    const val ERROR_GW_UNONLIN= 80001
    //base64字符串解码失败 80002
    const val ERROR_GW_BASE64= 80002
    //服务忙 501
    const val ERROR_SERVER_BUSYNEWSS= 501

    //服务崩溃 502
    const val ERROR_SERVER_CRASH= 502
    //路由未上线 90001
    const val ROUTER_OFFLINE= 90001
    //该设备不是路由设备，请联系生产方 90010
    const val ROUTER_NOT_RIGHT= 90010
    //路由已添加 90002
    const val ROUTER_IS_EXITE= 90002
    //路由已被其他人添加，你不能添加
    const val ROUTER_OTHER_ADD= 90003

    /**
     * 请求成功处理: 0
    服务器出错: 500
    参数异常: 10000
    参数没有通过正则验证: 10001
    数据库操作时抛出错误,数据操作失败: 10002
    JSESSIONID过期: 10003
    用户已存在: 20000
    用户不存在: 20001
    密码错误: 20002
    盐值错误: 请先获取盐值或盐值错误或盐值已过期 : 20003
    请求头中不包含token: 20004
    用户不是管理员: 20006
    用户密码为空: 20007
    电话号码为空: 20008
    授权信息不存在或授权者取消了授权: 20019

    不能解析自己生成的码: 20023
    该用户已接受授权: 20024
    码已过期: 20025

    码已失效: 20027
    权限不足,无法操作: 20028
    该区域不存在: 30000
     */

}