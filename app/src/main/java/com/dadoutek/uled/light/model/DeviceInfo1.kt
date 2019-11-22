package com.dadoutek.uled.light.model

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2019/11/22 14:21
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class DeviceI(val name: String, val age: Int) : Serializable {
    var macAddress: String? = null
    /**
     * 设备名称
     */
    var deviceName: String? = null

    /**
     * 网络名称
     */
    var meshName: String? = null
    /**
     * 网络地址
     */
    var meshAddress: Int = 0
    var meshUUID: Int = 0
    /**
     * 设备的产品标识符
     */
    var productUUID: Int = 0
    var status: Int = 0
    var rssi: Int = 0
}
