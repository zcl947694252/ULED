package com.dadoutek.uled.util

import android.content.Context
import android.content.pm.PackageManager
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.*

/**
 * Created by hejiajun on 2018/4/17.
 */

class AppUtils private constructor() {

    init {
        /* cannot be instantiated */
        throw UnsupportedOperationException("cannot be instantiated")
    }

    companion object {

        /**
         * [获取应用程序版本名称信息]
         *
         * @param context
         * @return 当前应用的版本名称
         */
        fun getVersionName(context: Context): String? {
            try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(
                        context.packageName, 0)
                return packageInfo.versionName

            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            return null
        }

        //手机CPU信息
        val isExynosSoc: Boolean
            get() {
                val str1 = "/proc/cpuinfo"
                var str2: String?
                try {
                    val fr = FileReader(str1)
                    val localBufferedReader = BufferedReader(fr, 8192)

                    while (true) {
                        str2 = localBufferedReader.readLine()
                        if (str2 == null) {
                            return false
                        } else if (str2.contains("Exynos")) {
                            localBufferedReader.close()
                            return true
                        }
                    }

                } catch (ignored: IOException) {
                    return false
                }

            }

        /**
         * 是否支持快速恢复出厂设置
         * @param version 版本号
         */
        fun isSupportFastResetFactory(version: String?): Boolean {

            val supportVersions = ArrayList<String>()
            supportVersions.add("LG-06")
            supportVersions.add("LGS-06")
            supportVersions.add("LA-06")
            supportVersions.add("LAS-06")
            supportVersions.add("L20-3.3.8")
            supportVersions.add("L20S-3.3.8")
            supportVersions.add("L36-3.3.8")
            supportVersions.add("L36S-3.3.8")
            supportVersions.add("LC-3.3.8")
            supportVersions.add("LCS-3.3.8")
            supportVersions.add("L-3.3.8")
            supportVersions.add("LN-3.3.8")
            supportVersions.add("LNS-3.3.8")
            supportVersions.add("C-3.3.8")

            version?.let {
                val splitStr = version.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val prefix = splitStr[0]
                val versionCode = splitStr[1]

                for (supportVersion in supportVersions) {
                    if (supportVersion.contains(prefix)) {
                        val supportVersionCode = supportVersion.split("-")[1]
                        if (versionCode >= supportVersionCode) {        //比设定的版本高，就能支持快速恢复出厂设置
                            return true
                        }
                    }
                }
            }

            return false
        }
    }
}
