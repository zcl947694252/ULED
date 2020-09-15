package com.dadoutek.uled.base

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/15 12:01
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class RouterOTAFinishBean(
    val cmd: Int,
    val finish: Boolean,
    val msg: String,
    val ser_id: String,
    val start: Long,
    val status: Int
):Serializable{
    override fun toString(): String {
        return "RouterOTAFinishBean(cmd=$cmd, finish=$finish, msg='$msg', ser_id='$ser_id', start=$start, status=$status)"
    }
}