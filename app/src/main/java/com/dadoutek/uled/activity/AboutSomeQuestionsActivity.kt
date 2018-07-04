package com.dadoutek.uled.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.dadoutek.uled.R
import kotlinx.android.synthetic.main.toolbar.*

class AboutSomeQuestionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_about_info)
        initView()
    }

    private fun initView() {
        toolbar.title = getString(R.string.common_question)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }

    }
}
