package com.dadoutek.uled.router.bean

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/3 17:05
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

class ScanDataBean(
        private val offlineRouterNames: List<String>,
        val timeout: Int
) : Serializable {
    override fun toString(): String {
        return "ScanDataBean(offlineRouterNames=$offlineRouterNames, timeout=$timeout)"
    }
}