package com.dadoutek.uled.switches

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/15 17:47
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterListBody(
        var id: Long?,
        var groupMeshAddrs: List<Int>,
        var ser_id: String) : Serializable