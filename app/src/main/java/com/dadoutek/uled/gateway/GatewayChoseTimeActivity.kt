package com.dadoutek.uled.gateway

import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity


/**
 * 创建者     ZCL
 * 创建时间   2020/3/4 10:30
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GatewayChoseTimeActivity : BaseActivity() {
    private val hourList: MutableList<String> get() = mutableListOf()
    private val minuteList: MutableList<String> get() = mutableListOf()

    override fun initListener() {
    }

    override fun initData() {
    }

    override fun initView() {
    }

    override fun setLayoutID(): Int {
        return R.layout.activity_gate_way_chose_time
    }

}