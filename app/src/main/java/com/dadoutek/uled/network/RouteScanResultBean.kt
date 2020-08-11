package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/8/11 15:06
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

data class RouteScanResultBean(
    var `data`: Data = Data(),
    var errorCode: Int = 0,
    var message: String = ""
):Serializable{
    override fun toString(): String {
        return "RouteScanResultBea(`data`=$`data`, errorCode=$errorCode, message='$message')"
    }
}

data class Data(
    var `data`: List<DataX> = listOf(),
    var endTime: String = "",
    var id: Int = 0,
    var rid: Int = 0,
    var scanSerId: Int = 0,
    var scanType: Int = 0,
    var startTime: String = "",
    var status: Int = 0,
    var uid: Int = 0
):Serializable{
    override fun toString(): String {
        return "Data(`data`=$`data`, endTime='$endTime', id=$id, rid=$rid, scanSerId=$scanSerId, scanType=$scanType, startTime='$startTime', status=$status, uid=$uid)"
    }
}

data class DataX(
    var belongGroupId: Int = 0,
    var belongRegionId: Int = 0,
    var boundMac: String = "",
    var brightness: Int = 0,
    var color: Int = 0,
    var colorTemperature: Int = 0,
    var id: Int = 0,
    var index: Int = 0,
    var macAddr: String = "",
    var meshAddr: Int = 0,
    var meshUUID: Int = 0,
    var name: String = "",
    var productUUID: Int = 0,
    var status: Int = 0,
    var uid: Int = 0,
    var version: String = ""
):Serializable{
    override fun toString(): String {
        return "DataX(belongGroupId=$belongGroupId, belongRegionId=$belongRegionId, boundMac='$boundMac', brightness=$brightness, color=$color, colorTemperature=$colorTemperature, id=$id, index=$index, macAddr='$macAddr', meshAddr=$meshAddr, meshUUID=$meshUUID, name='$name', productUUID=$productUUID, status=$status, uid=$uid, version='$version')"
    }
}