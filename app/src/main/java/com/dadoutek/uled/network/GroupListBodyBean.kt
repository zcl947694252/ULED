package com.dadoutek.uled.network

import com.dadoutek.uled.model.dbModel.DbGroup
import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/16 17:20
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupListBodyBean (
        val groups: List<DbGroup>
):Serializable