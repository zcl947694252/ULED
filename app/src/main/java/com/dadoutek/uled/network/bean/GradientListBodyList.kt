package com.dadoutek.uled.network.bean

import okhttp3.RequestBody
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/26 16:08
 * 描述
 */
class GradientListBodyList(
    val dcs : List<RequestBody>
) : Serializable