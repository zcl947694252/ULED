package com.dadoutek.uled.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.dadoutek.uled.R
import kotlinx.android.synthetic.main.toolbar.*

class SceneSwitchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scene_switch)
        setSupportActionBar(toolbar)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
    }
}
