package com.dadoutek.uled.rgb

import android.os.Bundle
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkBaseActivity

class SelectColorActivity :TelinkBaseActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rgb_group_setting)
        initToolBar()
        initData()
        initView()
    }

    private fun initView() {

    }

    private fun initData() {

    }

    private fun initToolBar() {

    }
}