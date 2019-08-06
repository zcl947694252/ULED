package com.dadoutek.uled.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayoutID())
        initView()
        initData()
        initListener()
    }

    abstract fun setLayoutID(): Int
    abstract fun initView()
    abstract fun initData()
    abstract fun initListener()
}
