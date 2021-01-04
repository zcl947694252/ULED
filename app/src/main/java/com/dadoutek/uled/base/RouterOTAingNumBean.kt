package com.dadoutek.uled.base

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/15 11:58
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class RouterOTAingNumBean(
    val cmd: Int,
    val finish: Boolean,
    val msg: String,
    val otaResult: OtaResult,
    val start: Long,
    val status: Int
):Serializable{
    override fun toString(): String {
        return "RouterOTAingNumBean(cmd=$cmd, finish=$finish, msg='$msg', otaResult=$otaResult, start=$start, status=$status)"
    }
}

data class OtaResult(
    val failedCode: Int,
    val finish: Long,
    val fromVersion: String,
    val id: Int,
    val macAddr: String,
    val name: String,
    val productUUID: Int,
    val rid: Int,
    val start: Long,
    val status: Int,
    val toVersion: String,
    val uid: Int
):Serializable{
    override fun toString(): String {
        return "OtaResult(failedCode=$failedCode, finish=$finish, fromVersion='$fromVersion', id=$id, macAddr='$macAddr', name='$name', productUUID=$productUUID, rid=$rid, start=$start, status=$status, toVersion='$toVersion', uid=$uid)"
    }
}