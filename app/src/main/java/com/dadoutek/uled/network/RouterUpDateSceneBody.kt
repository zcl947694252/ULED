package com.dadoutek.uled.network

import com.dadoutek.uled.model.dbModel.DbSceneActions
import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/17 10:40
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterUpDateSceneBody(
        val sid: Long,
        val actions: List<DbSceneActions>,
        val ser_id: String) : Serializable