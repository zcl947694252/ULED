package com.dadoutek.uled.othersview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils
import kotlinx.android.synthetic.main.activity_batch_group_four.*
import kotlinx.android.synthetic.main.activtiy_myzxing.*

class MyScanActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activtiy_myzxing)
        com.dadoutek.uled.util.StringUtils.initEditTextFilter(scan_device_code)
        image_bluetooth.visibility = View.GONE
        toolbarTv.text = getString(R.string.scan_code)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        LogUtils.v("==============================进入MyScanActivity界面=======================================")
        scan_confim.setOnClickListener {
            val toString = scan_device_code.editableText.toString()

            when {
                toString.isEmpty() || toString.length != 12 -> ToastUtils.showShort(getString(R.string.input_right_code))
                else -> {
                    val intent = Intent()
                    intent.putExtra(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_SUCCESS)
                    LogUtils.v("==================================传递的toString：$toString======================================================")
                    intent.putExtra(CodeUtils.RESULT_STRING, toString)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                    LogUtils.v("zcl------设备码回传------------$toString")
                }
            }
        }
    }
}