package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.httpModel.AccountModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_input_pwd.*
import org.jetbrains.anko.toast

/**
 * 登录不共享此界面 登录在EnterPasswordActivity
 */
class InputPwdActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {
    var typeStr: String? = null
    var isPassword = false
    var password: String? = null
    var phone: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_pwd)
        phone = intent.getStringExtra("phone")
        typeStr = intent.getStringExtra(Constant.USER_TYPE)
        initView()
        initListener()
    }

    private fun initView() {
        if (typeStr == Constant.TYPE_REGISTER) {
            pwd_notice.text = getString(R.string.please_password)
            pwd_title.text = getString(R.string.enter_password)
            pwd_btn.text = getString(R.string.register)
        } else if (typeStr == Constant.TYPE_FORGET_PASSWORD) {
            pwd_notice.text = getString(R.string.follow_the_steps)
            pwd_title.text = getString(R.string.set_password)
            pwd_btn.text = getString(R.string.complete)
        }
    }

    private fun initListener() {
        pwd_eye.setOnClickListener(this)
        pwd_btn.setOnClickListener(this)
        pwd_return.setOnClickListener(this)
        pwd_input.addTextChangedListener(this)
    }

    override fun onClick(p0: View?) {
        when (p0!!.id) {
            R.id.pwd_eye -> eyePassword()

            R.id.pwd_return -> {
                when {
                    pwd_btn.text.toString() == getString(R.string.complete) || pwd_btn.text.toString() == getString(R.string.register) -> {
                        finish()
                    }
                    pwd_btn.text.toString() == getString(R.string.complete) -> {
                        pwd_input.hint = getString(R.string.please_password)
                        pwd_input.setText(password)
                        pwd_notice.text = getString(R.string.please_password)
                        pwd_btn.text = getString(R.string.next)
                    }
                }
            }

            R.id.pwd_btn -> {
                var pwd = pwd_input.editableText.toString()
                password = pwd
                when {
                    pwd_btn.text.toString() == getString(R.string.complete) -> {
                        if (!TextUtils.isEmpty(password)) {
                            upDatePwd()
                        } else {
                            toast(getString(R.string.new_password))
                        }
                    }
                    pwd_btn.text.toString() == getString(R.string.register) -> {
                        if (TextUtils.isEmpty(pwd)) {
                            toast(getString(R.string.please_password))
                            return
                        }
                        password = pwd
                        register()
                    }
                }
            }
        }
    }

    private fun upDatePwd() {
        //toast("账户$phone----密码$password")
        val subscribe = NetworkFactory.getApi()
                .putPassword(phone, NetworkFactory.md5(password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                    hideLoadingDialog()
                    if (it.errorCode == 0) {
                        //("logging" + stringResponse.errorCode + "更改成功")
                        ToastUtils.showLong(R.string.tip_update_password_success)
                        startActivity(Intent(this@InputPwdActivity, MainActivity::class.java))
                        setResult(RESULT_FIRST_USER)
                        finish()
                    } else {
                        //ToastUtils.showLong(R.string.tip_update_password_fail)
                        ToastUtils.showLong(it.message)
                    }
                }, {
                    hideLoadingDialog()
                    Toast.makeText(this@InputPwdActivity, "onError:${it.message}", Toast.LENGTH_SHORT).show()
                })
    }

    @SuppressLint("CheckResult")
    private fun register() {
        NetworkFactory.getApi()
                .register(phone, NetworkFactory.md5(password), phone)
                .compose(NetworkTransformer())
                .flatMap {
                    hideLoadingDialog()
                    showLoadingDialog(getString(R.string.logging_tip))
                    AccountModel.login(phone!!.toString(), password!!)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                       //("logging: " + "登录成功")
                        DBUtils.deleteLocalData()
                        hideLoadingDialog()
                        //判断是否用户是首次在这个手机登录此账号，是则同步数据
                        showLoadingDialog(getString(R.string.sync_now))
                        SyncDataPutOrGetUtils.syncGetDataStart(it, syncCallback)
                        SharedPreferencesUtils.setUserLogin(true)
                    },{
                        hideLoadingDialog()
                })
    }

    private fun eyePassword() {
        if (isPassword) {
            pwd_eye.setImageResource(R.drawable.icon_turn)
            isPassword = false
            pwd_input.transformationMethod = PasswordTransformationMethod.getInstance()
            pwd_input.setSelection(pwd_input.text.length)
        } else {
            isPassword = true
            pwd_eye.setImageResource(R.drawable.icon_open_eye)
            pwd_input.transformationMethod = HideReturnsTransformationMethod.getInstance()
            pwd_input.setSelection(pwd_input.text.length)
        }
    }

    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            setResult(RESULT_FIRST_USER)
            startActivity(Intent(this@InputPwdActivity, MainActivity::class.java))
            finish()
        }

        override fun error(msg: String) {
            hideLoadingDialog()
            ToastUtils.showLong(msg)
        }
    }

    override fun afterTextChanged(p0: Editable?) {}

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (TextUtils.isEmpty(p0.toString()))
            pwd_btn.background = getDrawable(R.drawable.btn_rec_black_bt)
        else
            pwd_btn.background = getDrawable(R.drawable.btn_rec_blue_bt)
    }

}
