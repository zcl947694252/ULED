package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/27 18:02
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

data class RouterTimerSceneBean(
    var belongRegionId: Int,
    var hour: Int,
    var id: Int,
    var min: Int,
    var name: String,
    var sid: Int,
    var sname: String,
    var state: Int,
    var status: Int,
    var uid: Int,
    var week: Int
):Serializable