package com.dadoutek.uled.base


/**
 * 创建者     ZCL
 * 创建时间   2020/10/17 15:43
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

import java.io.Serializable

data class RouteGetVerBean(
        val cmd: Int,
        val finish: Boolean,
        val msg: String,
        val ser_id: String,
        val status: Int,
        val succeedNow: List<SucceedNow>,
        val succeedTotal: List<SucceedTotal>,
        val targetGroupId: Int
):Serializable{
    override fun toString(): String {
        return "RouteGroupingOrDelOrGetVerBean(cmd=$cmd, finish=$finish, msg='$msg', ser_id='$ser_id', status=$status, succeedNow=$succeedNow, succeedTotal=$succeedTotal, targetGroupId=$targetGroupId)"
    }
}

data class SucceedNow(
        val meshAddr: Int,
        val version: String
):Serializable{
    override fun toString(): String {
        return "SucceedNow(meshAddr=$meshAddr, version='$version')"
    }
}

data class SucceedTotal(
        val `ref`: String
):Serializable{
    override fun toString(): String {
        return "SucceedTotal(`ref`='$`ref`')"
    }
}