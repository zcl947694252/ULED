package com.dadoutek.uled.router.bean

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/27 14:54
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
 class MacResetBody(
    val macAddr: String,
    val meshAddr: Int,
    val meshType: Int,
    val ser_id: String

):Serializable{
    override fun toString(): String {
        return "MacResetBody(macAddr='$macAddr', meshAddr=$meshAddr, meshType=$meshType, ser_id='$ser_id')"
    }
}