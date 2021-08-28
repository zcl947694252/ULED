package com.dadoutek.uled.network.bean

import com.dadoutek.uled.model.dbModel.DbLight
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/26 15:31
 * 描述
 */
class LightListBodyBean(
    val lights: List<DbLight>
):Serializable {
    override fun toString(): String {
        return lights.toString()
    }
}