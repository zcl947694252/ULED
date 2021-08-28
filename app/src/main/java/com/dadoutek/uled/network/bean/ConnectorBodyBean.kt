package com.dadoutek.uled.network.bean

import com.dadoutek.uled.model.dbModel.DbConnector
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/26 15:58
 * 描述
 */
class ConnectorBodyBean(
    val relays : List<DbConnector>
) : Serializable