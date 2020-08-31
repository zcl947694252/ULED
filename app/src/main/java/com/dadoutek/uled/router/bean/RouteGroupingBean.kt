package com.dadoutek.uled.router.bean

import java.io.Serializable

data class RouteGroupingBean(
    val cmd: Int,
    val finish: Boolean,
    val msg: String,
    val ser_id: String,
    val status: Int,
    val succeedNow: List<Int>,
    val succeedTotal: List<Int>,
    val targetGroupId: Int
):Serializable{
    override fun toString(): String {
        return "RouteGroupingBean(cmd=$cmd, finish=$finish, msg='$msg', ser_id='$ser_id', status=$status, succeedNow=$succeedNow, succeedTotal=$succeedTotal, targetGroupId=$targetGroupId)"
    }
}