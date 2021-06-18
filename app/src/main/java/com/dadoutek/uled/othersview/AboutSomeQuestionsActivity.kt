package com.dadoutek.uled.othersview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        toolbarTv.text = getString(R.string.common_question)
        toolbar.setNavigationIcon(R.drawable.icon_return)
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
