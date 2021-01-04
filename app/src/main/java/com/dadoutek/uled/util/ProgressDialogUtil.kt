package com.dadoutek.uled.util

import android.content.Context
import android.support.v7.app.AlertDialog
import android.widget.ProgressBar
import android.widget.TextView
import com.dadoutek.uled.R
import org.jetbrains.anko.layoutInflater


object ProgressDialogUtil {
    private var dialog: AlertDialog? = null

    fun create(context: Context, content: String, indeterminate: Boolean = true) {
        val dialogBuilder = AlertDialog.Builder(context)
        val dialogView = context.layoutInflater.inflate(R.layout.progress_dialog_layout, null)
        val textView = dialogView.findViewById<TextView>(R.id.content)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.isIndeterminate = indeterminate
        textView.text = content
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)
        dialog = dialogBuilder.create()
        dialog?.show()
    }

    fun setProgress(progress: Int) {
        val progressBar = dialog?.findViewById<ProgressBar>(R.id.progressBar)
        progressBar?.progress = progress
        progressBar?.invalidate()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}