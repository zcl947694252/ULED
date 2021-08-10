package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2021/7/22 9:44
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class TimerSceneUpdateBody(
    var id: Int = 0,
    var hour: Int = 0,
    var min: Int = 0,
    var week: Int = 0,
    var sid: Int = 0,
    var ser_id: String? = null,
    var routerMacAddr: String? = null
) : Serializable