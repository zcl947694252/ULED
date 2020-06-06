package com.dadoutek.uled.gateway.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.UnsupportedEncodingException
import java.lang.UnsupportedOperationException
import java.util.*


/**
 * 创建者     ZCL
 * 创建时间   2020/6/5 14:31
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
object Base64Utils {
    @RequiresApi(Build.VERSION_CODES.O)
   open fun encodeToStrings(gattPar: ByteArray): String {
        var decode = ""
        try {
            decode = Base64.getEncoder().encodeToString(gattPar)
        } catch (ex: UnsupportedEncodingException) {
            ex.printStackTrace()
        }
        return decode
    }
}