package com.dadoutek.uled.user

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import kotlinx.android.synthetic.main.activity_enter_password.*

class EnterPasswordActivity : AppCompatActivity(), View.OnClickListener {

    private var phone: String? = null

    private var type: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_password)
        type = intent.extras!!.getString(Constant.TYPE_USER)
        initViewType()
        initView()
    }

    private fun initViewType() {
        when (type) {
            Constant.TYPE_FORGET_PASSWORD -> {

            }
            Constant.TYPE_LOGIN -> {

            }
            Constant.TYPE_REGISTER -> {

            }
        }
    }

    private fun initView() {
        eye_btn.setOnClickListener(this)
        btn_login.setOnClickListener(this)
        image_return_password.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.eye_btn -> {

            }

            R.id.btn_login -> {

            }

            R.id.image_return_password -> finish()
        }
    }

}
