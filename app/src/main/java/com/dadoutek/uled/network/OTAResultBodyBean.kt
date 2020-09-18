package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/16 11:48
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class OTAResultBodyBean(var page: Int, var size: Int,var start: Long) : Serializable {
    override fun toString(): String {
        return "OTAResultBodyBean(page=$page, size=$size, start=$start)"
    }
}