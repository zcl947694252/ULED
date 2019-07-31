package com.dadoutek.uled.util;

import android.util.Log;

/**
 * 创建者     ZCL
 * 创建时间   2018/3/27 17:27
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */

public class LogUtil {
    public static  boolean isTry = true;

    public static void util(String tag, String s) {
        if (isTry)
            Log.e(tag,s);
    }
}
