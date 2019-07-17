package com.dadoutek.uled.user

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkBaseActivity
import kotlinx.android.synthetic.main.activity_input_pwd.*
import kotlinx.android.synthetic.main.activity_login.*

class InputPwdActivity : TelinkBaseActivity(), View.OnClickListener {
    var isPassword = false
    var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_pwd)
        initListener()
    }

    private fun initListener() {
        pwd_eye.setOnClickListener(this)
        pwd_btn.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        when (p0!!.id) {
            R.id.pwd_eye -> eyePassword()

            R.id.pwd_btn -> {
                if (pwd_btn.text.equals(getString(R.string.next))) {

                } else {

                }
            }
        }


    }

    private fun eyePassword() {
        if (isPassword) {
            pwd_eye.setImageResource(R.drawable.icon_turn)
            isPassword = false
            pwd_input.transformationMethod = PasswordTransformationMethod.getInstance()
            pwd_input.setSelection(edit_user_password.text.length)
        } else {
            isPassword = true
            pwd_eye.setImageResource(R.drawable.icon_open_eye)
            pwd_input.transformationMethod = HideReturnsTransformationMethod.getInstance()
            pwd_input.setSelection(edit_user_password.text.length)
        }
    }

}
