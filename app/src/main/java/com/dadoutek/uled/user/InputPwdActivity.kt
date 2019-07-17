package com.dadoutek.uled.user

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.blankj.utilcode.util.StringUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.util.LogUtils
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_input_pwd.*
import kotlinx.android.synthetic.main.fragment_me.*
import org.jetbrains.anko.toast

class InputPwdActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {
    private lateinit var dbUser: DbUser
    var editPassWord: String? = null
    var type: String? = null
    var isPassword = false
    var password: String? = null
    var phone: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_pwd)
        phone = intent.getStringExtra("phone")
        type = intent.getStringExtra(Constant.USER_TYPE)
        initView()
        initListener()
    }

    private fun initView() {
        dbUser = DbUser()
        LogUtils.d(dbUser.toString()+"----"+dbUser.channel)
        if (type == Constant.TYPE_REGISTER) {
            pwd_notice.text = getString(R.string.please_password)
            pwd_title.text = getString(R.string.enter_password)
            pwd_btn.text = getString(R.string.next)
        } else if (type == Constant.TYPE_FORGET_PASSWORD) {
            pwd_notice.text = getString(R.string.follow_the_steps)
            pwd_title.text = getString(R.string.forget_password)
            pwd_btn.text = getString(R.string.login_bt_name)
        }
    }

    private fun initListener() {
        pwd_eye.setOnClickListener(this)
        pwd_btn.setOnClickListener(this)
        pwd_return.setOnClickListener(this)
        pwd_btn.addTextChangedListener(this)
    }

    override fun onClick(p0: View?) {
        when (p0!!.id) {
            R.id.pwd_eye -> eyePassword()

            R.id.pwd_return -> {
                if (pwd_btn.text.equals(getString(R.string.next)) ||
                        pwd_btn.text.equals(getString(R.string.login_bt_name))) {
                    finish()
                } else if (pwd_btn.text.equals(getString(R.string.complete))) {
                    pwd_input.hint = getString(R.string.please_password)
                    pwd_input.setText(password)
                    pwd_notice.text = getString(R.string.please_password)
                    pwd_btn.text = getString(R.string.next)
                }
            }

            R.id.pwd_btn -> {
                val pwd = pwd_input.editableText.toString()
                if (pwd_btn.text.equals(getString(R.string.next))) {
                    if (TextUtils.isEmpty(pwd)) {
                        toast(getString(R.string.please_password))
                        return
                    }
                    pwd_btn.text = getString(R.string.complete)
                    password = pwd
                    pwd_input.text.clear()
                    pwd_input.hint = getString(R.string.please_again_password)
                    pwd_notice.text = getString(R.string.please_again_password)

                } else if (pwd_btn.text.equals(getString(R.string.complete))) {
                    if (!pwd.equals(password) && !TextUtils.isEmpty(password)) {
                        toast(getString(R.string.different_input))
                        return
                    }
                    register()
                } else if (pwd_btn.text.equals(getString(R.string.login_bt_name))) {
                    if (TextUtils.isEmpty(password)) {
                        toast(getString(R.string.please_password))
                        return
                    }
                    login()
                }
            }
        }
    }

    private fun login() {
        editPassWord = pwd_input!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        if (!StringUtils.isTrimEmpty(phone) && !StringUtils.isTrimEmpty(editPassWord)) {
            showLoadingDialog(getString(R.string.logging_tip))
            AccountModel.login(phone!!, editPassWord!!, dbUser!!.channel)
                    .subscribe(object : NetworkObserver<DbUser>() {
                        override fun onNext(dbUser: DbUser) {
                            DBUtils.deleteLocalData()
                            SharedPreferencesUtils.saveLastUser("$phone-$editPassWord")
                            //判断是否用户是首次在这个手机登录此账号，是则同步数据
                            SyncDataPutOrGetUtils.syncGetDataStart(dbUser, syncCallback)
                            SharedPreferencesUtils.setUserLogin(true)
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            LogUtils.d("logging: " + "登录错误" + e.message)
                            hideLoadingDialog()
                        }
                    })
        } else {
            toast(getString(R.string.phone_or_password_can_not_be_empty))
        }

    }

    private fun register() {
        NetworkFactory.getApi()
                .register(phone, NetworkFactory.md5(password), phone)
                .compose(NetworkTransformer())
                .flatMap {
                    hideLoadingDialog()
                    showLoadingDialog(getString(R.string.logging_tip))
                    AccountModel.login(userName.toString()!!, password!!, it!!.channel)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : NetworkObserver<DbUser>() {
                    override fun onNext(dbUser: DbUser) {
                        LogUtils.d("logging: " + "登录成功")
                        DBUtils.deleteLocalData()
                        hideLoadingDialog()
                        //判断是否用户是首次在这个手机登录此账号，是则同步数据
                        showLoadingDialog(getString(R.string.sync_now))
                        SyncDataPutOrGetUtils.syncGetDataStart(dbUser, syncCallback)
                        SharedPreferencesUtils.setUserLogin(true)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        hideLoadingDialog()
                    }
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
            startActivity(Intent(this@InputPwdActivity, MainActivity::class.java))
            finish()
        }

        override fun error(msg: String) {
            LogUtils.d("GetDataError:$msg")
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
