package com.dadoutek.uled.network

import retrofit2.http.Field
import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/30 9:52
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class NameBody(var name:String ,var imgName:String="",var index:Int = 0 ):Serializable {
    override fun toString(): String {
        return "NameBody(name='$name', imgName='$imgName', index=$index)"
    }
}