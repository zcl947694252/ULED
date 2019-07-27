package com.dadoutek.uled.util;

import android.app.AlertDialog;
import android.content.Context;

import com.telink.bluetooth.LeBluetooth;

/**
 * 创建者     ZCL
 * 创建时间   2019/7/27 14:49
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class ComonentUtils {
    public static AlertDialog.Builder newAlertDialog(Context applicationContext, int open_blutooth, int btn_ok) {
        return new AlertDialog.Builder(applicationContext)
                .setCancelable(false)
                .setMessage(applicationContext.getString(open_blutooth))
                .setPositiveButton(applicationContext.getString(btn_ok), (dialog, which) -> LeBluetooth.getInstance().enable(applicationContext));
    }
}
