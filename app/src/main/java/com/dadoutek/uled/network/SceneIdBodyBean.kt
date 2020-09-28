package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/27 17:10
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
 class SceneIdBodyBean(
    val ser_id: String,
    val sid: Int
):Serializable{
    override fun toString(): String {
        return "SceneBodyBean(ser_id='$ser_id', sid=$sid)"
    }
}