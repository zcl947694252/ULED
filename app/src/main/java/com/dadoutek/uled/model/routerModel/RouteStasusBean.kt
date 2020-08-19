package com.dadoutek.uled.model.routerModel


/**
 * 创建者     ZCL
 * 创建时间   2020/8/19 11:36
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class RouteStasusBean(
    var `data`: Data = Data(),
    var errorCode: Int = 0,
    var message: String = ""
)

data class Data(
    var `data`: DataX = DataX(),
    var status: Int = 0
)


data class DataX(
    var endTime: String = "",
    var scanSerId: Int = 0,
    var scanType: Int = 0,
    var scanedData: List<ScanedData> = listOf(),
    var otaData: List<OtaData> = listOf(),
    var startTime: String = ""
)

data class ScanedData(
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
)

data class OtaData(
    var failedCode: Int = 0,
    var finish: Long = 0,
    var fromVersion: String = "",
    var id: Int = 0,
    var macAddr: String = "",
    var name: String = "",
    var productUUID: Int = 0,
    var result_id: Int = 0,
    var rid: Int = 0,
    var start: Long = 0,
    var status: Int = 0,
    var toVersion: String = "",
    var uid: Int = 0
)