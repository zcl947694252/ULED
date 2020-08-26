package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import cn.smssdk.EventHandler
import cn.smssdk.SMSSDK
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbUser
import com.dadoutek.uled.model.httpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_enter_confirmation_code.*
import kotlinx.android.synthetic.main.activity_verification_code.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 忘记密码设置新密码/短信登录
 */
class EnterConfirmationCodeActivity : TelinkBaseActivity(), View.OnClickListener {
    private var num: Long = 0
    private var account: String? = null
    private val TIME_INTERVAL: Long = 60

    private val mCompositeDisposable = CompositeDisposable()
    private var countryCode: String = "86"
    private var phone: String? = null
    private var typeStr: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_confirmation_code)
        typeStr = this.intent.extras!!.getString(Constant.TYPE_USER)
        initViewType()
        initView()
        timing()

        SMSSDK.registerEventHandler(eventHandler)
    }


    @SuppressLint("SetTextI18n")
    private fun initViewType() {
        countryCode = this.intent.extras!!.getString("country_code") ?: ""
        phone = this.intent.extras!!.getString("phone")
        account = this.intent.extras!!.getString("account")
        when (typeStr) {
            Constant.TYPE_VERIFICATION_CODE -> {
                codePhone.text = resources.getString(R.string.send_code) + "+" + countryCode + " " + phone
            }
            Constant.TYPE_REGISTER -> {
                codePhone.text = resources.getString(R.string.send_code) + "+" + countryCode + phone
            }
            Constant.TYPE_FORGET_PASSWORD -> {
                codePhone.text = resources.getString(R.string.follow_the_steps)
            }
        }
    }

    private fun initView() {
        verCodeInputView.setOnCompleteListener { verificationLogin() }
        verCodeInputView.setOnCompleteListener { it ->
            var code = it
            submitCode(countryCode, phone!!, code.toString().trim { it <= ' ' })
        }
        verCodeInputView_line.setOnTextChangeListener { s, _ ->
            if (s.length >= 6)
                submitCode(countryCode, phone!!, s)
                // verificationLogin()
        }
        image_return.setOnClickListener(this)
        reacquireCode.setOnClickListener {
            if (num == 0L)
                verificationCode()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.refresh_code -> verificationCode()

            R.id.image_return -> {
                SMSSDK.unregisterEventHandler(eventHandler)
                finish()
            }
        }
    }

    private val eventHandler = object : EventHandler() {
        override fun afterEvent(event: Int, result: Int, data: Any?) {
            // afterEvent会在子线程被调用，因此如果后续有UI相关操作，需要将数据发送到UI线程
            val msg = Message()
            msg.arg1 = event
            msg.arg2 = result
            msg.obj = data
            Handler(Looper.getMainLooper(), Handler.Callback { msg ->
                val event = msg.arg1
                val result = msg.arg2
                val data = msg.obj
                when (event) {
                    SMSSDK.EVENT_GET_VERIFICATION_CODE -> {
                        if (result == SMSSDK.RESULT_COMPLETE) {
                            // 请注意，此时只是完成了发送验证码的请求，验证码短信还需要几秒钟之后才送达
                            ToastUtils.showLong(R.string.send_message_success)
                            timing()
                        } else {
                            when (result) {
                                SMSSDK.RESULT_ERROR -> {
                                    try {
                                        val a = (data as Throwable)
                                        val jsonObject = JSONObject(a.localizedMessage)
                                        val message = jsonObject.opt("detail").toString()
                                        ToastUtils.showLong(message)
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                                else -> {
                                    val a = (data as Throwable)
                                    a.printStackTrace()
                                    ToastUtils.showLong(a.message)
                                }
                            }
                        }
                        hideLoadingDialog()
                    }
                    SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE -> {
                        when (result) {
                            SMSSDK.RESULT_COMPLETE -> {
                                when (typeStr) {
                                    Constant.TYPE_VERIFICATION_CODE -> verificationLogin()
                                    Constant.TYPE_REGISTER -> {
                                        val intent = Intent(this@EnterConfirmationCodeActivity, InputPwdActivity::class.java)
                                        intent.putExtra("phone", phone)
                                        intent.putExtra(Constant.USER_TYPE, Constant.TYPE_REGISTER)
                                        startActivityForResult(intent, 0)
                                        finish()
                                    }
                                    Constant.TYPE_FORGET_PASSWORD -> {
                                        val intent = Intent(this@EnterConfirmationCodeActivity, InputPwdActivity::class.java)
                                        intent.putExtra(Constant.USER_TYPE, Constant.TYPE_FORGET_PASSWORD)
                                        intent.putExtra("phone", account)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            }
                            else -> {
                                if (result == SMSSDK.RESULT_ERROR) {
                                    try {
                                        val a = (data as Throwable)
                                        val jsonObject = JSONObject(a.localizedMessage)
                                        val message = jsonObject.opt("detail").toString()
                                        when{
                                            message.contains("请填写正确的")->ToastUtils.showShort(getString(R.string.right_phone_num))
                                            message.contains("提交校验的验证")->ToastUtils.showShort(getString(R.string.submit_error_code))
                                            message.contains("验证码失效")->ToastUtils.showShort(getString(R.string.verification_code_invalid))
                                            else-> ToastUtils.showLong(message)
                                        }
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                } else {
                                    val a = (data as Throwable)
                                    a.printStackTrace()
                                    //ToastUtils.showLong(a.message)
                                    ToastUtils.showShort(getString(R.string.send_message_fail))
                                }
                                hideLoadingDialog()
                            }
                        }
                    }
                }
                false
            }).sendMessage(msg)
        }
    }

    private fun submitCode(country: String, phone: String, code: String) {
        SMSSDK.submitVerificationCode(country, phone, code)
    }

    @SuppressLint("SetTextI18n")
    private fun timing() {
        mCompositeDisposable.add(Observable.intervalRange(0, TIME_INTERVAL, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    num = 59 - it as Long
                    if (num == 0L) {
                        reacquireCode.text = resources.getString(R.string.reget)
                        reacquireCode.setTextColor(Color.parseColor("#18B4ED"))
                    } else {
                        reacquireCode.text = getString(R.string.regetCount, num)
                        reacquireCode.setTextColor(Color.parseColor("#999999"))
                    }
                })
    }

    private fun verificationCode() {
        if (NetWorkUtils.isNetworkAvalible(this)) {
            send_verification()
        } else {
            ToastUtils.showLong(getString(R.string.network_unavailable))
        }
    }

    private fun send_verification() {
        if (StringUtils.isEmpty(phone)) {
            ToastUtils.showLong(R.string.phone_cannot_be_empty)
        } else {
            showLoadingDialog(getString(R.string.get_code_ing))
            SMSSDK.getVerificationCode(countryCode, phone)
        }
    }


    @SuppressLint("CheckResult")
    private fun verificationLogin() {
        if (!StringUtils.isTrimEmpty(phone)) {
            showLoadingDialog(getString(R.string.logging_tip))
            //("logging: " + "登录错误")
            AccountModel.smsLoginTwo(phone!!)
                    .subscribe({
                        DBUtils.deleteLocalData()
                        //判断是否用户是首次在这个手机登录此账号，是则同步数据
                        showLoadingDialog(getString(R.string.sync_now))
                        SyncDataPutOrGetUtils.syncGetDataStart(it, this.syncCallback)
                        SharedPreferencesUtils.setUserLogin(true)
                    }, {
                        //("logging: " + "登录错误" + e.message)
                        hideLoadingDialog()
                    })
        } else {
            Toast.makeText(this, getString(R.string.phone_or_password_can_not_be_empty), Toast.LENGTH_SHORT).show()
        }
    }

    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            SharedPreferencesHelper.putBoolean(this@EnterConfirmationCodeActivity, Constant.IS_LOGIN, true)
            startActivity(Intent(this@EnterConfirmationCodeActivity, MainActivity::class.java))
            finish()
        }

        override fun error(msg: String) {
            hideLoadingDialog()
            ToastUtils.showLong(msg)
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
            0 -> if (resultCode == Activity.RESULT_FIRST_USER) finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SMSSDK.unregisterEventHandler(eventHandler)
    }
}
