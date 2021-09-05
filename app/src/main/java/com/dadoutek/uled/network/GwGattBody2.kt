package com.dadoutek.uled.network


/**
 * 创建者     Chown
 * 创建时间   2021/9/5 17:31
 * 描述
 */
class GwGattBody2 {
    var ser_id: Int = 0//app会话id，成功or失败会回传给app
    var data: String? = null //base64编码后的指令

    //下发命令给网关转发
    var cmd :Int =0
    var meshAddr :Int =0

}