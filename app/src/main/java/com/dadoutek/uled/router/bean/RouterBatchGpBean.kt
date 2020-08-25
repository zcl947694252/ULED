package com.dadoutek.uled.router.bean
import java.io.Serializable

/**
 * 创建者     ZCL
 * 创建时间   2020/8/20 15:11
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
 class RouterBatchGpBean(
    var noBoundMeshAddrs: List<Int> = listOf(),
    var offlineRouterNames: List<String> = listOf(),
    var timeout: Int = 0
):Serializable{
    override fun toString(): String {
        return "RouterBatchGpBean(noBoundMeshAddrs=$noBoundMeshAddrs, offlineRouterNames=$offlineRouterNames, timeout=$timeout)"
    }
}

