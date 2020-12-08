package com.dadoutek.uled.rgb

import com.dadoutek.uled.model.dbModel.DbColorNode
import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/28 17:34
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class AddGradientBean(
        val name: String,
        val type: Int,
        val speed: Int,
        val colorNodes: List<DbColorNode>,
        val meshAddr: Int,
        val meshType: Int,
        val ser_id: String
) : Serializable {
    override fun toString(): String {
        return "AddGradientBean(name='$name', type=$type, speed=$speed, colorNodes=$colorNodes, meshAddr=$meshAddr, meshType=$meshType, ser_id='$ser_id')"
    }
}

