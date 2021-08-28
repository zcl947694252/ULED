package com.dadoutek.uled.network.bean

import com.dadoutek.uled.model.dbModel.DbSceneActions
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/27 10:15
 * 描述
 */
data class SceneBody(
    val id: Long,
    val name: String,
    val actions: List<DbSceneActions>,
    val index: Int,
    val imgName: String
) : Serializable