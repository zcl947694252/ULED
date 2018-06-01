package com.dadoutek.uled.util

import android.content.Context
import android.support.v7.app.AlertDialog
import com.dadoutek.uled.R

/**
 * Created by hejiajun on 2018/5/10.
 */

object DialogUtils {
    /**
     * @param context: Activity的上下文
     * @param positiveCallback  positive按钮的回调方法
     * @param negativeCallback  negative按钮的回调方法
     */
    fun showNoBlePermissionDialog(context: Context, positiveCallback: () -> Unit, negativeCallback: () -> Unit) {
        AlertDialog.Builder(context)
                .setMessage(R.string.require_permission_reason)
                .setPositiveButton(R.string.i_know, { _, _ -> positiveCallback.invoke() })
                .setNegativeButton(R.string.exit, { _, _ -> negativeCallback.invoke() })
                .show()
    }

}
