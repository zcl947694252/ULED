package com.dadoutek.uled.router.bean

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/8/11 10:44
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class RouteInAccountBean(
        var cmd: Int = 0,
        var finish: Boolean = false,
        var msg: String = "",
        var router: Router = Router(),
        var ser_id: String = "",
        var status: Int = 0
) : Serializable {
    override fun toString(): String {
        return "RouteInAccountBean(cmd=$cmd, finish=$finish, msg='$msg', router=$router, ser_id='$ser_id', status=$status)"
    }

}

data class Router(
        var belongRegionId: Int = 0,
        var ble_version: String = "",
        var esp_version: String = "",
        var id: Int = 0,
        var lastOfflineTime: Any = Any(),
        var lastOnlineTime: Any = Any(),
        var macAddr: String = "",
        var name: String = "",
        var `open`: Int = 0,
        var productUUID: Int = 0,
        var state: Int = 0,
        var timeZoneHour: Int = 0,
        var timeZoneMin: Int = 0,
        var uid: Int = 0
) : Serializable {
    override fun toString(): String {
        return "Router(belongRegionId=$belongRegionId, ble_version='$ble_version', esp_version='$esp_version', id=$id, lastOfflineTime=$lastOfflineTime, lastOnlineTime=$lastOnlineTime, macAddr='$macAddr', name='$name', `open`=$`open`, productUUID=$productUUID, state=$state, timeZoneHour=$timeZoneHour, timeZoneMin=$timeZoneMin, uid=$uid)"
    }
}