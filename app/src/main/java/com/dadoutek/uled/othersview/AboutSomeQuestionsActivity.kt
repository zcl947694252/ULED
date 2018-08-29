package com.dadoutek.uled.othersview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.util.SharedPreferencesUtils
import kotlinx.android.synthetic.main.activity_app_about_info.*
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

        if(SharedPreferencesUtils.isDeveloperModel()){
            address_test.visibility= View.VISIBLE
        }else{
            address_test.visibility= View.GONE
        }
    }
}
