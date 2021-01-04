package com.dadoutek.uled.router

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/22 15:38
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
 class GroupBlinkBodyBean(
    val meshAddrs: List<Int>,
    val meshType: Int
):Serializable