package com.dadoutek.uled.user

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.app.hubert.guide.util.LogUtil
import com.blankj.utilcode.util.StringUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.LogUtils
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.dadoutek.uled.util.ToastUtil
import kotlinx.android.synthetic.main.activity_enter_password.*
import org.jetbrains.anko.toast

class EnterPasswordActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {

    private var phone: String? = null

    private var type: String? = null

    private var isPassword = false

    private var editPassWord: String? = null

    private var dbUser: DbUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_password)
        type = intent.extras!!.getString("USER_TYPE")
        initViewType()
        initView()
    }

    private fun initViewType() {
        when (type) {
            Constant.TYPE_FORGET_PASSWORD -> {

            }
            Constant.TYPE_LOGIN -> {
                dbUser = DbUser()
                phone = intent.extras!!.getString("phone")
            }

            Constant.TYPE_REGISTER -> {
                dbUser = DbUser()
                phone = intent.extras!!.getString("phone")
            }
        }
    }

    private fun initView() {
        edit_user_password.addTextChangedListener(this)
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
                LogUtil.e("login phonetype=" + type)
                when (type) {
                    Constant.TYPE_LOGIN -> {
                        if (TextUtils.isEmpty(edit_user_password.editableText.toString())) {
                          toast(getString(R.string.please_password))
                            return
                        }
                        login()
                    }

                    Constant.TYPE_FORGET_PASSWORD -> forgetPassword()

                    Constant.TYPE_REGISTER -> register()
                }
            }

            R.id.image_return_password -> finish()

            R.id.forget_password -> {
                var intent = Intent(this, ForgetPassWordActivity::class.java)
                intent.putExtra("fromLogin", "forgetPassword")
                startActivity(intent)
            }
        }
    }

    private fun register() {

    }

    private fun forgetPassword() {
        editPassWord = edit_user_password!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        if (!StringUtils.isTrimEmpty(editPassWord)) {
            val intent = Intent(this, AgainEnterPasswordActivity::class.java)
            intent.putExtra("phone", phone)
            intent.putExtra("password", editPassWord)
            startActivity(intent)
        } else {
            ToastUtil.showToast(this, getString(R.string.password_cannot))
        }
    }


    private fun login() {
        LogUtil.e("login phone=" + phone)
        editPassWord = edit_user_password!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        if (!StringUtils.isTrimEmpty(editPassWord)) {
            showLoadingDialog(getString(R.string.logging_tip))
            LogUtil.e("login phone=" + phone + "===" + editPassWord + "---" + dbUser!!.channel)
            AccountModel.login(phone!!, editPassWord!!, dbUser!!.channel)
                    .subscribe(object : NetworkObserver<DbUser>() {
                        override fun onNext(dbUser: DbUser) {
                            DBUtils.deleteLocalData()
//                            ToastUtils.showLong(R.string.login_success)
                            SharedPreferencesUtils.saveLastUser("$phone-$editPassWord")
//                            hideLoadingDialog()
                            //判断是否用户是首次在这个手机登录此账号，是则同步数据
                            showLoadingDialog(getString(R.string.sync_now))
                            SyncDataPutOrGetUtils.syncGetDataStart(dbUser, syncCallback)
                            SharedPreferencesUtils.setUserLogin(true)
                            LogUtils.d("logging: " + "登录成功" + dbUser.name)
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            LogUtils.d("logging: " + "登录错误" + e.message)
                            hideLoadingDialog()
                        }
                    })
        } else {
            ToastUtil.showToast(this, getString(R.string.password_cannot))
        }
    }

    var isSuccess: Boolean = true
    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {
//            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            if (isSuccess) {
                syncComplet()
            }
        }

        override fun error(msg: String) {
            isSuccess = false
            hideLoadingDialog()
            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, false)
            LogUtils.d("GetDataError:" + msg)
        }

    }

    private fun syncComplet() {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, true)
//        ToastUtils.showLong(getString(R.string.download_data_success))
        transformView()
        hideLoadingDialog()
    }

    private fun transformView() {
        if (DBUtils.allLight.isEmpty()) {
            startActivity(Intent(this@EnterPasswordActivity, MainActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this@EnterPasswordActivity, MainActivity::class.java))
        }
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        if (TextUtils.isEmpty(p0.toString()))
            btn_login.background = getDrawable(R.drawable.btn_rec_black_bt)
        else
            btn_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
    }
}
