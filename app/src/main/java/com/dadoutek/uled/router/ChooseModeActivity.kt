package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.model.httpModel.UserModel
import com.dadoutek.uled.tellink.TelinkLightService
import kotlinx.android.synthetic.main.activity_choose_mode.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/8/13 14:45
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ChooseModeActivity : TelinkBaseActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_mode)

        toolbarTv.text = getString(R.string.select_mode)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setNavigationIcon(R.drawable.icon_return)

        updateUi()

        toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.visibility =View.GONE

        choose_mode_ble_iv.setOnClickListener(this)
        choose_mode_router_iv.setOnClickListener(this)
    }

    private fun updateUi() {
        if (Constant.IS_ROUTE_MODE) {
            choose_mode_ble_iv.setImageResource(R.drawable.choice_off)
            choose_mode_router_iv.setImageResource(R.drawable.choice_on)
        } else {
            choose_mode_ble_iv.setImageResource(R.drawable.choice_on)
            choose_mode_router_iv.setImageResource(R.drawable.choice_off)
        }
    }

    @SuppressLint("CheckResult")
    override fun onClick(v: View?) {
                    Constant.IS_ROUTE_MODE = !Constant.IS_ROUTE_MODE
        when (v?.id) {
            R.id.choose_mode_ble_iv, R.id.choose_mode_router_iv -> {
                updateModeStatus()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun updateModeStatus() {
        UserModel.updateModeStatus().subscribe({
            SharedPreferencesHelper.putBoolean(this, Constant.ROUTE_MODE, Constant.IS_ROUTE_MODE)
            TelinkLightService.Instance()?.idleMode(true)
            updateUi()
            LogUtils.v("zcl--------------上传云状态为路由----")
        }, {
            Constant.IS_ROUTE_MODE = !Constant.IS_ROUTE_MODE
        })
    }
}