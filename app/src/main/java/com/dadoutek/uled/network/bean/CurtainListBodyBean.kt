package com.dadoutek.uled.network.bean

import com.dadoutek.uled.model.dbModel.DbCurtain
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/26 16:05
 * 描述
 */
class CurtainListBodyBean(
    val curtains : List<DbCurtain>
) : Serializable