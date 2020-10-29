package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/28 15:18
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class TimerSceneBody(
        var id: Int = 0,
        var name: String? = null,
        var hour: Int = 0,
        var min: Int = 0,
        var week: Int = 0,
        var sid: Int = 0,
        var ser_id: String? = null
) : Serializable