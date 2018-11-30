package com.dadoutek.uled.util

import android.app.Dialog
import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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
                .setPositiveButton(android.R.string.ok, { _, _ -> positiveCallback.invoke() })
                .setNegativeButton(R.string.exit, { _, _ -> negativeCallback.invoke() })
                .show()
    }

    fun showLoadingDialog(content: String, context: Context, loadDialog: Dialog) {
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing()) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            loadDialog!!.show()
        }
    }

    fun hideLoadingDialog(loadDialog: Dialog) {
        if (loadDialog != null) {
            loadDialog!!.dismiss()
        }
    }

}
