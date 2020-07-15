package com.dadoutek.uled.gateway.bean

import org.greenrobot.greendao.annotation.Id
import java.io.Serializable
import java.util.*

/**
 * 创建者     ZCL
 * 创建时间   2020/7/15 10:44
 * 描述
 *
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class DbRouter : Serializable {
    /**
     * belongRegionId : 1
     * ble_version : ble-1.0.0
     * esp_version : esp-1.0.0
     * id : 1
     * macAddr : 0102030405
     * name : 路由:0102030405
     * open : 1
     * productUUID : 50
     * state : 0
     * uid : 300133
     */
    @Id(autoincrement = true)
    var id = 0
    var belongRegionId = 0
    var ble_version: String? = null
    var esp_version: String? = null
    var macAddr: String? = null
    var name: String? = null
    var open = 0
    var productUUID = 0
    var state = 0
    var uid = 0
    val lastOnlineTime: Date? = null
    val lastOfflineTime: Date? = null
}