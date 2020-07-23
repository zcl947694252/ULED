package com.dadoutek.uled.stomp

data class MqttBodyBean(
    var authorizer_user_id: Int = 0,
    var authorizer_user_phone: String = "",
    var cmd: Int = 0,
    var code: String = "",
    var extend: String = "",
    var loginStateKey: String = "",
    var macAddr: String = "",
    var meshAddr: Int = 0,
    var msg: String = "",
    var ref_user_phone: String = "",
    var region_name: String = "",
    var rid: Int = 0,
    var ser_id: String = "",
    var status: Int = 0,
    var ts: Long = 0,
    var type: Int = 0
)