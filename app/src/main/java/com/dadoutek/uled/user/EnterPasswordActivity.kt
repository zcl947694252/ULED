package com.dadoutek.uled.user

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import kotlinx.android.synthetic.main.activity_enter_password.*

class EnterPasswordActivity : AppCompatActivity(), View.OnClickListener {

    private var phone: String? = null

    private var type: String? = null

    private var isPassword = false

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
        forget_password.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.eye_btn -> {
                if (isPassword) {
                    eye_btn.setImageResource(R.drawable.icon_turn)
                    isPassword = false
                    edit_user_password.transformationMethod = PasswordTransformationMethod.getInstance()
                    edit_user_password.setSelection(edit_user_password.text.length)
                } else {
                    isPassword = true
                    eye_btn.setImageResource(R.drawable.icon_open_eye)
                    edit_user_password.transformationMethod = HideReturnsTransformationMethod.getInstance()
                    edit_user_password.setSelection(edit_user_password.text.length)
                }
            }

            R.id.btn_login -> {

            }

            R.id.image_return_password -> finish()

            R.id.forget_password -> {
                var intent = Intent(this,ForgetPassWordActivity::class.java)
                intent.putExtra("fromLogin", "forgetPassword")
                startActivity(intent)
            }
        }
    }

}
