package com.dadoutek.uled.network.bean

import com.dadoutek.uled.model.dbModel.DbColorNode
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/27 10:41
 * 描述
 */
class GradientBody(
    val id: Long,
    val name: String,
    val type: Int,
    val speed: Int,
    val colorNodes: List<DbColorNode>,
    val index: Int
) : Serializable