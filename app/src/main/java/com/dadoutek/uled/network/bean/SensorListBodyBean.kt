package com.dadoutek.uled.network.bean

import com.dadoutek.uled.model.dbModel.DbSensor
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/26 16:03
 * 描述
 */
class SensorListBodyBean(
    val sensors : List<DbSensor>
) : Serializable