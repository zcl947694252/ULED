package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/8/11 16:17
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class RouterTimeoutBean(
    var timeout: Int = 0
):Serializable{
    override fun toString(): String {
        return "RouteTimeoutBean(timeout=$timeout)"
    }
}
