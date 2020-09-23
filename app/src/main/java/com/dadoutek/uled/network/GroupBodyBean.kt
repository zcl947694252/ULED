package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/22 10:45
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
 class GroupBodyBean(
    val deviceMeshAddrs: List<Int>,
    val meshType: Int,
    val ser_id: String,
    val targetGroupMeshAddr: Int
):Serializable