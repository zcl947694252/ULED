package com.dadoutek.uled.model.routerModel

import com.dadoutek.uled.model.dbModel.DbColorNode
import com.dadoutek.uled.model.dbModel.DbSceneActions
import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/28 17:26
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class UpdateGradientBean(
    val id: Int,
    val type: Int,
    val colorNodes: List<DbColorNode>,
    val meshAddr: Int,
    val meshType: Int,
    val ser_id: String
):Serializable

