package com.dadoutek.uled.network

import com.dadoutek.uled.switches.bean.KeyBean
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/13 15:54
 * 描述
 */
class SwFourAndSixSwBody(
    var ser_id: String,
    var id: Int,
    var type: Int,
    var keys: List<KeyBean>
): Serializable