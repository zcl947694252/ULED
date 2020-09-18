package com.dadoutek.uled.switches.bean

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/18 17:40
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class KeyBean(
    val featureId: Int,
    val keyId: Int,
    val name: String,
    val reserveValue_A: Int,
    val reserveValue_B: Int
):Serializable{
    override fun toString(): String {
        return "KeyBean(featureId=$featureId, keyId=$keyId, name='$name', reserveValue_A=$reserveValue_A, reserveValue_B=$reserveValue_B)"
    }
}