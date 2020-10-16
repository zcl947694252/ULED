package com.dadoutek.uled.router.bean

import java.io.Serializable

data class RouteGroupingOrDelOrGetVerBean(
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