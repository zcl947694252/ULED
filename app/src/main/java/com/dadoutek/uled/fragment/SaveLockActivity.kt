package com.dadoutek.uled.fragment

import android.os.Bundle
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import kotlinx.android.synthetic.main.activity_save_lock.*


/**
 * 创建者     ZCL
 * 创建时间   2020/5/12 18:06
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SaveLockActivity : TelinkBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_lock)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        safe_open
    }

    private fun initData() {
    }

    private fun initView() {

    }
}