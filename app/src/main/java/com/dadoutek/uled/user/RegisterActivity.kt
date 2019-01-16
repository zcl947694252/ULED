package com.dadoutek.uled.user

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.WindowManager
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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hbb20.CCPCountry.setDialogTitle
import com.hbb20.CountryCodePicker
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_ota_update.view.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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
        StringUtils.initEditTextFilterForRegister(edit_user_phone!!.editText)
        StringUtils.initEditTextFilterForRegister(edit_user_password!!.editText)
        SMSSDK.registerEventHandler(eventHandler)
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.user_register)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.register_completed -> {
                if(NetWorkUtils.isNetworkAvalible(this)){
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
                }else{
                    ToastUtils.showLong(getString(R.string.net_work_error))
                }
            }
            R.id.btn_send_verification ->
                if(NetWorkUtils.isNetworkAvalible(this)){
                    send_verification()
                }else{
                    ToastUtils.showLong(getString(R.string.net_work_error))
                }
        }
    }

    private fun send_verification() {
        val phoneNum = edit_user_phone.getEditText()!!.getText().toString().trim({ it <= ' ' })
        if (com.blankj.utilcode.util.StringUtils.isEmpty(phoneNum)) {
            ToastUtils.showShort(R.string.phone_cannot_be_empty)
        } else {
            SMSSDK.getVerificationCode(countryCode, phoneNum)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SMSSDK.unregisterEventHandler(eventHandler);
    }

    val eventHandler = object : EventHandler() {
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
                if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                    if (result == SMSSDK.RESULT_COMPLETE) {
                        // TODO 处理成功得到验证码的结果
                        // 请注意，此时只是完成了发送验证码的请求，验证码短信还需要几秒钟之后才送达
                        ToastUtils.showLong(R.string.send_message_success)
                        timing()
                    } else {
                        // TODO 处理错误的结果
                        if(result == SMSSDK.RESULT_ERROR){
                            val a=(data as Throwable)
                            val jsonObject=JSONObject(a.localizedMessage)
                            val message=jsonObject.opt("detail").toString()
                            ToastUtils.showLong(message)
                        }else{
                            val a=(data as Throwable)
                            a.printStackTrace()
                            ToastUtils.showLong(a.message)
                        }
                    }
                } else if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                    if (result == SMSSDK.RESULT_COMPLETE) {
                        // TODO 处理验证成功的结果
                        register()
                    } else {
                        // TODO 处理错误的结果
                        if(result == SMSSDK.RESULT_ERROR){
                            val a=(data as Throwable)
                            val jsonObject=JSONObject(a.localizedMessage)
                            val message=jsonObject.opt("detail").toString()
                            ToastUtils.showLong(message)
                            hideLoadingDialog()
                        }else{
                            val a=(data as Throwable)
                            a.printStackTrace()
                            ToastUtils.showLong(a.message)
                        }
                    }
                }
                // TODO 其他接口的返回结果也类似，根据event判断当前数据属于哪个接口
                false
            }).sendMessage(msg)
        }
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
            LogUtils.d("GetDataError:$msg")
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

        if (compileExChar(userName!!)) {
            ToastUtils.showLong(R.string.phone_input_error)
            return false
        } else if (compileExChar(userName!!) || compileExChar(userPassWord!!)) {
            ToastUtils.showLong(R.string.tip_register_input_error)
            return false
        } else {
            return true
        }
    }

    // 提交验证码，其中的code表示验证码，如“1357”
    fun submitCode(country: String, phone: String, code: String){
        SMSSDK.submitVerificationCode(country, phone, code)
    }
}
