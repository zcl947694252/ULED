package com.dadoutek.uled.router.bean

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/10 16:14
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class RouterVersionsBean(
        var timeout: Int = 0,
        val noboundMeshAddrs: List<Int>,
        val offlineRouterNames: List<String>
) : Serializable {
    override fun toString(): String {
        return "RouterVersionsBean(timeout=$timeout, noboundMeshAddrs=$noboundMeshAddrs, offlineRouterNames=$offlineRouterNames)"
    }
}
