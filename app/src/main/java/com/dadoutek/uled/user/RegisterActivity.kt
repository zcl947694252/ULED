package com.dadoutek.uled.user

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import butterknife.ButterKnife
import cn.smssdk.EventHandler
import cn.smssdk.SMSSDK
import com.app.hubert.guide.NewbieGuide
import com.app.hubert.guide.model.GuidePage
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkFactory.md5
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.util.*
import com.hbb20.CCPCountry.setDialogTitle
import com.hbb20.CountryCodePicker
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_ota_update.view.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.concurrent.TimeUnit

/**
 * Created by hejiajun on 2018/5/16.
 */

class RegisterActivity : TelinkBaseActivity(), View.OnClickListener {
    private var userName: String? = null
    private var userPassWord: String? = null
    private var MD5PassWord: String? = null
    private var countryCode: String? = null
    private val mCompositeDisposable = CompositeDisposable()
    private val TIME_INTERVAL: Long = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        ButterKnife.bind(this)
        initView()
    }

    private fun initView() {
        initToolbar()
        countryCode = ccp.selectedCountryCode
        ccp.setOnCountryChangeListener { countryCode = ccp.selectedCountryCode }
        register_completed.setOnClickListener(this)
        btn_send_verification.setOnClickListener(this)
        StringUtils.initEditTextFilter(edit_user_phone!!.editText)
        StringUtils.initEditTextFilter(edit_user_password!!.editText)
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.user_register)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }

    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.register_completed -> {
                if (checkIsOK()) {
                    if (Constant.TEST_REGISTER) {
                        showLoadingDialog(getString(R.string.registing))
                        register()
                    } else {
                        showLoadingDialog(getString(R.string.registing))
                        submitCode(countryCode
                                ?: "", userName!!, edit_verification.editText!!.text.toString().trim { it <= ' ' })
                    }
                }
            }
            R.id.btn_send_verification ->
                send_verification()
        }
    }

    private fun send_verification() {
        val phoneNum = edit_user_phone.getEditText()!!.getText().toString().trim({ it <= ' ' })
        if (com.blankj.utilcode.util.StringUtils.isEmpty(phoneNum)) {
            ToastUtils.showShort(R.string.phone_cannot_be_empty)
        } else {
            sendCode(countryCode!!, phoneNum)
        }
    }

    // 请求验证码，其中country表示国家代码，如“86”；phone表示手机号码，如“13800138000”
    fun sendCode(country: String, phone: String) {
        timing()
        // 注册一个事件回调，用于处理发送验证码操作的结果
        SMSSDK.registerEventHandler(object : EventHandler() {
            override fun afterEvent(event: Int, result: Int, data: Any?) {
                if (result == SMSSDK.RESULT_COMPLETE) {
                    // TODO 处理成功得到验证码的结果
                    // 请注意，此时只是完成了发送验证码的请求，验证码短信还需要几秒钟之后才送达
                    ToastUtils.showLong(R.string.send_message_success)
                } else {
                    // TODO 处理错误的结果
                    ToastUtils.showLong(R.string.send_message_fail)
                }

            }
        })
        // 触发操作
        SMSSDK.getVerificationCode(country, phone)
    }

    private fun timing() {
        mCompositeDisposable.add(Observable.intervalRange(0, TIME_INTERVAL, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val num = 59 - it as Long
                    if (num == 0L) {
                        btn_send_verification.text = resources.getString(R.string.reget)
                        btn_send_verification.isEnabled = true
                    } else {
                        btn_send_verification.text = getString(R.string.regetCount, num)
                        btn_send_verification.isEnabled = false
                    }
                })
    }

    private fun register() {
        MD5PassWord = md5(userPassWord)
        NetworkFactory.getApi()
                .register(userName, MD5PassWord, userName)
                .compose(NetworkTransformer())
                .flatMap { it: DbUser ->
                    hideLoadingDialog()
                    showLoadingDialog(getString(R.string.logging_tip))
                    AccountModel.login(userName!!, userPassWord!!, it!!.channel)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : NetworkObserver<DbUser>() {
                    override fun onNext(dbUser: DbUser) {

                        LogUtils.d("logging: " + "登录成功")
                        DBUtils.deleteLocalData()
//                        ToastUtils.showLong(R.string.login_success)
                        hideLoadingDialog()
                        //判断是否用户是首次在这个手机登录此账号，是则同步数据
                        showLoadingDialog(getString(R.string.sync_now))
                        SyncDataPutOrGetUtils.syncGetDataStart(dbUser, syncCallback)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        hideLoadingDialog()
                    }
                })
    }

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            syncComplet()
        }

        override fun error(msg: String) {
            LogUtils.d("GetDataError:" + msg)
        }

    }

    private fun syncComplet() {
//        ToastUtils.showLong(getString(R.string.upload_complete))
        hideLoadingDialog()
        TransformView()
    }

    private fun TransformView() {
        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
        finish()
    }

    private fun checkIsOK(): Boolean {
        userName = edit_user_phone!!.editText!!.text.toString().trim { it <= ' ' }
        userPassWord = edit_user_password!!.editText!!.text.toString().trim { it <= ' ' }

        if (compileExChar(userName)) {
            ToastUtils.showLong(R.string.phone_input_error)
            return false
        } else if (compileExChar(userName) || compileExChar(userPassWord)) {
            ToastUtils.showLong(R.string.tip_register_input_error)
            return false
        } else {
            return true
        }
    }

    // 提交验证码，其中的code表示验证码，如“1357”
    fun submitCode(country: String, phone: String, code: String): Boolean {
        // 注册一个事件回调，用于处理提交验证码操作的结果
        SMSSDK.registerEventHandler(object : EventHandler() {
            override fun afterEvent(event: Int, result: Int, data: Any?) {
                if (result == SMSSDK.RESULT_COMPLETE) {
                    // TODO 处理验证成功的结果
                    register()
                } else {
                    // TODO 处理错误的结果
                    ToastUtils.showLong(R.string.verification_code_error)
                    hideLoadingDialog()
                }

            }
        })
        // 触发操作
        SMSSDK.submitVerificationCode(country, phone, code)
        return false
    }
}
