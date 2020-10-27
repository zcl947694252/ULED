package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/27 18:02
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

data class RouterTimerSceneList(
    val belongRegionId: Int,
    val hour: Int,
    val id: Int,
    val min: Int,
    val name: String,
    val sid: Int,
    val sname: String,
    val state: Int,
    val status: Int,
    val uid: Int,
    val week: Int
):Serializable