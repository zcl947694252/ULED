package com.dadoutek.uled.network

import com.dadoutek.uled.model.dbModel.DbColorNode
import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/12/8 10:49
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

class UpdateGradientNameSpeedBean(
        val index: Int = 1,
        val name: String,
        val speed: Int
): Serializable