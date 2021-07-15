package com.dadoutek.uled.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.telink.util.Strings
import java.util.*


/**
 * 创建者     ZCL
 * 创建时间   2021/7/7 20:47
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class DeviceUtil {
    companion object {
        fun getIMEI(context: Context): String {
            if (context.applicationInfo.targetSdkVersion >= 29 && Build.VERSION.SDK_INT >= 29 ){
                //大于等于29使用特殊方法
                return getUniqueID(context).toString();
            }
            return DeviceUtils.getIMEI(TelinkLightApplication.getApp().mContext)
        }

        private fun getUniqueID(context: Context): String? {
            var id: String? = null
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!Strings.isEmpty(androidId) && "9774d56d682e549c" != androidId) {
                try {
                    val uuid = UUID.nameUUIDFromBytes(androidId.toByteArray(charset("utf8")))
                    id = uuid.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (Strings.isEmpty(id)) {
                id = getUUID()
            }
            return if (Strings.isEmpty(id)) UUID.randomUUID().toString() else id
        }
        private fun getUUID(): String? {
            var serial: String? = null
            val m_szDevIDShort = "35" + Build.BOARD.length % 10 + Build.BRAND.length % 10 + (if (null != Build.CPU_ABI) Build.CPU_ABI.length else 0) % 10 + Build.DEVICE.length % 10 + Build.DISPLAY.length % 10 + Build.HOST.length % 10 + Build.ID.length % 10 + Build.MANUFACTURER.length % 10 + Build.MODEL.length % 10 + Build.PRODUCT.length % 10 + Build.TAGS.length % 10 + Build.TYPE.length % 10 + Build.USER.length % 10 //13 位
            if (Build.VERSION.SDK_INT <= 29) {
                try {
                    serial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Build.getSerial()
                    } else {
                        Build.SERIAL
                    }
                    //API>=9 使用serial号
                    return UUID(m_szDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
                } catch (exception: java.lang.Exception) {
                    serial = "serial" // 随便一个初始化
                }
            } else {
                serial = Build.UNKNOWN // 随便一个初始化
            }

            //使用硬件信息拼凑出来的15位号码
            return UUID(m_szDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
        }
    }



}