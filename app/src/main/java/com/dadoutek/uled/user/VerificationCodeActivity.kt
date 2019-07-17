package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import cn.smssdk.EventHandler
import cn.smssdk.SMSSDK
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.util.LogUtils
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.acitvity_phone_verification.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_verification_code.*
import kotlinx.android.synthetic.main.activity_verification_code.btn_send_verification
import kotlinx.android.synthetic.main.activity_verification_code.edit_user_phone
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_register.ccp as ccp1
import kotlinx.android.synthetic.main.activity_verification_code.ccp as ccp1

class VerificationCodeActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {

    private var countryCode: String? = null
    private val mCompositeDisposable = CompositeDisposable()
    private val TIME_INTERVAL: Long = 60
    private var isChangePwd = false
    private var type: String? = null
    private var isPhone = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_code)
        initView()

    }


    private fun initView() {
        countryCode = ccp.selectedCountryCode
        ccp.setOnCountryChangeListener { countryCode = ccp.selectedCountryCode }
        btn_register.setOnClickListener(this)
        btn_send_verification.setOnClickListener(this)
        sms_login.setOnClickListener(this)
        edit_user_phone.addTextChangedListener(this)
        password_login.setOnClickListener(this)
        SMSSDK.registerEventHandler(eventHandler)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_register -> register()
            R.id.btn_send_verification -> verificationCode()
            R.id.sms_login ->{
                if (TextUtils.isEmpty(edit_user_phone.editableText.toString())) {
                    toast(getString(R.string.please_phone_number))
                    return
                }
            login()
            }
            R.id.password_login -> passwordLogin()
            R.id.date_phone_list -> phoneList()
        }
    }

    private fun phoneList() {
//        if (isPhone) {
////            list_phone_code.visibility = View.VISIBLE
////            edit_verification.visibility = View.GONE
////            btn_login.visibility = View.GONE
////            eye_btn.visibility = View.GONE
//////            btn_register.visibility=View.GONE
////            forget_password.visibility = View.GONE
////            sms_password_login.visibility = View.GONE
////            third_party_text.visibility = View.GONE
////            qq_btn.visibility = View.GONE
////            google_btn.visibility = View.GONE
////            facebook_btn.visibility = View.GONE
////            val layoutmanager = LinearLayoutManager(this)
////            layoutmanager.orientation = LinearLayoutManager.VERTICAL
////            recyclerView!!.layoutManager = layoutmanager
////            this.adapter = PhoneListRecycleViewAdapter(R.layout.recyclerview_phone_list, phoneList!!)
////
////            val decoration = DividerItemDecoration(this,
////                    DividerItemDecoration
////                            .VERTICAL)
////            decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
////                    .divider)))
////            //添加分割线
////            recyclerView?.addItemDecoration(decoration)
////            recyclerView?.itemAnimator = DefaultItemAnimator()
////
//////        adapter!!.addFooterView(getFooterView())
////            adapter!!.bindToRecyclerView(recyclerView)
////            adapter!!.onItemChildClickListener = onItemChildClickListener
////            isPhone = false
////            date_phone.setImageResource(R.drawable.icon_up)
////        } else {
////            list_phone.visibility = View.GONE
////            edit_user_password.visibility = View.VISIBLE
////            btn_login.visibility = View.VISIBLE
////            btn_register.visibility = View.VISIBLE
////            forget_password.visibility = View.VISIBLE
////            eye_btn.visibility = View.VISIBLE
////            sms_password_login.visibility = View.VISIBLE
////            third_party_text.visibility = View.VISIBLE
////            qq_btn.visibility = View.VISIBLE
////            google_btn.visibility = View.VISIBLE
////            facebook_btn.visibility = View.VISIBLE
////            isPhone = true
////            date_phone.setImageResource(R.drawable.icon_down)
////        }
    }

    private fun passwordLogin() {
        val intent = Intent(this@VerificationCodeActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun login() {
//        verificationLogin()
//        verificationCode()
        var intent = Intent(this@VerificationCodeActivity, EnterConfirmationCodeActivity::class.java)
        intent.putExtra(Constant.TYPE_USER, Constant.TYPE_VERIFICATION_CODE)
        intent.putExtra("country_code",countryCode)
        intent.putExtra("phone",edit_user_phone!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), ""))
        startActivity(intent)
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
                        if (result == SMSSDK.RESULT_ERROR) {
                            val a = (data as Throwable)
                            val jsonObject = JSONObject(a.localizedMessage)
                            val message = jsonObject.opt("detail").toString()
                            ToastUtils.showLong(message)
                        } else {
                            val a = (data as Throwable)
                            a.printStackTrace()
                            ToastUtils.showLong(a.message)
                        }
                    }
                    hideLoadingDialog()
                } else if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                    if (result == SMSSDK.RESULT_COMPLETE) {
                        // TODO 处理验证成功的结果
                        if (isChangePwd) {

                        } else {
                            verificationLogin()
                        }
                    } else {
                        // TODO 处理错误的结果
                        if (result == SMSSDK.RESULT_ERROR) {
                            val a = (data as Throwable)
                            val jsonObject = JSONObject(a.localizedMessage)
                            val message = jsonObject.opt("detail").toString()
                            ToastUtils.showLong(message)
                            hideLoadingDialog()
                        } else {
                            val a = (data as Throwable)
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

    private fun verificationLogin() {
        var phone = edit_user_phone!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        if (!StringUtils.isTrimEmpty(phone)) {
            showLoadingDialog(getString(R.string.logging_tip))
            AccountModel.smsLogin(phone!!)
                    .subscribe(object : NetworkObserver<DbUser>() {
                        override fun onNext(dbUser: DbUser) {
                            DBUtils.deleteLocalData()
//                            ToastUtils.showLong(R.string.login_success)
//                            hideLoadingDialog()
                            //判断是否用户是首次在这个手机登录此账号，是则同步数据
//                            showLoadingDialog(getString(R.string.sync_now))
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
            Toast.makeText(this, getString(R.string.phone_or_password_can_not_be_empty), Toast.LENGTH_SHORT).show()
        }
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
        startActivity(Intent(this@VerificationCodeActivity, MainActivity::class.java))
        finish()
    }

    @SuppressLint("SetTextI18n")
    private fun timing() {
        mCompositeDisposable.add(Observable.intervalRange(0, TIME_INTERVAL, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val num = 59 - it as Long
                    if (num == 0L) {
                        btn_send_verification.text = resources.getString(R.string.reget)
                        btn_send_verification.setBackgroundResource(R.drawable.get_code_btn)
                        btn_send_verification.setTextColor(Color.parseColor("#18B4ED"))
                        btn_send_verification.isEnabled = true
                    } else {
                        btn_send_verification.text = num.toString() + " s"
                        btn_send_verification.setBackgroundResource(R.drawable.get_code_btn_false)
                        btn_send_verification.setTextColor(Color.parseColor("#999999"))
                        btn_send_verification.isEnabled = false
                    }
                })
    }

    private fun verificationCode() {
        if (NetWorkUtils.isNetworkAvalible(this)) {
            send_verification()
        } else {
            ToastUtils.showLong(getString(R.string.net_work_error))
        }
    }

    private fun send_verification() {
        val phoneNum = edit_user_phone.getText().toString().trim({ it <= ' ' })
        if (StringUtils.isEmpty(phoneNum)) {
            ToastUtils.showShort(R.string.phone_cannot_be_empty)
        } else {
            showLoadingDialog(getString(R.string.get_code_ing))
            SMSSDK.getVerificationCode(countryCode, phoneNum)
        }
    }

    private fun register() {
        val intent = Intent(this@VerificationCodeActivity, RegisterActivity::class.java)
        intent.putExtra("fromLogin", "register")
        startActivity(intent)
    }

    override fun afterTextChanged(p0: Editable?) {}

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (TextUtils.isEmpty(p0.toString()))
            sms_login.background = getDrawable(R.drawable.btn_rec_black_bt)
        else
            sms_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
    }
}
