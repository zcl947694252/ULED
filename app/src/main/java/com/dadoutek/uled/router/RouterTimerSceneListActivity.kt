package com.dadoutek.uled.router

import android.os.Bundle
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import kotlinx.android.synthetic.main.activity_batch_group_four.*
import kotlinx.android.synthetic.main.activity_mainsss.view.*


/**
 * 创建者     ZCL
 * 创建时间   2020/10/27 18:12
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterTimerSceneListActivity : TelinkBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_router_timer_scene_list)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {

    }

    private fun initData() {
    }

    private fun initView() {
        toolbarTv.text = getString(R.string.timer_scene)
        toolbar.setNavigationOnClickListener { finish() }
    }
}