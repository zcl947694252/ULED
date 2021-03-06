package com.dadoutek.uled.user

import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import kotlinx.android.synthetic.main.activity_again_enter_password.*

class AgainEnterPasswordActivity : TelinkBaseActivity(), View.OnClickListener {

    private var phone: String? = null

    private var password: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_again_enter_password)

        LogUtils.d("AgainEnterPasswordActivity")
        initView()
    }

    private fun initView() {
        image_return_again_password.setOnClickListener(this)
        loginBtn.setOnClickListener(this)
        eye_btn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.image_return_again_password -> finish()

            R.id.loginBtn -> login()

            R.id.eye_btn -> showHidden()
        }
    }

    private fun login() {

    }

    private fun showHidden(){

    }
}
