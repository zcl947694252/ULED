package com.dadoutek.uled.network

import com.dadoutek.uled.switches.bean.KeyBean
import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/17 16:10
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

class SwEightBody(
        var id: Int,
        var keys: List<KeyBean>,
        var ser_id: String,
        var type: Int
): Serializable