package com.dadoutek.uled.base

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2019/9/4 12:00
 * 描述	      df
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述   df
 */
data class CancelAuthorMsg(
        var authorizer_user_id: Int,
        var authorizer_user_phone: String,
        var region_name: String,
        var rid: Int
) : Serializable