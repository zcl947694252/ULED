package com.dadoutek.uled.fragment

import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import kotlinx.android.synthetic.main.activity_save_lock.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/5/12 18:06
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SafeLockActivity : TelinkBaseActivity(), View.OnClickListener {
    private var allGroup: DbGroup? = null
    private var isFristUserClickCheckConnect: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_lock)
        initView()
        initData()
        initListener()
    }

    private fun initData() {
        toolbarTv.text = getString(R.string.safe_lock)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initView() {
        allGroup = DBUtils.getGroupByMeshAddr(0xFFFF)
    }

    private fun initListener() {
        safe_open.setOnClickListener(this)
        safe_lock.setOnClickListener(this)
        safe_close.setOnClickListener(this)
        safe_unlock.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        isFristUserClickCheckConnect = true
        val dstAddr = this.allGroup!!.meshAddr
        if (TelinkLightApplication.getApp().connectDevice == null)
            checkConnect()
        else {
            when (v?.id) {
                R.id.safe_open -> {
                    Commander.openOrCloseLights(dstAddr, true)
                    safe_open.setBackgroundResource(R.drawable.rect_blue_60)
                    safe_open_arrow.setImageResource(R.mipmap.icon_safe_arrow_blue)
                    safe_close.setBackgroundResource(R.drawable.rect_gray_60)
                    safe_close_arrow.setImageResource(R.mipmap.icon_arrow_safe)
                }

                R.id.safe_lock -> {

                }

                R.id.safe_close -> {
                    safe_close.setBackgroundResource(R.drawable.rect_blue_60)
                    safe_close_arrow.setImageResource(R.mipmap.icon_safe_arrow_blue)
                    safe_open.setBackgroundResource(R.drawable.rect_gray_60)
                    safe_open_arrow.setImageResource(R.mipmap.icon_arrow_safe)
                    Commander.openOrCloseLights(dstAddr, false)
                }

                R.id.safe_unlock -> {

                }
            }
        }
    }

    private fun checkConnect() {
        try {
            if (TelinkLightApplication.getApp().connectDevice == null) {
                ToastUtils.showShort(getString(R.string.connecting))
                if (isFristUserClickCheckConnect) {
                    val activity = this as MainActivity
                    activity.autoConnect()
                    isFristUserClickCheckConnect = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}