package com.dadoutek.uled.stomp

import java.io.Serializable

data class MqttBodyBean(
        //单点
        var cmd: Int = 0,
        var loginStateKey: String = "",

        //e二维码扫描
        var rid: Int = 0,
        var type: Int = 0,
        var code: String = "",
        var region_name: String = "",
        var ref_user_phone: String = "",

        //解析code
        var authorizer_user_id: Int = 0,
        var authorizer_user_phone: String = "",

        //标签下发标签结果
        var ts: Long = 0,
        var msg: String = "",
        var status: Int = 0,
        var ser_id: String = "",
        var macAddr: String = "",

        //网关控制命令下发
        var meshAddr: Int = 0,
        var extend: Extend = Extend(),

        //网关离线
        var name: String = "",
        var productUUID: Int = 0,
        var state: Int = 0

) : Serializable{
    override fun toString(): String {
        return "MqttBodyBean(cmd=$cmd, loginStateKey='$loginStateKey', rid=$rid, type=$type, code='$code', region_name='$region_name', ref_user_phone='$ref_user_phone', authorizer_user_id=$authorizer_user_id, authorizer_user_phone='$authorizer_user_phone', ts=$ts, msg='$msg', status=$status, ser_id='$ser_id', macAddr='$macAddr', meshAddr=$meshAddr, extend=$extend)"
    }
}

class Extend : Serializable {
    var cmd: Int = 0
    var meshAddr: Int = 0
    override fun toString(): String {
        return "Extend(cmd=$cmd, meshAddr=$meshAddr)"
    }

}

