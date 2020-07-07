package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import cn.smssdk.SMSSDK
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.CountryActivity
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.StringUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_forget_password.*
import kotlinx.android.synthetic.main.activity_forget_password.ccp_tv
import kotlinx.android.synthetic.main.activity_forget_password.edit_user_phone
import kotlinx.android.synthetic.main.activity_forget_password.phone_area_num_ly
import kotlinx.android.synthetic.main.activity_verification_code.*
import org.jetbrains.anko.toast
import java.util.*

/**
 * Created by hejiajun on 2018/5/18.
 * 忘记密码
 */

class ForgetPassWordActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {

    private var countryCode: String = "86"
    private var isChangePwd = false
    private var dbUser: DbUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)
        initView()
    }

    private fun initView() {
        val changeKey = intent.getStringExtra("fromLogin")
        isChangePwd = changeKey != "register"
        register_completed.setOnClickListener(this)
        image_return.setOnClickListener(this)
        StringUtils.initEditTextFilterForRegister(edit_user_phone)

        edit_user_phone.addTextChangedListener(this)
        if (isChangePwd) {
            dbUser = DbUser()
            register_completed.setText(R.string.confirm)
        }

        phone_area_num_ly.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.register_completed -> {
                getAccount()
            }
            R.id.image_return -> finish()
            R.id.phone_area_num_ly -> {
                val intent = Intent()
                intent.setClass(this@ForgetPassWordActivity, CountryActivity::class.java)
                startActivityForResult(intent, 10)
            }
        }
    }

    private fun getAccount() {
        if (NetWorkUtils.isNetworkAvalible(this)) {
            val userName = edit_user_phone.editableText.toString().trim { it <= ' '}
                    .replace(" ".toRegex(), "")
            send_verification()

            if (TextUtils.isEmpty(userName)) {
                toast(getString(R.string.please_phone_number))
                return
            } else {
                val map = HashMap<String, String>()
                map["phone"] = userName!!
                map["channel"] = dbUser!!.channel
                NetworkFactory.getApi()
                        .getAccount(userName, dbUser!!.channel)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : NetworkObserver<Response<String>?>() {
                            override fun onNext(t: Response<String>) {
                                hideLoadingDialog()
                                if (t.errorCode == 0) {
                                    LogUtils.e("logging" + t.errorCode + "获取成功account")
                                    dbUser!!.account = t.t
                                    //正式代码走短信验证
                                    val intent = Intent(this@ForgetPassWordActivity, EnterConfirmationCodeActivity::class.java)
                                    intent.putExtra(Constant.TYPE_USER, Constant.TYPE_FORGET_PASSWORD)
                                    intent.putExtra("country_code",countryCode)
                                    intent.putExtra("phone", userName)
                                    intent.putExtra("account", dbUser!!.account)
                                    startActivity(intent)
                                    //调试程序直接修改
                                    // val intent = Intent(this@ForgetPassWordActivity, InputPwdActivity::class.java)
                                    // intent.putExtra(Constant.USER_TYPE, Constant.TYPE_FORGET_PASSWORD)
                                    // intent.putExtra("phone",  dbUser!!.account)
                                    // startActivity(intent)
                                    // finish()
                                } else {
                                    ToastUtils.showLong(t.message)
                                }
                            }

                            override fun onError(it: Throwable) {
                                super.onError(it)
                                hideLoadingDialog()
                                Toast.makeText(this@ForgetPassWordActivity, "onError:$it", Toast.LENGTH_SHORT).show()
                            }
                        })
            }

        } else {
            ToastUtils.showLong(getString(R.string.net_work_error))
        }
    }


    private fun send_verification() {
        val phoneNum = edit_user_phone.text.toString().trim { it <= ' ' }
        if (com.blankj.utilcode.util.StringUtils.isEmpty(phoneNum)) {
            ToastUtils.showLong(R.string.phone_cannot_be_empty)
        } else {
            showLoadingDialog(getString(R.string.get_code_ing))
            SMSSDK.getVerificationCode(countryCode, phoneNum)
        }
    }

    override fun afterTextChanged(p0: Editable?) {

    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (TextUtils.isEmpty(p0.toString())) {
            register_completed.background = getDrawable(R.drawable.btn_rec_black_bt)
            register_phone_line.background = getDrawable(R.drawable.line_gray)
        } else {
            register_phone_line.background = getDrawable(R.drawable.line_blue)
            register_completed.background = getDrawable(R.drawable.btn_rec_blue_bt)
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data?.extras
                val countryName = bundle?.getString("countryName")
                val countryNumber = bundle?.getString("countryNumber")

                val toString = countryNumber?.replace("+", "").toString()
                LogUtils.v("zcl------------------countryCode接手前$countryCode")
                if (TextUtils.isEmpty(countryCode))
                    return
                LogUtils.v("zcl------------------countryCode接收后$countryCode")
                countryCode = toString
                ccp_tv.text = countryName + countryNumber
            }
            else -> { }
        }
    }
}