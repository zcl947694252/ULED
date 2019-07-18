package com.dadoutek.uled.user

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.StringUtils
import kotlinx.android.synthetic.main.activity_forget_password.*
import kotlinx.android.synthetic.main.activity_register.btn_send_verification
import kotlinx.android.synthetic.main.activity_register.ccp
import kotlinx.android.synthetic.main.activity_register.edit_user_phone
import kotlinx.android.synthetic.main.activity_register.register_completed
import org.jetbrains.anko.toast

/**
 * Created by hejiajun on 2018/5/18.
 */

class ForgetPassWordActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {

    private var countryCode: String? = null
    private var isChangePwd = false
    private var dbUser: DbUser? = null
    private var isPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)
        initView()
    }

    private fun initView() {
        val changeKey = intent.getStringExtra("fromLogin")
        isChangePwd = changeKey != "register"
        countryCode = ccp.selectedCountryCode
        ccp.setOnCountryChangeListener { countryCode = ccp.selectedCountryCode }
        register_completed.setOnClickListener(this)
        btn_send_verification.setOnClickListener(this)
        forget_password_btn.setOnClickListener(this)
        image_return.setOnClickListener(this)
        StringUtils.initEditTextFilterForRegister(edit_user_phone)
        register_completed.addTextChangedListener(this)
        if (isChangePwd) {
            dbUser = DbUser()
            register_completed.setText(R.string.btn_ok)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.register_completed -> {
                if (NetWorkUtils.isNetworkAvalible(this)) {
                    val phone = edit_user_phone.editableText.toString().trim { it <= ' '}
                            .replace(" ".toRegex(), "")
                    if (TextUtils.isEmpty(phone)) {
                        toast(getString(R.string.please_phone_number))
                        return
                    } else {
                        val intent = Intent(this@ForgetPassWordActivity, EnterConfirmationCodeActivity::class.java)
                        intent.putExtra(Constant.TYPE_USER, Constant.TYPE_FORGET_PASSWORD)
                        intent.putExtra("country_code",countryCode)
                        intent.putExtra("phone", phone)
                        startActivity(intent)
                    }
                } else {
                    ToastUtils.showLong(getString(R.string.net_work_error))
                }
            }
            R.id.btn_send_verification ->
                if (NetWorkUtils.isNetworkAvalible(this)) {
                    send_verification()
                } else {
                    ToastUtils.showLong(getString(R.string.net_work_error))
                }
            R.id.forget_password_btn -> eyePassword()
            R.id.image_return -> finish()
        }
    }

    private fun eyePassword() {
        if (isPassword) {
            forget_password_btn.setImageResource(R.drawable.icon_turn)
            isPassword = false
            edit_forget_password.transformationMethod = PasswordTransformationMethod.getInstance()
            edit_forget_password.setSelection(edit_forget_password.text.length)
        } else {
            isPassword = true
            forget_password_btn.setImageResource(R.drawable.icon_open_eye)
            edit_forget_password.transformationMethod = HideReturnsTransformationMethod.getInstance()
            edit_forget_password.setSelection(edit_forget_password.text.length)
        }
    }

    private fun send_verification() {
        val phoneNum = edit_user_phone.getText().toString().trim({ it <= ' ' })
        if (com.blankj.utilcode.util.StringUtils.isEmpty(phoneNum)) {
            ToastUtils.showShort(R.string.phone_cannot_be_empty)
        } else {
            showLoadingDialog(getString(R.string.get_code_ing))
            //SMSSDK.getVerificationCode(countryCode, phoneNum)
        }
    }

    override fun afterTextChanged(p0: Editable?) {}

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (TextUtils.isEmpty(p0.toString()))
            register_completed.background = getDrawable(R.drawable.btn_rec_black_bt)
        else
            register_completed.background = getDrawable(R.drawable.btn_rec_blue_bt)
    }
}
